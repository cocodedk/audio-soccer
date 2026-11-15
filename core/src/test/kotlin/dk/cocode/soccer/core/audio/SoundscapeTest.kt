package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.cues.beaconEvery
import dk.cocode.soccer.core.cues.distanceGain
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.ballWorld
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.sim.createWorld
import dk.cocode.soccer.core.sim.dist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The rest of the continuous sounds of prototype/js/soundscape.js, speakers path only: the goal chime, footsteps, the keeper
// and the crowd bed.
class SoundscapeTest {
    private val sink = RecordingSink()
    private val scape = Soundscape(sink).also { it.reset(2) }

    @Test
    fun theGoalChimePlaysOnlyWhileThePlayerHasTheBall() {
        val w = ballWorld(Vec(10.5, 10.0))
        sink.run(scape, w, 3.0)
        assertEquals(emptyList<Double>(), sink.times(Sound.CHIME), "the ball is the target, not the goal")
        w.hasBall = true
        sink.run(scape, w, 0.35)
        assertEquals(1, sink.of(Sound.CHIME).size, "a chime within 0.3 s of taking the ball")
        val every = beaconEvery(dist(w.player, GOAL))
        sink.run(scape, w, 8.0)
        assertEvery(every, sink.times(Sound.CHIME).drop(1))
        val chime = sink.of(Sound.CHIME).last()
        close(0.4 * distanceGain(dist(w.player, GOAL)), chime.gain)
        close(every / 2, chime.stretch)
        assertFalse(chime.behind)
    }

    @Test
    fun theChimeComesFasterAsYouCloseInOnTheGoal() {
        val w = ballWorld(Vec(39.5, 10.0), hasBall = true, player = Vec(39.0, 10.0))
        sink.run(scape, w, 4.0)
        assertEvery(0.5, sink.times(Sound.CHIME).drop(1))
        close(0.25, sink.of(Sound.CHIME).last().stretch)
        close(0.4, sink.of(Sound.CHIME).last().gain)
    }

    @Test
    fun footstepsComeEveryThirdOfASecondWhileMovingAndBrushTheBoardWhenNearIt() {
        val w = ballWorld(Vec(30.0, 10.0))
        sink.run(scape, w, 1.0)
        assertEquals(emptyList<Double>(), sink.times(Sound.STEP), "standing still")
        w.playerMoving = true
        sink.run(scape, w, 2.0)
        assertEvery(0.35, sink.times(Sound.STEP))
        assertEquals(0.22, sink.of(Sound.STEP).first().gain, "a footstep carries no distance")
        assertEquals(emptyList<Double>(), sink.times(Sound.BRUSH), "mid-pitch")
        w.player = Vec(10.0, 1.0)
        sink.run(scape, w, 0.4)
        close(0.7, sink.of(Sound.BRUSH).last().gain)
        w.player = Vec(10.0, 19.0)
        val brushes = sink.of(Sound.BRUSH).size
        sink.run(scape, w, 0.4)
        assertTrue(sink.of(Sound.BRUSH).size > brushes, "the right board too")
    }

    @Test
    fun theKeeperStepsEveryFortyTwoHundredthsWhileHeMoves() {
        scape.reset(3)
        val w = createWorld(Vec(10.0, 10.0), Vec(30.0, 10.0), keeper = true)
        sink.run(scape, w, 1.0)
        assertEquals(emptyList<Double>(), sink.times(Sound.KEEPER_STEP), "standing still")
        val keeper = checkNotNull(w.keeper)
        keeper.moving = true
        sink.run(scape, w, 2.0)
        assertEvery(0.42, sink.times(Sound.KEEPER_STEP))
        close(0.7 * distanceGain(dist(w.player, keeper.pos)), sink.of(Sound.KEEPER_STEP).first().gain)
    }

    @Test
    fun noKeeperNoKeeperSteps() {
        sink.run(scape, ballWorld(Vec(30.0, 10.0)), 1.0)
        assertEquals(emptyList<Double>(), sink.times(Sound.KEEPER_STEP))
    }

    @Test
    fun theCrowdIsOffInLevel1() {
        scape.reset(1)
        val w = ballWorld(Vec(30.0, 10.0))
        sink.run(scape, w, 0.1)
        w.ball = Vec(20.0, 10.0)
        sink.run(scape, w, 0.1)
        assertTrue(sink.crowds.isNotEmpty() && sink.crowds.all { it == 0.0 }, "the bed stays off, whatever the third")
    }

    @Test
    fun theCrowdSwellsInTheFinalThirdOfLevels2And3AndDropsBack() {
        for (level in 2..3) {
            scape.reset(level)
            sink.crowds.clear()
            val w = ballWorld(Vec(20.0, 10.0))
            sink.run(scape, w, 0.1)
            assertEquals(listOf(0.045), sink.crowds)
            sink.run(scape, w, 0.1)
            assertEquals(listOf(0.045), sink.crowds, "only a change of third speaks to the sink")
            w.ball = Vec(27.0, 10.0)
            sink.run(scape, w, 0.1)
            assertEquals(listOf(0.045, 0.07), sink.crowds)
            w.ball = Vec(26.0, 10.0)
            sink.run(scape, w, 0.1)
            assertEquals(listOf(0.045, 0.07, 0.045), sink.crowds)
        }
    }

    @Test
    fun resetStartsTheCrowdOverForTheNextLevel() {
        sink.run(scape, ballWorld(Vec(20.0, 10.0)), 0.1)
        scape.reset(1)
        sink.run(scape, ballWorld(Vec(20.0, 10.0)), 0.1)
        assertEquals(listOf(0.045, 0.0), sink.crowds)
    }
}
