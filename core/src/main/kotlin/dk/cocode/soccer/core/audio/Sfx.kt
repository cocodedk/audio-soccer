package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.audio.FilterType.BANDPASS
import dk.cocode.soccer.core.audio.FilterType.HIGHPASS
import dk.cocode.soccer.core.audio.FilterType.LOWPASS
import dk.cocode.soccer.core.audio.Wave.SINE
import dk.cocode.soccer.core.audio.Wave.TRIANGLE
import kotlin.random.Random

private val CLANG_HZ = doubleArrayOf(523.0, 1287.0, 2093.0, 3011.0)
private val WHISTLE_HZ = doubleArrayOf(2900.0, 3060.0)
private val FANFARE_HZ = doubleArrayOf(392.0, 523.0, 659.0, 784.0) // G4 C5 E5 G5
private const val FANFARE_STEP = 0.11 // s between the notes of its run

// The recipes of prototype/js/sfx.js, speakers path only (no panner, so mono). Each one adds the voices of one sound to
// the mixer's list (each noise voice with a generator of its own from rng); the volume and the lowpass for a sound from behind belong to the whole call. The gains, times and
// frequencies are the trial's. Used on the render thread only.
internal class Sfx(private val rng: Random, private val voices: MutableList<Voice>) {
    private var volume = 1.0
    private var behind = false

    fun play(sound: Sound, volume: Double, behind: Boolean, stretch: Double) {
        this.volume = volume
        this.behind = behind
        when (sound) {
            Sound.RATTLE -> rattle()
            Sound.TICK -> tick()
            Sound.TAP -> tap()
            Sound.KICK -> kick()
            Sound.BOARD -> board()
            Sound.END_WALL -> endWall()
            Sound.BRUSH -> burst(dur = 0.13, freq = 1900.0, q = 0.8, gain = 0.4, attack = 0.04)
            Sound.STEP, Sound.KEEPER_STEP -> burst(dur = 0.05, type = LOWPASS, freq = 420.0, gain = 0.9)
            Sound.CHIME -> chime(stretch.coerceIn(0.35, 1.0))
            Sound.POST -> clang()
            Sound.SAVE -> save()
            Sound.GOAL -> goal()
            Sound.FANFARE -> fanfare()
        }
    }

    // Noise through a biquad, with an attack and then an exponential decay to the end at dur.
    private fun burst(
        dur: Double,
        freq: Double,
        gain: Double,
        type: FilterType = BANDPASS,
        q: Double = 1.0,
        at: Double = 0.0,
        attack: Double = 0.002,
    ) {
        voices += NoiseBurst(Random(rng.nextLong()), Biquad(type, freq, q), Envelope(gain, attack, 0.0, dur - attack), frames(at), volume, behind)
    }

    // An oscillator gliding exponentially from f0 to f1 over dur, with an attack and an exponential decay.
    private fun tone(
        f0: Double,
        f1: Double,
        dur: Double,
        gain: Double,
        wave: Wave = SINE,
        at: Double = 0.0,
        attack: Double = 0.003,
    ) {
        voices += Tone(f0, f1, dur, wave, Envelope(gain, attack, 0.0, dur - attack), frames(at), volume, behind)
    }

    // Metallic grains of the ball's rattle, plus one mid-range grain that still gets through the lowpass of a ball
    // behind you. One call is one cluster; how often it is called is the soundscape's business.
    private fun rattle() {
        repeat(rng.nextInt(2, 4)) {
            burst(dur = 0.016, freq = 4200 + rng.nextDouble(1600.0), q = 9.0, gain = 0.85, at = rng.nextDouble(0.03))
        }
        burst(dur = 0.02, freq = 1200 + rng.nextDouble(300.0), q = 3.5, gain = 0.5, at = rng.nextDouble(0.03))
    }

    // A still ball's soft tick, so a stopped ball is never silent. The 950 Hz body survives the lowpass.
    private fun tick() {
        burst(dur = 0.02, freq = 4800.0, q = 10.0, gain = 0.7)
        tone(2500.0, 2300.0, 0.06, 0.12)
        tone(950.0, 900.0, 0.05, 0.15)
    }

    private fun tap() {
        tone(180.0, 80.0, 0.07, 0.3)
        burst(dur = 0.018, freq = 4600.0, q = 8.0, gain = 0.28)
    }

    private fun kick() {
        tone(150.0, 45.0, 0.13, 0.1)
        burst(dur = 0.03, type = LOWPASS, freq = 1500.0, gain = 0.06)
    }

    // Side boards: a wooden knock.
    private fun board() {
        tone(210.0, 150.0, 0.1, 0.3, TRIANGLE)
        burst(dur = 0.09, freq = 750.0, q = 3.0, gain = 0.38)
    }

    // End walls: lower and heavier than the side boards.
    private fun endWall() {
        tone(105.0, 62.0, 0.18, 0.14, TRIANGLE)
        burst(dur = 0.12, freq = 330.0, q = 2.5, gain = 0.15)
    }

    // Goal beacon: a soft two-note chime. k (0.35 to 1) shortens it for the quick beeps of speakers mode.
    private fun chime(k: Double) {
        tone(784.0, 780.0, 0.6 * k, 0.21, attack = 0.01)
        tone(1175.0, 1170.0, 0.75 * k, 0.18, attack = 0.01, at = 0.17 * k)
    }

    // Post: a ringing metallic clang.
    private fun clang() {
        for (i in CLANG_HZ.indices) {
            tone(CLANG_HZ[i], CLANG_HZ[i] * 0.995, 1 - i * 0.18, 0.034 / (i + 1), attack = 0.001)
        }
        burst(dur = 0.02, freq = 3000.0, q = 2.0, gain = 0.042)
    }

    // Keeper save: a glove slap and a thud.
    private fun save() {
        tone(200.0, 90.0, 0.12, 0.1)
        burst(dur = 0.06, type = LOWPASS, freq = 900.0, gain = 0.1)
        burst(dur = 0.03, freq = 2200.0, q = 1.5, gain = 0.08, at = 0.01)
    }

    // The whole goal sound: the net, the cheer and the whistle, at the volumes the trial's soundscape gave them.
    private fun goal() {
        val whole = volume // play() sets it again for the next sound
        volume = whole * 1.2
        burst(dur = 0.45, type = HIGHPASS, freq = 2600.0, gain = 0.045, attack = 0.05)
        burst(dur = 0.3, freq = 900.0, q = 0.7, gain = 0.04, attack = 0.02)
        volume = whole * 0.7
        burst(dur = 2.8, freq = 1100.0, q = 0.5, gain = 0.11, attack = 0.25)
        burst(dur = 2.2, freq = 2600.0, q = 0.8, gain = 0.05, attack = 0.3)
        volume = whole * 0.3
        whistle()
    }

    // A won round: a quick rising run, then G5 and C6 together, ringing out. Not in the trial, which has no fanfare.
    private fun fanfare() {
        for (i in FANFARE_HZ.indices) {
            tone(FANFARE_HZ[i], FANFARE_HZ[i], 0.16, 0.22, TRIANGLE, at = i * FANFARE_STEP, attack = 0.01)
        }
        val chord = FANFARE_HZ.size * FANFARE_STEP
        tone(FANFARE_HZ.last(), FANFARE_HZ.last(), 0.8, 0.18, TRIANGLE, at = chord, attack = 0.01)
        tone(1047.0, 1047.0, 0.8, 0.14, TRIANGLE, at = chord, attack = 0.01)
    }

    // Two oscillators a little apart, each with a 27 Hz vibrato, held for 0.3 s.
    private fun whistle() {
        for (f in WHISTLE_HZ) {
            voices += Tone(
                f, f, 0.36, SINE, Envelope(0.055, 0.02, 0.28, 0.06), 0, volume, behind,
                vibratoHz = 27.0, vibratoDepth = 110.0,
            )
        }
    }
}
