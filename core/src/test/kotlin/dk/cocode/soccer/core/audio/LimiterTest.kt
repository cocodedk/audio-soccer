package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

// From prototype/test/limiter.test.mjs: what the master chain must and must not do to the cues. The trial measured its
// Web Audio compressor; here the limiter is our own, so the same three claims are checked on it, and on the mixer.
class LimiterTest {
    private fun db(x: Double) = 20 * log10(x)

    // The limiter's own gain, from what it returned for the input x.
    private fun gainOf(out: Double, x: Double) = out / (x * MASTER_GAIN)

    // What the static curve asks of the gain for a sustained input: (threshold / level) ^ (1 - 1 / ratio), ratio 20.
    private fun settledGain(level: Double) = (THRESHOLD / level).pow(1 - 1 / 20.0)

    @Test
    fun theLimiterComesFirstAndTheMasterGainAfterIt() {
        val limiter = Limiter()
        assertEquals(0.1 * MASTER_GAIN, limiter.process(0.1), 1e-12)
        assertEquals(-0.3 * MASTER_GAIN, limiter.process(-0.3), 1e-12)
        // A level the limiter must act on, which the master gain would take under the threshold if it came first:
        // held for more than the 3 ms attack, the limiter's gain must fall below 1.
        val x = (THRESHOLD + THRESHOLD / MASTER_GAIN) / 2
        val held = Limiter()
        repeat(frames(0.01)) { held.process(x) }
        assertTrue(gainOf(held.process(x), x) < 0.99, "the limiter did not act on $x")
    }

    // The ceiling hides a compressor that does nothing, so the compressor is checked on its own: a sustained input
    // twice full scale must come out under the ceiling's knee within 10 ms (the 3 ms attack at work).
    @Test
    fun theCompressorAloneBringsASustainedLoudInputUnderTheKneeWithinTenMilliseconds() {
        val limiter = Limiter()
        repeat(frames(0.01)) { limiter.process(2.0) }
        repeat(1000) { assertTrue(limiter.process(2.0) <= KNEE, "still above the knee") }
    }

    // A tone is measured by its peaks, not by the samples near its zero crossings: the static curve says how far a tone
    // of amplitude 4 must be pulled down, and the limiter must get that far, not half way.
    @Test
    fun aSustainedLoudToneIsPulledDownAsFarAsTheStaticCurveAsks() {
        val limiter = Limiter()
        var peak = 0.0
        repeat(frames(0.1)) { n ->
            val out = limiter.process(4.0 * sin(2 * PI * 784.0 * n / SAMPLE_RATE))
            if (n > frames(0.03)) peak = maxOf(peak, abs(out))
        }
        assertEquals(4.0 * settledGain(4.0) * MASTER_GAIN, peak, 0.03)
    }

    // The near tick keeps its full volume difference to the far one, as long as the limiter leaves both alone.
    @Test
    fun ordinaryCuesPassUntouchedSoTheNearAndFarTickKeepTheirVolumeDifference() {
        var loudest = -100.0
        for (seed in 1L..30L) {
            val quiet = playOnce(Sound.TICK, 0.1, gain = 0.1, seed = seed).peak().toDouble()
            val near = playOnce(Sound.TICK, 0.1, gain = 1.2, seed = seed).peak().toDouble()
            val far = playOnce(Sound.TICK, 0.1, gain = 1.2 * 0.3, seed = seed).peak().toDouble()
            assertEquals(12.0, near / quiet, 0.01, "the limiter squeezed the near tick, seed $seed")
            assertEquals(near * 0.3, far, 0.002 * near, "near and far differ by their gain, seed $seed")
            loudest = maxOf(loudest, db(near / MASTER_GAIN / Short.MAX_VALUE))
        }
        assertTrue(THRESHOLD_DB >= loudest + 0.25, "threshold $THRESHOLD_DB is too close to the loudest cue at $loudest dBFS")
    }

    // With no lookahead the first milliseconds of a sudden loud mix get past the compressor; the ceiling catches them.
    @Test
    fun nothingGetsPastTheCeilingAndTheSampleBendsInSmoothlyAboveTheKnee() {
        for (x in listOf(1.0, 3.0, 10.0, 1000.0)) {
            val out = Limiter().process(x)
            assertTrue(out > 0.5 && out <= CEILING, "$x came out as $out")
            assertEquals(-out, Limiter().process(-x), 1e-12)
        }
        val justAbove = Limiter().process(0.5 / MASTER_GAIN + 0.01)
        assertEquals(0.5 + 0.01 * MASTER_GAIN, justAbove, 0.001)
    }

    @Test
    fun aFullScaleInputComesOutNoLouderThanBefore() {
        val limiter = Limiter()
        var out = 0.0
        repeat(SAMPLE_RATE) { out = limiter.process(1.0) }
        assertTrue(db(out) <= -7.5, "full-scale static output ${db(out)} dBFS")
    }

    @Test
    fun theLimiterTakesHoldInAboutItsAttackTime() {
        val limiter = Limiter()
        val x = 0.5 // 1.5 dB over the threshold, and under the ceiling's knee after the master gain
        val settled = settledGain(x)
        var n = 0
        do n++ while (gainOf(limiter.process(x), x) > 1 - 0.632 * (1 - settled) && n < SAMPLE_RATE)
        assertEquals(0.003 * SAMPLE_RATE, n.toDouble(), 5.0)
    }

    @Test
    fun theLimiterLetsGoInAboutItsReleaseTime() {
        val limiter = Limiter()
        repeat(SAMPLE_RATE) { limiter.process(1.0) }
        val squeezed = settledGain(1.0)
        var n = 0
        do n++ while (gainOf(limiter.process(0.1), 0.1) < squeezed + 0.632 * (1 - squeezed) && n < 2 * SAMPLE_RATE)
        // The peak hold lets go of 1.0 first, with a time constant of 10 ms, which adds about 5 ms.
        assertEquals(0.25 * SAMPLE_RATE, n.toDouble(), 350.0)
    }

    // One sample that is not a number must not be kept by the peak hold, which would silence everything after it.
    private val tone = DoubleArray(300) { if (it % 2 == 0) 0.9 else -0.9 }

    private fun outputWith(bad: Double): List<Double> {
        val limiter = Limiter()
        val out = mutableListOf<Double>()
        for (x in tone) out += limiter.process(x)
        out += limiter.process(bad)
        for (x in tone) out += limiter.process(x)
        return out
    }

    @Test
    fun aSampleThatIsNotFiniteIsTakenAsSilence() {
        val silent = outputWith(0.0)
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val out = outputWith(bad)
            assertTrue(out.all { it.isFinite() }, "$bad")
            assertEquals(silent, out, "$bad")
        }
    }
}
