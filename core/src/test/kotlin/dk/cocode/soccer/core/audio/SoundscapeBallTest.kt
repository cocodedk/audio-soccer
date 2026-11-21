package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.cues.distanceGain
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.ballWorld
import dk.cocode.soccer.core.sim.close
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The ball's sounds in prototype/js/soundscape.js, speakers path only: the tick of a still ball and the rattle of a rolling one.
class SoundscapeBallTest {
    private val sink = RecordingSink()
    private val scape = Soundscape(sink).also { it.reset(2) }

    @Test
    fun aStillBall3mAwayTicksEveryFifthOfASecondAnd20mAwayEveryEightTenths() {
        sink.run(scape, ballWorld(Vec(13.0, 10.0)), 2.0)
        assertEvery(0.2, sink.times(Sound.TICK))
        assertTrue(sink.of(Sound.TICK).size in 8..10)
    }

    @Test
    fun aStillBall20mAwayTicksEveryEightTenthsTheFirstOneComingThreeTenthsInto() {
        sink.run(scape, ballWorld(Vec(30.0, 10.0)), 5.0)
        assertEvery(0.8, sink.times(Sound.TICK))
        close(0.3, sink.times(Sound.TICK).first(), 0.011)
    }

    @Test
    fun theTickSpeedsUpAtOnceWhenYouCloseIn() {
        val w = ballWorld(Vec(30.0, 10.0))
        sink.run(scape, w, 0.35)
        assertEquals(1, sink.of(Sound.TICK).size)
        w.ball = Vec(13.0, 10.0)
        sink.run(scape, w, 0.25)
        assertEquals(2, sink.of(Sound.TICK).size, "the next tick is within 0.2 s, not 0.8 s")
    }

    @Test
    fun aTickIsLoudAsTheTrialsGainTimesTheDistanceGainFixedAtTriggerTime() {
        sink.run(scape, ballWorld(Vec(13.0, 10.0)), 0.3)
        val tick = sink.of(Sound.TICK).first()
        close(1.2 * distanceGain(3.0), tick.gain)
        assertFalse(tick.behind)
        assertEquals(1.0, tick.stretch)
    }

    @Test
    fun aBallMoreThanAMetreBehindPlaysBehindWithTheMakeupGain() {
        sink.run(scape, ballWorld(Vec(8.0, 10.0)), 0.3)
        val behind = sink.of(Sound.TICK).first()
        assertTrue(behind.behind)
        close(1.2 * 1.3 * distanceGain(2.0), behind.gain)
    }

    @Test
    fun aBallLessThanAMetreBehindIsInReachNotBehind() {
        sink.run(scape, ballWorld(Vec(9.2, 10.0)), 0.3)
        assertFalse(sink.of(Sound.TICK).first().behind)
    }

    @Test
    fun aRollingBallRattlesFasterTheFasterItGoesAndDoesNotTick() {
        val w = ballWorld(Vec(14.0, 10.0))
        w.ballVel = Vec(5.0, 0.0)
        sink.run(scape, w, 1.0, dt = 0.001)
        assertEquals(emptyList<Double>(), sink.times(Sound.TICK))
        assertEvery(0.115, sink.times(Sound.RATTLE))
        close(0.45 + 5.0 / 14 * 0.55, sink.of(Sound.RATTLE).first().gain / distanceGain(4.0))
        w.ballVel = Vec(14.0, 0.0)
        val before = sink.of(Sound.RATTLE).size
        sink.run(scape, w, 1.0, dt = 0.001)
        assertEvery(0.035, sink.times(Sound.RATTLE).drop(before))
        close(1.0, sink.of(Sound.RATTLE).last().gain / distanceGain(4.0))
        w.ballVel = Vec(0.2, 0.0) // slower than 0.3 m/s counts as still
        val rattles = sink.of(Sound.RATTLE).size
        sink.run(scape, w, 0.5)
        assertEquals(rattles, sink.of(Sound.RATTLE).size)
        assertTrue(sink.of(Sound.TICK).isNotEmpty())
    }

    @Test
    fun aRattleFromBehindGetsTheDoubleMakeup() {
        val w = ballWorld(Vec(8.0, 10.0))
        w.ballVel = Vec(-14.0, 0.0)
        sink.run(scape, w, 0.01)
        val rattle = sink.of(Sound.RATTLE).single()
        assertTrue(rattle.behind)
        close(2 * distanceGain(2.0), rattle.gain)
    }
}
