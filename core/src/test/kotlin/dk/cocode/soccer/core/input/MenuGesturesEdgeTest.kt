package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.input.MenuInput.Focus
import dk.cocode.soccer.core.input.MenuInput.Next
import dk.cocode.soccer.core.input.MenuInput.Repeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// How the menu recognizer treats what the five rules leave out: strokes that mean nothing, extra fingers, the clock
// running between events, and fingers it has not seen.
class MenuGesturesEdgeTest {
    private val r = MenuRig()
    private val nothing = emptyList<MenuInput>()

    @Test
    fun aStrokeThatIsNeitherATapNorASwipeYieldsNothing() {
        r.down(1, 100, 400, 0)
        r.up(1, 100 + SWIPE_DP.toInt() - 1, 400, 150) // too short for a swipe, too long for a tap
        r.down(1, 100, 400, 1000)
        r.up(1, 100 + DEAD_DP.toInt(), 400, 1100) // exactly the dead zone has moved
        r.down(1, 100, 400, 2000)
        r.up(1, 300, 400, 2000 + TAP_MS + 1) // a swipe too slow, and no tick came to make it an exploration
        r.down(1, 100, 400, 3000)
        r.up(1, 100, 400, 3000 + TAP_MS + 1) // a tap too slow
        r.tick(9000)
        assertEquals(nothing, r.got)
    }

    @Test
    fun aFingerThatWanderedOutAndBackIsNotATap() {
        r.down(1, 200, 400, 0)
        r.move(1, 200, 330, 50)
        r.move(1, 201, 401, 100)
        r.up(1, 201, 401, 150)
        r.tick(9000)
        assertEquals(nothing, r.got)
    }

    @Test
    fun aHoldIsAlsoSeenByAMoveWhenNoTickCameInBetween() {
        r.down(1, 100, 100, 0)
        r.move(1, 100, 105, TAP_MS)
        assertEquals(nothing, r.got)
        r.move(1, 100, 110, TAP_MS + 1)
        assertEquals(listOf(Focus(0)), r.got)
    }

    @Test
    fun aSecondFingerLandingDuringAnExplorationOrASwipeIsIgnored() {
        r.explore(1, 0, 100)
        r.down(2, 200, 800, 400)
        r.move(2, 200, 850, 450)
        r.up(2, 200, 850, 500)
        r.move(1, 100, 450, 600) // the first finger goes on exploring
        assertEquals(listOf(Focus(0), Focus(1)), r.got)
        r.up(1, 100, 450, 700)
        r.down(3, 100, 400, 2000)
        r.down(4, 100, 500, 2010)
        r.up(4, 250, 500, 2050)
        r.up(3, 250, 400, 2100)
        r.tick(9000)
        assertEquals(listOf(Focus(0), Focus(1), Next), r.got)
    }

    @Test
    fun anotherFingerDuringATapLeavesTheFirstFingersTapAlone() {
        r.down(1, 200, 450, 0)
        r.down(2, 200, 100, 20)
        r.up(2, 200, 100, 60)
        r.up(1, 200, 450, 80)
        r.tick(9000)
        assertEquals(listOf(Repeat), r.got)
    }

    @Test
    fun liftingAnExploringFingerIsNoTapAndStartsNoDoubleTap() {
        r.explore(1, 0, 100)
        r.up(1, 200, 100, 400)
        r.tap(2, 500, 700)
        r.tick(9000)
        assertEquals(listOf(Focus(0), Repeat), r.got)
    }

    @Test
    fun anExploringFingerLiftedByAnEarlierEventTimeIsStillNoSwipe() {
        r.down(1, 100, 400, 0)
        r.tick(1000) // the frame clock ran ahead of the lift's event time
        r.up(1, 250, 400, 200)
        r.tick(9000)
        assertEquals(listOf(Focus(1)), r.got)
    }

    @Test
    fun aSwipeAfterATapIsASwipeAndTheTapIsForgotten() {
        r.tap(1, 0, 450)
        r.down(2, 100, 400, 150)
        r.up(2, 250, 400, 250)
        r.tick(9000)
        assertEquals(listOf(Next), r.got)
    }

    @Test
    fun anExplorationAfterATapIsTheOnlyFocus() {
        r.tap(1, 0, 100)
        r.explore(2, 150, 800)
        r.up(2, 200, 800, 700)
        r.tick(9000)
        assertEquals(listOf(Focus(2)), r.got)
    }

    @Test
    fun aTapWhoseWindowHasPassedIsReportedWhenTheNextFingerLands() {
        r.tap(1, 0, 450) // lifted at 80, and no tick came
        r.down(2, 100, 100, 80 + DOUBLE_MS + 1)
        assertEquals(listOf(Repeat), r.got)
        r.up(2, 100, 100, 80 + DOUBLE_MS + 60)
        r.tick(9000)
        assertEquals(listOf(Repeat, Repeat), r.got)
    }

    @Test
    fun fingersTheRecognizerHasNotSeenAreIgnored() {
        r.move(9, 100, 100, 0)
        r.up(9, 100, 300, 50)
        r.tick(1000)
        assertEquals(nothing, r.got)
    }

    @Test
    fun aFingerLandingForAnIdAlreadyDownStartsItsStrokeAgain() {
        r.down(1, 100, 400, 0)
        r.down(1, 300, 700, 1000) // the lift was missed
        r.tick(1000 + TAP_MS + 1) // the old stroke would explore where it began, in band 1
        assertEquals(listOf(Focus(2)), r.got)
    }

    @Test
    fun resetForgetsTheStrokeAndTheTapWaitingForItsSecond() {
        r.tap(1, 0, 450)
        r.down(2, 100, 100, 10)
        r.reset()
        r.up(2, 250, 100, 100)
        r.tick(9000)
        assertEquals(nothing, r.got)
        r.tap(3, 10000, 700)
        r.tick(11000)
        assertEquals(listOf(Repeat), r.got, "the next touch works normally")
    }

    @Test
    fun anEmptyMenuHasNoBandToThrowAbout() {
        val rig = MenuRig(count = 0)
        rig.explore(1, 0, 450)
        assertEquals(listOf(Focus(0)), rig.got)
    }
}
