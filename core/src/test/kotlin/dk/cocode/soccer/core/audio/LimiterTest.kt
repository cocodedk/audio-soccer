package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.log10
import kotlin.math.pow

// From prototype/test/limiter.test.mjs: what the master chain must and must not do to the cues. The trial measured its
// Web Audio compressor; here the limiter is our own, so the same three claims are checked on it, and on the mixer.
class LimiterTest {
    private fun db(x: Double) = 20 * log10(x)

    // The limiter's own gain, from what it returned for the input x.
    private fun gainOf(out: Double, x: Double) = out / (x * MASTER_GAIN)

    @Test
    fun theLimiterComesFirstAndTheMasterGainAfterIt() {
        val limiter = Limiter()
        assertEquals(0.1 * MASTER_GAIN, limiter.process(0.1), 1e-12)
        assertEquals(-0.3 * MASTER_GAIN, limiter.process(-0.3), 1e-12)
        assertTrue(Limiter().process(2.0) < 2.0 * MASTER_GAIN, "above the threshold it pulls the gain down")
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
        val settled = (THRESHOLD / x).pow(0.95) // (threshold / level) ^ (1 - 1 / ratio), the ratio being 20
        var n = 0
        do n++ while (gainOf(limiter.process(x), x) > 1 - 0.632 * (1 - settled))
        assertEquals(0.003 * SAMPLE_RATE, n.toDouble(), 5.0)
    }

    @Test
    fun theLimiterLetsGoInAboutItsReleaseTime() {
        val limiter = Limiter()
        repeat(SAMPLE_RATE) { limiter.process(1.0) }
        val squeezed = THRESHOLD.pow(0.95)
        var n = 0
        do n++ while (gainOf(limiter.process(0.1), 0.1) < squeezed + 0.632 * (1 - squeezed))
        assertEquals(0.25 * SAMPLE_RATE, n.toDouble(), 60.0)
    }
}
