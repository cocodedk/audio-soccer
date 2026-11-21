package dk.cocode.soccer.core.text

import dk.cocode.soccer.core.sim.DT
import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.KICK_ANGLE
import dk.cocode.soccer.core.sim.KICK_SPEED
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.TAKE_MAX_SPEED
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.aimedIn
import dk.cocode.soccer.core.sim.arrows
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.sim.createWorld
import dk.cocode.soccer.core.sim.crossingY
import dk.cocode.soccer.core.sim.has
import dk.cocode.soccer.core.sim.rollDistance
import dk.cocode.soccer.core.sim.run
import dk.cocode.soccer.core.sim.step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/describe.test.mjs.
class DescribeTest {
    private val en = gameStrings(Lang.EN)

    private fun kickFrom(x: Double, y: Double): Boolean {
        val w = createWorld(Vec(x - 0.5, y), Vec(x, y), hasBall = true)
        step(w, DT, kick = true)
        return run(w, 6.0).has<Event.Goal>()
    }

    @Test
    fun shotRangeIsDerivedFromTheBallPhysicsAbout24mLessThanAFullKick() {
        close(SHOT_RANGE, rollDistance(KICK_SPEED, TAKE_MAX_SPEED))
        assertTrue(SHOT_RANGE > 23.5 && SHOT_RANGE < 25, "$SHOT_RANGE")
        assertTrue(rollDistance(KICK_SPEED) > SHOT_RANGE && rollDistance(KICK_SPEED) < 27)
        assertTrue(kickFrom(PITCH_L - SHOT_RANGE + 0.5, GOAL.y), "a kick from inside the range reaches the goal")
        assertFalse(kickFrom(PITCH_L - 27.5, GOAL.y), "a kick from beyond its full range stops short")
    }

    @Test
    fun guideTooFarToShootBeyondTheShotRange() {
        assertEquals("Goal, 30 metres, too far to shoot", guideText(Vec(10.0, 10.0), en))
        assertTrue(guideText(Vec(PITCH_L - SHOT_RANGE - 0.1, GOAL.y), en).endsWith("too far to shoot"))
        assertTrue(guideText(Vec(PITCH_L - SHOT_RANGE + 0.1, GOAL.y), en).endsWith("straight ahead"))
    }

    @Test
    fun guideStraightAheadExactlyWhenAStraightKickScoresWithAimHelp() {
        for (y in listOf(7.5, 8.0, 10.0, 12.0, 12.5)) assertTrue(guideText(Vec(31.0, y), en).endsWith("straight ahead"), "y $y")
        for (y in listOf(7.4, 12.6, 3.0, 17.0)) assertFalse(guideText(Vec(31.0, y), en).contains("straight ahead"), "y $y")
    }

    @Test
    fun guideSlightlyLeftOrRightExactlyWhenTheMatching15DegreeKickScoresOtherwiseMetresToMove() {
        // 10 m out a 15 degree kick moves the line 2.68 m, so from y 4.82 upward the right kick reaches the window
        assertTrue(guideText(Vec(30.0, 5.0), en).endsWith("slightly right"))
        assertTrue(guideText(Vec(30.0, 7.4), en).endsWith("slightly right"))
        assertTrue(guideText(Vec(30.0, 4.7), en).endsWith("move 3 metres right"))
        assertTrue(guideText(Vec(30.0, 15.0), en).endsWith("slightly left"))
        assertTrue(guideText(Vec(30.0, 15.3), en).endsWith("move 3 metres left"))
        assertEquals("Goal, 10 metres, move 6 metres right", guideText(Vec(34.0, 2.0), en))
        assertEquals("Goal, 10 metres, move 6 metres left", guideText(Vec(34.0, 18.5), en))
    }

    @Test
    fun guideAgreesWithTheKickWhatItPromisesIsWhatTheKickDoes() {
        for (x in listOf(20.0, 25.0, 30.0, 35.0)) {
            for (y in (2..38).map { it / 2.0 }) { // 1 to 19 in halves
                val text = guideText(Vec(x, y), en)
                val side = if (y > GOAL.y) -1 else 1
                val straight = aimedIn(crossingY(x, y, 0.0))
                val angled = aimedIn(crossingY(x, y, side * KICK_ANGLE))
                if (text.contains("straight ahead")) assertTrue(straight, "$x,$y")
                else if (text.contains("slightly")) assertTrue(!straight && angled, "$x,$y")
                else assertTrue(!straight && !angled, "$x,$y $text")
            }
        }
        for (y in listOf(7.5, 8.2, 12.4)) assertTrue(kickFrom(30.0, y), "a straight kick scores from y $y")
    }

    // The world after a sideways dribble and a stop; the simulation is deterministic, so building it again
    // gives a fresh copy to kick without disturbing the one that was asked.
    private fun dribbled(y0: Double, key: String, secs: Double) = createWorld(Vec(30.0, y0), Vec(30.0, y0), hasBall = true).also {
        run(it, secs, arrows(key))
        run(it, 0.2) // stop; the ball rolls on a little and stays yours
    }

    // Kicking as W told does what it promised. Returns whether the advice differed from the player-based one.
    private fun adviceHolds(y0: Double, key: String, secs: Double): Boolean {
        val w = dribbled(y0, key, secs)
        assertTrue(w.hasBall, "$key ${secs}s still has the ball")
        val said = Regex("Goal, [^.]*").find(whereText(w, en))!!.value
        fun kickWith(vararg keys: String): Boolean {
            val k = dribbled(y0, key, secs)
            step(k, DT, arrows(*keys), true)
            return run(k, 4.0).has<Event.Goal>()
        }
        val msg = "$key ${secs}s, ball y ${w.ball.y}: $said"
        if (said.contains("straight ahead")) assertTrue(kickWith(), msg)
        else if (said.contains("slightly right")) assertTrue(kickWith("Right") && !kickWith(), msg)
        else if (said.contains("slightly left")) assertTrue(kickWith("Left") && !kickWith(), msg)
        else assertTrue(!kickWith() && !kickWith("Right") && !kickWith("Left"), msg)
        return said != guideText(w.player, en)
    }

    // W after a sideways dribble: the ball runs about 1 m ahead of the player, to the side, so the advice
    // has to be judged from the ball (where the kick leaves), not from the player.
    @Test
    fun wAfterASidewaysDribbleKickingAsToldDoesWhatItPromised() {
        val differed = listOf(3.0 to "Right", 17.0 to "Left").sumOf { (y0, key) ->
            (2..10).count { adviceHolds(y0, key, it * 0.25) } // 0.5 to 2.5 s
        }
        assertTrue(differed > 0, "some stops leave the ball where the player-based advice would be wrong")
    }

    @Test
    fun whereSaysPlaceBallGoalAndKeeperInEnglish() {
        val w = createWorld(Vec(20.0, 1.0), Vec(20.0, 4.0), hasBall = false, keeper = true)
        assertEquals(
            "Middle third, by the left board. Ball 3 o'clock, 3 metres. Goal, 22 metres, move 7 metres right. Keeper 1 o'clock, 22 metres.",
            whereText(w, en),
        )
        val mine = createWorld(Vec(34.0, 10.0), Vec(34.5, 10.0), hasBall = true)
        assertEquals("Attacking third, centre. Ball at your feet. Goal, 6 metres, straight ahead.", whereText(mine, en))
        assertEquals("Defending third, by the right board. Ball 9 o'clock, 5 metres. Goal, 41 metres, too far to shoot.", whereText(createWorld(Vec(0.5, 19.0), Vec(0.5, 14.0)), en))
    }
}
