package dk.cocode.soccer.core.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.tanh

// The trial's master compressor, used as a limiter so that no sound peaks far above the rest (the tester's ears are
// sensitive), from prototype/js/audio.js: threshold -7.5 dB, ratio 20, attack 3 ms, release 250 ms, hard knee. The
// threshold sits just above the loudest cue (the near tick), so ordinary cues pass untouched and the volume that carries
// distance survives. A feed-forward peak compressor: the level is the peak of the last few milliseconds (a peak hold, so a
// tone is measured by its peaks and not by its zero crossings), the gain follows that level, smoothed by the attack when
// it must fall and by the release when it may rise. Used on the render thread only.
internal const val THRESHOLD_DB = -7.5
internal val THRESHOLD = 10.0.pow(THRESHOLD_DB / 20)

private const val RATIO = 20.0

// How much of the way to its target a one-pole smoother stays short of after one sample, for a time constant in seconds.
private fun decay(seconds: Double) = exp(-1 / (seconds * SAMPLE_RATE))

private val PEAK_DECAY = decay(0.01) // the peak hold lets go with a time constant of 10 ms
private val ATTACK = 1 - decay(0.003)
private val RELEASE = 1 - decay(0.25)

// After the limiter. The trial's 0.57 only took back the +4.3 dB of makeup gain that the browser's compressor adds on
// its own; this limiter adds none, so it is 0.57 * 10 ^ (4.3 / 20): the cues keep the trial's level and a full-scale input
// still settles at -7.75 dBFS, under the -7.5 the trial's test demands.
internal const val MASTER_GAIN = 0.93

// A soft ceiling after the master gain, because the compressor has no lookahead: until its gain has come down (the 3 ms
// attack) the loud first samples of a sudden mix pass at full level. A safety stage for those milliseconds only, not a
// stand-in for the compressor. Below KNEE a sample passes unchanged (ordinary cues peak under 0.4 after the master
// gain); above it the sample bends smoothly towards CEILING and never reaches it.
internal const val KNEE = 0.5
internal const val CEILING = 0.85

internal class Limiter {
    private var gain = 1.0
    private var peak = 0.0

    fun process(x: Double): Double {
        peak = max(abs(x), peak * PEAK_DECAY)
        val target = if (peak > THRESHOLD) (THRESHOLD / peak).pow(1 - 1 / RATIO) else 1.0
        gain += (target - gain) * if (target < gain) ATTACK else RELEASE
        return ceiling(x * gain * MASTER_GAIN)
    }

    private fun ceiling(x: Double): Double {
        val level = abs(x)
        return if (level <= KNEE) x else sign(x) * (KNEE + (CEILING - KNEE) * tanh((level - KNEE) / (CEILING - KNEE)))
    }
}
