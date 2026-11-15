package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.tan

// The helpers of prototype/js/ball.js that describe.js, speakers.js and buzz.js build on.
class BallTest {
    @Test
    fun aimedInCoversTheMouthPlusMinusOneMetre() {
        assertEquals(7.5, AIM_LOW)
        assertEquals(12.5, AIM_HIGH)
        assertTrue(aimedIn(7.5) && aimedIn(10.0) && aimedIn(12.5))
        assertFalse(aimedIn(7.49) || aimedIn(12.51))
    }

    @Test
    fun crossingYIsWhereTheLineMeetsTheGoalLine() {
        close(crossingY(30.0, 10.0, 0.0), 10.0)
        close(crossingY(30.0, 10.0, KICK_ANGLE), 10 + 10 * tan(KICK_ANGLE))
        close(crossingY(30.0, 10.0, -KICK_ANGLE), 10 - 10 * tan(KICK_ANGLE))
    }

    @Test
    fun goalSideIsAheadWhenAStraightKickGoesInElseWhichWayTheGoalLies() {
        assertEquals(GoalSide.AHEAD, goalSide(10.0))
        assertEquals(GoalSide.AHEAD, goalSide(12.5))
        assertEquals(GoalSide.LEFT, goalSide(15.0))
        assertEquals(GoalSide.RIGHT, goalSide(3.0))
    }

    @Test
    fun rollingDecelerationAddsDragAtLowSpeedAndFadesOutBySlowDragUntil() {
        close(decelAt(SLOW_DRAG_UNTIL), FRICTION)
        close(decelAt(20.0), FRICTION)
        close(decelAt(0.0), FRICTION + SLOW_DRAG)
        assertTrue(decelAt(3.0) < decelAt(0.0) && decelAt(3.0) > decelAt(6.0))
    }

    @Test
    fun rollDistanceIsTheSameLawMoveBallAppliesAndShrinksWithTheFloorSpeed() {
        val full = rollDistance(KICK_SPEED)
        assertTrue(full > rollDistance(KICK_SPEED, TAKE_MAX_SPEED) && full < 27, "rolls $full")
        val w = createWorld(Vec(35.0, 2.0), Vec(5.0, 10.0))
        w.ballVel = Vec(KICK_SPEED, 0.0)
        run(w, 6.0)
        close(w.ball.x - 5, full, 0.3)
    }
}
