package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.input.PlayInput.KICK
import dk.cocode.soccer.core.input.PlayInput.WHERE
import dk.cocode.soccer.core.sim.Vec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// Beyond the trial's cases: a tap that waits for a resting finger is reported by tick, at most one input per call,
// the inclusive thresholds, a cancelled finger, and fingers the recognizer has not seen.
class PlayGesturesEdgeTest {
    private val r = PlayRig()

    @Test
    fun aTapBesideARestingFingerIsReportedWhenTheRestingFingerTimesOut() {
        r.down(1, 100, 400, 0)
        r.down(2, 200, 400, 40)
        r.up(2, 200, 400, 100)
        r.tick(TAP_MS)
        assertEquals(emptyList<PlayInput>(), r.got, "the resting finger may still lift in time")
        r.tick(TAP_MS + 1)
        assertEquals(listOf(KICK), r.got)
        r.up(1, 100, 400, 2000)
        assertEquals(listOf(KICK), r.got, "and the late lift adds nothing")
    }

    @Test
    fun twoTapsCompletingTogetherAreReportedOneAtATimeNeverLost() {
        r.down(1, 100, 400, 0) // rests
        r.down(2, 200, 400, 10)
        r.up(2, 200, 400, 60)
        r.down(3, 100, 400, 200) // rests; a chord of its own, 200 ms after the first
        r.down(4, 200, 400, 210)
        r.up(4, 200, 400, 260)
        r.tick(520) // both resting fingers have timed out
        assertEquals(listOf(KICK), r.got)
        r.tick(521)
        assertEquals(listOf(KICK, KICK), r.got)
        r.tick(522)
        assertEquals(listOf(KICK, KICK), r.got)
    }

    @Test
    fun theThresholdsAreInclusiveWhereTheTrialHadThemSo() {
        r.tap(1, 0, ms = TAP_MS) // lifted at exactly the tap time
        r.tap(2, 1000, ms = TAP_MS + 1)
        assertEquals(listOf(KICK), r.got)
        r.down(3, 100, 400, 2000)
        r.move(3, 100, 400 - DEAD_DP.toInt() + 1, 2010)
        assertEquals(Vec.ZERO, r.dir)
        r.move(3, 100, 400 - DEAD_DP.toInt(), 2020) // exactly the dead zone has moved
        assertEquals(Vec(1.0, 0.0), r.dir)
    }

    @Test
    fun fingersLandingWithinTheChordTimeFormOneTapAndLaterOnesDoNot() {
        r.down(1, 100, 400, 0)
        r.down(2, 200, 400, CHORD_MS)
        r.up(1, 100, 400, 200)
        r.up(2, 200, 400, 210)
        assertEquals(listOf(WHERE), r.got)
        r.down(3, 100, 400, 1000)
        r.down(4, 200, 400, 1000 + CHORD_MS + 1)
        r.up(3, 100, 400, 1200)
        r.up(4, 200, 400, 1210)
        assertEquals(listOf(WHERE, KICK, KICK), r.got)
    }

    @Test
    fun aLiftReportsATapWhoseRestingFingerTimedOutBefore() {
        r.down(1, 100, 400, 0) // rests
        r.down(2, 200, 400, 10)
        r.up(2, 200, 400, 60)
        r.down(3, 300, 400, 200)
        r.move(3, 300, 300, 250)
        r.up(3, 300, 300, TAP_MS + 50) // a drag, no tap of its own
        assertEquals(listOf(KICK), r.got)
    }

    @Test
    fun aLandingReportsATapWhoseRestingFingerTimedOutBefore() {
        r.down(1, 100, 400, 0) // rests
        r.down(2, 200, 400, 10)
        r.up(2, 200, 400, 60)
        r.down(3, 300, 400, TAP_MS + 50)
        assertEquals(listOf(KICK), r.got)
    }

    @Test
    fun aRestingFingerThatStartsToSteerLetsTheTapBesideItFinish() {
        r.down(1, 100, 400, 0)
        r.down(2, 200, 400, 10)
        r.up(2, 200, 400, 60)
        r.move(1, 100, 300, 100)
        assertEquals(listOf(KICK), r.got)
        assertEquals(Vec(1.0, 0.0), r.dir)
    }

    @Test
    fun onlyTheFirstFingerToMoveSteersUntilItLifts() {
        r.down(1, 200, 500, 0)
        r.move(1, 200, 420, 40)
        r.down(2, 100, 500, 100)
        r.move(2, 160, 500, 150)
        assertEquals(Vec(1.0, 0.0), r.dir)
        r.up(1, 200, 420, 200)
        assertEquals(Vec.ZERO, r.dir)
        r.move(2, 170, 500, 300)
        assertEquals(Vec(0.0, 1.0), r.dir)
    }

    @Test
    fun aCancelledFingerLetsTheTapBesideItFinish() {
        r.down(1, 100, 400, 0)
        r.down(2, 200, 400, 30)
        r.up(2, 200, 400, 80)
        assertEquals(emptyList<PlayInput>(), r.got)
        r.cancel(1, 90)
        assertEquals(listOf(KICK), r.got)
    }

    @Test
    fun aFingerLandingForAnIdAlreadyDownReplacesIt() {
        r.down(1, 200, 400, 0)
        r.move(1, 200, 300, 40)
        assertEquals(Vec(1.0, 0.0), r.dir)
        r.down(1, 100, 400, 1000) // the lift was missed
        assertEquals(Vec.ZERO, r.dir)
        r.up(1, 101, 401, 1050)
        assertEquals(listOf(KICK), r.got)
    }

    @Test
    fun fingersTheRecognizerHasNotSeenAreIgnored() {
        r.move(9, 100, 100, 0)
        r.up(9, 100, 100, 50)
        r.cancel(9, 60)
        r.tick(1000)
        assertEquals(emptyList<PlayInput>(), r.got)
        assertEquals(Vec.ZERO, r.dir)
    }

    @Test
    fun resetForgetsEveryFinger() {
        r.down(1, 200, 400, 0)
        r.move(1, 200, 300, 40)
        r.down(2, 100, 400, 50)
        r.reset()
        assertEquals(Vec.ZERO, r.dir)
        r.up(1, 200, 300, 100)
        r.up(2, 100, 400, 110)
        assertEquals(emptyList<PlayInput>(), r.got)
        r.tap(3, 500)
        assertEquals(listOf(KICK), r.got, "the next touch works normally")
    }
}
