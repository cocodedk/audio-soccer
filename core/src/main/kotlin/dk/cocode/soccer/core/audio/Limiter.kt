package dk.cocode.soccer.core.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.tanh

// The trial's master compressor, used as a limiter so that no sound peaks far above the rest (the tester's ears are
// sensitive), from prototype/js/audio.js: threshold -7.5 dB, ratio 20, attack 3 ms, release 250 ms, hard knee. The
// threshold sits just above the loudest cue (the near tick), so ordinary cues pass untouched and the volume that carries
// distance survives. A feed-forward peak compressor: the gain follows the level of the sample itself, smoothed by the
// attack when it must fall and by the release when it may rise. With no lookahead it lets the first few milliseconds of a
// sudden loud mix through, so a soft ceiling after the master gain keeps every sample under CEILING. Used on the render
// thread only.
internal const val THRESHOLD_DB = -7.5
internal val THRESHOLD = 10.0.pow(THRESHOLD_DB / 20)

private const val RATIO = 20.0
private val ATTACK = 1 - exp(-1 / (0.003 * SAMPLE_RATE))
private val RELEASE = 1 - exp(-1 / (0.25 * SAMPLE_RATE))

// After the limiter. The trial's 0.57 only took back the +4.3 dB of makeup gain that the browser's compressor adds on
// its own; this limiter adds none, so it is 0.57 * 10 ^ (4.3 / 20): the cues keep the trial's level and a full-scale input
// still settles at -7.75 dBFS, under the -7.5 the trial's test demands.
internal const val MASTER_GAIN = 0.93

// Below KNEE a sample passes unchanged (ordinary cues peak under 0.4 after the master gain); above it the sample bends
// smoothly towards CEILING and never reaches it.
private const val KNEE = 0.5
internal const val CEILING = 0.85

internal class Limiter {
    private var gain = 1.0

    fun process(x: Double): Double {
        val level = abs(x)
        val target = if (level > THRESHOLD) (THRESHOLD / level).pow(1 - 1 / RATIO) else 1.0
        gain += (target - gain) * if (target < gain) ATTACK else RELEASE
        return ceiling(x * gain * MASTER_GAIN)
    }

    private fun ceiling(x: Double): Double {
        val level = abs(x)
        return if (level <= KNEE) x else sign(x) * (KNEE + (CEILING - KNEE) * tanh((level - KNEE) / (CEILING - KNEE)))
    }
}
