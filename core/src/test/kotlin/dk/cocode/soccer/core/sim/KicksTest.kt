package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/kicks.test.mjs.
class KicksTest {
    private fun kick(x: Double, y: Double) = Event.Kick(x, y)
    private fun goal() = Event.Goal(40.0, 10.0, 8.0)
    private fun record(x: Double, y: Double, dir: Vec = Vec.ZERO) = kickRecord(kick(x, y), dir)

    @Test
    fun aKickRecordsDistanceToTheLineSignedOffsetFromTheGoalCentreAndTheArrowHeld() {
        assertEquals(KickRecord(18.0, 12.1 - 10, Aim.STRAIGHT, null), record(22.0, 12.1))
        assertEquals(Aim.LEFT, record(30.0, 7.0, Vec(0.0, -1.0)).aim)
        assertEquals(Aim.RIGHT, record(30.0, 7.0, Vec(0.7, 0.7)).aim)
        assertTrue(record(30.0, 7.0).offset < 0, "left of centre is negative")
    }

    @Test
    fun theFirstEventAtTheLineSettlesTheKickLaterOnesDoNotChangeIt() {
        val cases = listOf(
            goal() to Outcome.GOAL,
            Event.Save(40.0, 10.0, 8.0) to Outcome.SAVED,
            Event.Post(40.0, 8.5, 8.0) to Outcome.POST,
            Event.EndWall(40.0, 5.0, 8.0, Side.LEFT) to Outcome.WIDE_LEFT,
            Event.EndWall(40.0, 15.0, 8.0, Side.RIGHT) to Outcome.WIDE_RIGHT,
        )
        for ((e, want) in cases) {
            val ks = listOf(record(30.0, 10.0))
            settleKick(ks, Event.Board(Side.LEFT, 20.0, 0.0, 6.0))
            settleKick(ks, Event.EndWall(40.0, 10.0, 8.0, null))
            assertNull(ks[0].outcome, "boards and a dribbled ball settle nothing")
            settleKick(ks, e)
            settleKick(ks, goal())
            assertEquals(want, ks[0].outcome)
        }
        settleKick(emptyList(), goal()) // no kick to settle: nothing happens
    }

    @Test
    fun aNewKickBeforeTheLineOrTheEndOfTheRoundMeansShort() {
        val ks = listOf(record(5.0, 10.0))
        settleKick(ks, kick(12.0, 10.0))
        assertEquals(Outcome.SHORT, ks[0].outcome)
        val taken = listOf(record(3.0, 10.0))
        settleKick(taken, Event.Take(3.5, 10.0))
        settleKick(taken, goal())
        assertEquals(Outcome.SHORT, taken[0].outcome, "taken back and dribbled in is not a goal from that kick")
        val rebound = listOf(record(30.0, 10.0))
        settleKick(rebound, Event.Save(40.0, 10.0, 8.0))
        settleKick(rebound, Event.Take(37.0, 10.0))
        assertEquals(Outcome.SAVED, rebound[0].outcome, "a rebound taken after a save stays saved")
        val open = listOf(record(5.0, 10.0))
        assertEquals(Outcome.SHORT, closeKicks(open)[0].outcome)
        assertNull(open[0].outcome, "closeKicks leaves its input alone")
        assertEquals(emptyList<KickRecord>(), closeKicks(emptyList()))
        assertEquals(Outcome.GOAL, closeKicks(listOf(record(30.0, 10.0).apply { outcome = Outcome.GOAL }))[0].outcome)
    }
}
