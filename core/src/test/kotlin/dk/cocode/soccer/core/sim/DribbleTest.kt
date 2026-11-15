package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.math.max

// From prototype/test/dribble.test.mjs.
class DribbleTest {
    @Test
    fun aStillBallIsTakenWithin08mNotBeyondAndNotWhenFast() {
        val near = createWorld(Vec(10.0, 10.0), Vec(10 + DRIBBLE_DIST - 0.01, 10.0))
        assertTrue(step(near, DT).has<Event.Take>())
        assertTrue(near.hasBall)
        val far = createWorld(Vec(10.0, 10.0), Vec(10 + DRIBBLE_DIST + 0.01, 10.0))
        assertFalse(step(far, DT).has<Event.Take>())
        val fast = createWorld(Vec(10.0, 10.0), Vec(10.3, 10.0))
        fast.ballVel = Vec(0.0, 9.0)
        assertFalse(step(fast, DT).has<Event.Take>())
    }

    @Test
    fun takingAStillBallWhileStandingStopsItAndWhileRunningTapsItOn() {
        val still = createWorld(Vec(10.0, 10.0), Vec(10.5, 10.0))
        still.ballVel = Vec(2.0, 0.0)
        val ev = step(still, DT)
        assertEquals(listOf<Event>(Event.Take(10.5, 10.0)), ev)
        assertEquals(Vec.ZERO, still.ballVel)
        val running = createWorld(Vec(10.0, 10.0), Vec(10.5, 10.0))
        assertTrue(step(running, DT, Vec(1.0, 0.0)).has<Event.Tap>())
    }

    @Test
    fun runningWithTheBallTapsItAbout1mAheadAndKeepsIt() {
        val w = createWorld(Vec(5.0, 10.0), Vec(5.5, 10.0), hasBall = true)
        var maxGap = 0.0
        var taps = 0
        var t = 0.0
        while (t < 3) {
            taps += step(w, DT, Vec(1.0, 0.0)).ofType<Event.Tap>().size
            maxGap = max(maxGap, w.ball.x - w.player.x)
            t += DT
        }
        assertTrue(w.hasBall)
        assertTrue(taps in 5..10, "taps $taps")
        assertTrue(maxGap > 0.8 && maxGap < 1.4, "gap $maxGap")
        close(w.player.x, 17.0, 0.1)
    }

    @Test
    fun turningKeepsTheBallAndADribbledBallStopsNearYou() {
        val w = createWorld(Vec(5.0, 10.0), Vec(5.5, 10.0), hasBall = true)
        run(w, 1.0, arrows("Up"))
        run(w, 1.5, arrows("Right"))
        assertTrue(w.hasBall)
        assertTrue(w.ball.y - w.player.y > 0.5, "the ball went right with the player")
        run(w, 3.0)
        assertTrue(hypot(w.ball.x - w.player.x, w.ball.y - w.player.y) < 3, "still within reach after stopping")
        assertTrue(w.hasBall)
    }

    @Test
    fun dribbleStopFor1sThenSpaceWithNoArrowsKicksStraightUpThePitch() {
        for (secs in listOf(0.7, 1.0, 1.3, 1.9, 2.4, 3.1)) {
            val w = createWorld(Vec(5.0, 10.0), Vec(5.5, 10.0), hasBall = true)
            run(w, secs, arrows("Up"))
            run(w, 1.0)
            assertTrue(step(w, DT, Vec.ZERO, true).has<Event.Kick>() && w.ballVel.x > 13, "after $secs s")
        }
    }

    @Test
    fun turningRightAroundTakesTheBallWithYou() {
        val w = createWorld(Vec(20.0, 10.0), Vec(19.5, 10.0), hasBall = true)
        run(w, 1.0, arrows("Down"))
        run(w, 1.0, arrows("Up"))
        assertTrue(w.hasBall)
        assertTrue(w.ball.x > w.player.x, "the ball is ahead again")
    }

    @Test
    fun aBallPastLoseDistIsNoLongerYours() {
        val w = createWorld(Vec(10.0, 10.0), Vec(10.5, 10.0), hasBall = true)
        w.ball = Vec(14.0, 10.0)
        step(w, DT)
        assertFalse(w.hasBall)
    }
}
