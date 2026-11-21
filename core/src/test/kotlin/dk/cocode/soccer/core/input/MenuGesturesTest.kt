package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.input.MenuInput.Activate
import dk.cocode.soccer.core.input.MenuInput.Focus
import dk.cocode.soccer.core.input.MenuInput.Next
import dk.cocode.soccer.core.input.MenuInput.Prev
import dk.cocode.soccer.core.input.MenuInput.Repeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The menu rules of the plan, one test each; MenuGesturesEdgeTest holds the rest. The rig is 3 options over 900 dp.
class MenuGesturesTest {
    private val r = MenuRig()
    private val nothing = emptyList<MenuInput>()

    // A finger held on height y from t until it explores, then lifted: it hears the band there.
    private fun MenuRig.hear(id: Int, t: Long, y: Int) {
        explore(id, t, y)
        up(id, 200, y, t + TAP_MS + 50)
    }

    @Test
    fun optionsAreEqualBandsDownTheScreen() {
        val bands = listOf(0 to 0, 299 to 0, 300 to 1, 599 to 1, 600 to 2, 899 to 2, 900 to 2, 1500 to 2, -20 to 0)
        for ((y, band) in bands) {
            val rig = MenuRig()
            rig.explore(1, 0, y)
            assertEquals(listOf(Focus(band)), rig.got, "y = $y")
        }
    }

    @Test
    fun theBandsFollowTheCountAndTheHeightAsTheyAreNow() {
        val rig = MenuRig(count = 4, height = 800f)
        rig.hear(1, 0, 399)
        rig.hear(2, 1000, 400)
        rig.height = 400f // the screen turned
        rig.hear(3, 2000, 399)
        rig.count = 2
        rig.hear(4, 3000, 399)
        rig.tick(9000)
        assertEquals(listOf(Focus(1), Focus(2), Focus(3), Focus(1)), rig.got)
    }

    @Test
    fun aFingerStillDownAfterTheTapTimeExploresAtOnce() {
        r.down(1, 100, 450, 0)
        r.tick(TAP_MS)
        assertEquals(nothing, r.got, "300 ms is still a tap")
        r.tick(TAP_MS + 1)
        assertEquals(listOf(Focus(1)), r.got)
        r.tick(TAP_MS + 200)
        assertEquals(listOf(Focus(1)), r.got, "said once")
    }

    @Test
    fun anExploringFingerFocusesAgainEachTimeItCrossesIntoAnotherBand() {
        r.explore(1, 0, 100)
        r.move(1, 100, 250, 400) // the same band
        r.move(1, 100, 350, 450)
        r.move(1, 100, 360, 500)
        r.move(1, 100, 700, 550)
        r.move(1, 100, 320, 600)
        r.move(1, 100, 1000, 650) // off the bottom is the last band
        assertEquals(listOf(Focus(0), Focus(1), Focus(2), Focus(1), Focus(2)), r.got)
    }

    @Test
    fun aStrokeOfSwipeLengthLiftedWithinTheTapTimeIsNextOrPrev() {
        r.down(1, 100, 400, 0)
        r.up(1, 220, 430, 150) // right
        r.down(1, 300, 400, 1000)
        r.up(1, 180, 380, 1150) // left
        r.down(1, 200, 500, 2000)
        r.up(1, 210, 380, 2150) // up
        r.down(1, 200, 380, 3000)
        r.up(1, 190, 500, 3150) // down
        r.down(1, 100, 400, 4000)
        r.up(1, 100 + SWIPE_DP.toInt(), 400, 4000 + TAP_MS) // exactly the length, exactly the time
        r.tick(9000)
        assertEquals(listOf(Next, Prev, Prev, Next, Next), r.got, "and a swipe leaves no tap behind")
    }

    @Test
    fun aTapFollowedByASecondTapWithinTheWindowIsActivateWhereverItLands() {
        r.tap(1, 0, 100) // lifted at 80
        r.tap(2, 80 + DOUBLE_MS, 800) // lands exactly 350 ms after the lift
        assertEquals(listOf(Activate), r.got)
        r.tick(5000)
        assertEquals(listOf(Activate), r.got, "the double-tap used both taps")
    }

    @Test
    fun aSingleTapRepeatsTheCurrentOptionFromTickOnceTheWindowHasPassed() {
        r.tap(1, 0, 450) // lifted at 80
        r.tick(80 + DOUBLE_MS)
        assertEquals(nothing, r.got, "a second tap may still come")
        r.tick(80 + DOUBLE_MS + 1)
        assertEquals(listOf(Repeat), r.got, "wherever it landed")
        r.tick(9000)
        assertEquals(listOf(Repeat), r.got, "said once")
    }

    @Test
    fun doubleTapDoesNotMoveFocus() {
        val rig = MenuRig(count = 4)
        rig.explore(1, 0, 800) // the last option
        rig.up(1, 200, 800, 500)
        rig.tap(2, 1000, 100) // two taps on the first
        rig.tap(3, 1180, 100)
        rig.tick(5000)
        assertEquals(listOf(Focus(3), Activate), rig.got, "no Focus(0): Activate chooses what is focused")
    }

    @Test
    fun cancelDropsTheStroke() {
        r.down(1, 100, 400, 0)
        r.move(1, 220, 400, 100)
        r.cancel(1, 120)
        r.up(1, 220, 400, 150) // a swipe had it not been cancelled
        r.explore(2, 1000, 100)
        r.cancel(2, 1400)
        r.up(2, 200, 100, 1500)
        r.tap(3, 2000, 450) // a tap waiting for its second
        r.cancel(3, 2100)
        r.tick(9000)
        assertEquals(listOf(Focus(0)), r.got, "only what the exploration said before the cancel")
        r.tap(4, 10000, 700)
        r.tick(11000)
        assertEquals(listOf(Focus(0), Repeat), r.got, "and the next touch works normally")
    }
}
