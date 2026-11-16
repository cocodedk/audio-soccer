package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.input.MenuInput.Focus
import dk.cocode.soccer.core.input.MenuInput.Next
import dk.cocode.soccer.core.input.MenuInput.Prev
import dk.cocode.soccer.core.input.MenuInput.Repeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// Two-finger swipes in the touch-to-hear menu, which a player asked for: a scroll with two fingers moves through the
// options at any speed. The rig is 3 options over 900 dp.
class MenuGesturesTwoFingerTest {
    private val r = MenuRig()

    // Fingers a and b land 40 ms apart, side by side, and travel together from (x0, y0) to (x1, y1) over ms, with the
    // frames ticking all the while; a lifts first, then b.
    private fun twoFingers(a: Int, b: Int, t0: Long, x0: Int, y0: Int, x1: Int, y1: Int, ms: Long = 1000) {
        r.down(a, x0, y0, t0)
        r.down(b, x0 + 80, y0, t0 + 40)
        for (step in 1..4) {
            val t = t0 + ms * step / 5
            r.move(a, x0 + (x1 - x0) * step / 5, y0 + (y1 - y0) * step / 5, t)
            r.move(b, x0 + 80 + (x1 - x0) * step / 5, y0 + (y1 - y0) * step / 5, t)
            r.tick(t)
        }
        r.up(a, x1, y1, t0 + ms)
        r.up(b, x1 + 80, y1, t0 + ms + 20)
    }

    @Test
    fun aSlowTwoFingerSwipeIsNextOrPrevInEveryDirectionAndNeverExplores() {
        twoFingers(1, 2, 0, 100, 400, 220, 420) // right
        twoFingers(3, 4, 2000, 300, 400, 180, 390) // left
        twoFingers(5, 6, 4000, 200, 600, 210, 380) // up
        twoFingers(7, 8, 6000, 200, 300, 190, 560) // down
        r.tick(9000)
        assertEquals(listOf(Next, Prev, Prev, Next), r.got, "no Focus: the fingers were down far longer than a tap")
    }

    @Test
    fun aQuickTwoFingerSwipeMovesToo() {
        twoFingers(1, 2, 0, 200, 300, 200, 300 + SWIPE_DP.toInt(), ms = 120)
        assertEquals(listOf(Next), r.got)
    }

    @Test
    fun aSlowTwoFingerStrokeShorterThanASwipeDoesNothingAndLeavesNoTapBehind() {
        twoFingers(1, 2, 0, 200, 450, 210, 470)
        r.tap(3, 1000 + 100, 450) // were the stroke a tap, this would be its second
        r.tick(5000)
        assertEquals(listOf(Repeat), r.got, "only the later tap, on its own")
    }

    @Test
    fun aTwoFingerSwipeAfterATapIsNotADoubleTap() {
        r.tap(1, 0, 450) // lifted at 80, waiting for a second tap
        twoFingers(2, 3, 200, 200, 300, 200, 600)
        r.tick(5000)
        assertEquals(listOf(Next), r.got)
    }

    // The frames may lag the touches: a finger landing after TAP_MS is too late to make the stroke a two-finger one.
    @Test
    fun aSecondFingerLandingAfterTheTapTimeLeavesTheFirstFingerExploring() {
        r.down(1, 200, 450, 0)
        r.down(2, 280, 450, TAP_MS + 1) // before any frame has ticked
        assertEquals(listOf(Focus(1)), r.got, "it explores at once")
        r.tick(TAP_MS + 10)
        r.move(1, 200, 700, TAP_MS + 100)
        assertEquals(listOf(Focus(1), Focus(2)), r.got, "and goes on exploring")
    }

    @Test
    fun theSecondFingerLiftingFirstDoesNotEndTheStroke() {
        r.down(1, 200, 300, 0)
        r.down(2, 280, 300, 30)
        r.up(2, 280, 300, 100)
        r.move(1, 200, 450, 500)
        r.tick(500)
        r.up(1, 200, 600, 1000)
        assertEquals(listOf(Next), r.got)
    }

    @Test
    fun aFingerLeftDownAfterTheStrokeHasEndedDoesNothingAndTheNextTouchWorks() {
        r.down(1, 200, 300, 0)
        r.down(2, 280, 300, 30)
        r.up(1, 200, 500, 400) // Next
        r.move(2, 280, 100, 600)
        r.up(2, 280, 100, 700)
        assertEquals(listOf(Next), r.got, "the finger left behind neither steers nor swipes")
        r.tap(3, 2000, 750)
        r.tick(5000)
        assertEquals(listOf(Next, Repeat), r.got)
    }
}
