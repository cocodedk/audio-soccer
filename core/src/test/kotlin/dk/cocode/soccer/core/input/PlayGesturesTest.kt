package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.input.PlayInput.KICK
import dk.cocode.soccer.core.input.PlayInput.PAUSE
import dk.cocode.soccer.core.input.PlayInput.WHERE
import dk.cocode.soccer.core.sim.Vec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

// The play rules of prototype/phone/js/gestures.js, case for case (prototype/test/phone-gestures.test.mjs). Its snap8 case
// is in VecTest; the screen-to-pitch part is checked through dir here. Vec equality tells -0.0 from 0.0, so each
// direction is also an exact zero where it should be.
class PlayGesturesTest {
    private val r = PlayRig()
    private val forward = Vec(1.0, 0.0)
    private val right = Vec(0.0, 1.0)

    @Test
    fun aDragInsideTheDeadZoneIsNoSteerBeyondItSteersFromWhereTheFingerLandedLiftingStops() {
        r.down(1, 200, 400, 0)
        r.move(1, 200, 400 - (DEAD_DP.toInt() - 4), 30)
        assertEquals(Vec.ZERO, r.dir)
        r.move(1, 210, 340, 60)
        assertEquals(forward, r.dir)
        r.move(1, 260, 400, 400) // the direction is measured from the landing point, not the last move
        assertEquals(right, r.dir)
        r.move(1, 205, 402, 450) // back inside the dead zone: stands still but is still the steering finger
        assertEquals(Vec.ZERO, r.dir)
        r.move(1, 200, 300, 500)
        assertEquals(forward, r.dir)
        r.up(1, 200, 300, 600)
        assertEquals(Vec.ZERO, r.dir)
        assertEquals(emptyList<PlayInput>(), r.got, "a drag is never a kick")
    }

    @Test
    fun screenUpIsTowardTheGoalAndScreenRightIsTheRight() {
        r.down(1, 200, 400, 0)
        r.move(1, 150, 400, 20)
        assertEquals(Vec(0.0, -1.0), r.dir)
        r.move(1, 200, 450, 40)
        assertEquals(Vec(-1.0, 0.0), r.dir)
        r.move(1, 250, 350, 60)
        val d = sqrt(0.5)
        assertEquals(Vec(d, d), r.dir)
    }

    @Test
    fun aTapWhileAnotherFingerSteersIsAKickAndTheSteeringGoesOn() {
        r.down(1, 200, 500, 0)
        r.move(1, 200, 420, 50)
        r.tap(2, 500, 100, 200)
        assertEquals(listOf(KICK), r.got)
        assertEquals(forward, r.dir)
        r.tap(3, 900, 300, 200) // and again
        assertEquals(listOf(KICK, KICK), r.got)
        assertEquals(forward, r.dir)
    }

    @Test
    fun twoFingersTappingTogetherAreOneWhereNotTwoKicks() {
        r.down(1, 100, 400, 0)
        r.down(2, 200, 400, 40)
        r.up(1, 100, 400, 110)
        assertEquals(emptyList<PlayInput>(), r.got, "nothing until both fingers are up")
        r.up(2, 200, 400, 130)
        assertEquals(listOf(WHERE), r.got)
    }

    @Test
    fun threeOrMoreFingersTappingTogetherArePause() {
        r.down(3, 100, 400, 1000)
        r.down(4, 200, 400, 1020)
        r.down(5, 300, 400, 1050)
        r.up(4, 200, 400, 1120)
        r.up(3, 100, 400, 1130)
        r.up(5, 300, 400, 1140)
        assertEquals(listOf(PAUSE), r.got)
        for (id in 1..4) r.down(id, 50 * id, 400, 2000L + id)
        for (id in 1..4) r.up(id, 50 * id, 400, 2100L + id)
        assertEquals(listOf(PAUSE, PAUSE), r.got)
    }

    @Test
    fun aTwoFingerTapWhileAThirdFingerSteersIsWhereAndTheSteeringGoesOn() {
        r.down(1, 200, 500, 0)
        r.move(1, 260, 500, 40)
        r.down(2, 100, 200, 300)
        r.down(3, 150, 200, 330)
        r.up(2, 100, 200, 400)
        r.up(3, 150, 200, 410)
        assertEquals(listOf(WHERE), r.got)
        assertEquals(right, r.dir)
    }

    @Test
    fun quickTapsOneAfterTheOtherAreSeparateKicksNotATwoFingerTap() {
        r.tap(1, 0)
        r.tap(2, 200)
        r.tap(1, 400)
        assertEquals(listOf(KICK, KICK, KICK), r.got)
    }

    @Test
    fun aFingerHeldStillOrLiftedAfterMovingIsNotATapAndACancelledFingerStopsSteering() {
        r.down(1, 200, 400, 0)
        r.tick(TAP_MS + 50)
        r.up(1, 200, 400, TAP_MS + 100)
        r.down(2, 200, 400, 1000)
        r.move(2, 200, 360, 1050)
        r.up(2, 200, 360, 1100)
        assertEquals(emptyList<PlayInput>(), r.got)
        r.down(3, 200, 400, 2000)
        r.move(3, 200, 300, 2050)
        assertEquals(forward, r.dir)
        r.cancel(3, 2060)
        assertEquals(Vec.ZERO, r.dir)
        assertEquals(emptyList<PlayInput>(), r.got)
    }

    @Test
    fun aFingerThatRestsFirstCanStillBecomeTheSteeringFinger() {
        r.down(1, 200, 400, 0)
        r.tick(TAP_MS * 2)
        r.move(1, 200, 340, TAP_MS * 2 + 20)
        assertEquals(forward, r.dir)
    }
}
