package dk.cocode.soccer.core.audio

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Web Audio's exponential ramps can neither start at nor reach zero, so the trial's envelopes run between this and the gain.
private const val FLOOR = 0.0001

// How long silence() takes to fade the sounds out, as the trial's stopAll() does.
private const val FADE_SECONDS = 0.06

private const val DULL_HZ = 2000.0 // behind: keeps the tick's 2.5 kHz tone and the rattle's mid grain

// How long the crowd bed takes to reach a new level.
private const val CROWD_RAMP_SECONDS = 0.3

internal fun frames(seconds: Double) = (seconds * SAMPLE_RATE).roundToInt()

// The trial's gain envelope: an exponential ramp from FLOOR up to the gain, held for hold seconds if asked (the whistle),
// then an exponential ramp back down to FLOOR. One sample at a time; finished once the last sample is out.
internal class Envelope(gain: Double, attack: Double, hold: Double, decay: Double) {
    private val attackN = frames(attack).coerceAtLeast(1)
    private val holdN = frames(hold)
    private val decayN = frames(decay).coerceAtLeast(1)
    private val up = (gain / FLOOR).pow(1.0 / attackN)
    private val down = (FLOOR / gain).pow(1.0 / decayN)
    private var value = FLOOR
    private var n = 0

    val finished get() = n >= attackN + holdN + decayN

    fun next(): Double {
        val v = value
        n++
        if (n <= attackN) value *= up else if (n > attackN + holdN) value *= down
        return v
    }
}

// One sound source in the mix. delay is the wait before it starts (the trial's `at`), gain its place in the sound's volume,
// behind puts the 2000 Hz lowpass of a sound heard from behind on it (the trial puts one filter on the whole sound; as the
// filter is linear, one on each voice gives the same sum). Only the render thread touches a voice.
internal abstract class Voice(private var delay: Int, private val gain: Double, behind: Boolean) {
    private val dull = if (behind) Biquad(FilterType.LOWPASS, DULL_HZ, 1.0) else null
    private var fade = 1.0
    private var fadeStep = 0.0

    var done = false
        private set

    // The next raw sample; calls end() with its last one.
    protected abstract fun next(): Double

    protected fun end() {
        done = true
    }

    // The envelope's next gain; the voice ends with the envelope.
    protected fun Envelope.step(): Double {
        val gain = next()
        if (finished) end()
        return gain
    }

    // Starts the fade that silence() asks for; the voice is done when it has run out.
    fun fadeOut() {
        fadeStep = 1.0 / frames(FADE_SECONDS)
    }

    // Adds the first n samples of the voice into buf.
    fun mix(buf: FloatArray, n: Int) {
        var i = 0
        while (i < n && !done) {
            fade -= fadeStep
            if (fade <= 0.0) {
                end()
            } else if (delay > 0) {
                delay--
            } else {
                var s = next() * gain
                if (dull != null) s = dull.process(s)
                buf[i] += (s * fade).toFloat()
            }
            i++
        }
    }
}

// Noise through a filter: the trial's burst().
internal class NoiseBurst(
    private val rng: Random,
    private val filter: Biquad,
    private val envelope: Envelope,
    delay: Int,
    gain: Double,
    behind: Boolean,
) : Voice(delay, gain, behind) {
    override fun next(): Double = filter.process(rng.noise()) * envelope.step()
}

private fun Random.noise() = nextDouble() * 2 - 1

internal enum class Wave { SINE, TRIANGLE }

// An oscillator gliding exponentially from f0 to f1 over glide seconds: the trial's tone(). The whistle adds a
// vibrato, a sine of vibratoHz moving the frequency by up to vibratoDepth Hz.
internal class Tone(
    f0: Double,
    f1: Double,
    glide: Double,
    private val wave: Wave,
    private val envelope: Envelope,
    delay: Int,
    gain: Double,
    behind: Boolean,
    private val vibratoHz: Double = 0.0,
    private val vibratoDepth: Double = 0.0,
) : Voice(delay, gain, behind) {
    private val glideRatio = (f1 / f0).pow(1.0 / frames(glide).coerceAtLeast(1))
    private var freq = f0
    private var phase = 0.0 // in cycles, 0 until 1
    private var vibratoPhase = 0.0

    override fun next(): Double {
        val e = envelope.step()
        val s = when (wave) {
            Wave.SINE -> sin(2 * PI * phase)
            Wave.TRIANGLE -> if (phase < 0.25) 4 * phase else if (phase < 0.75) 2 - 4 * phase else 4 * phase - 4
        }
        var f = freq
        if (vibratoDepth != 0.0) {
            f += vibratoDepth * sin(2 * PI * vibratoPhase)
            vibratoPhase += vibratoHz / SAMPLE_RATE
            if (vibratoPhase >= 1.0) vibratoPhase -= 1.0
        }
        phase += f / SAMPLE_RATE
        if (phase >= 1.0) phase -= 1.0
        freq *= glideRatio
        return s * e
    }
}

// The crowd bed: noise through the trial's band and low filters, at a level that moves in a straight ramp to each
// new target. It plays until a ramp to 0 has run out.
internal class Crowd(private val rng: Random) : Voice(0, 1.0, false) {
    private val band = Biquad(FilterType.BANDPASS, 650.0, 0.5)
    private val low = Biquad(FilterType.LOWPASS, 550.0, 1.0)
    private var level = 0.0
    private var target = 0.0
    private var step = 0.0
    private var left = 0

    fun ramp(to: Double) {
        target = to
        left = frames(CROWD_RAMP_SECONDS)
        step = (to - level) / left
    }

    override fun next(): Double {
        if (left > 0) {
            level += step
            if (--left == 0) {
                level = target
                if (target == 0.0) end()
            }
        }
        return low.process(band.process(rng.noise())) * level
    }
}
