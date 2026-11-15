package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Aim
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.KEEPER_X
import dk.cocode.soccer.core.sim.KickRecord
import dk.cocode.soccer.core.sim.Outcome
import dk.cocode.soccer.core.sim.PLAYER_SPEED
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.sim.dist
import dk.cocode.soccer.core.sim.seeded
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/i18n.test.mjs (the level texts) and game.test.mjs (the start lines), and the setup, stats and
// closing rules of levels.js.
class LevelsTest {
    private val en = gameStrings(Lang.EN)
    private val fa = gameStrings(Lang.FA)

    @Test
    fun level1StartsThePlayerAndAStillBallAtLeast10mApart() {
        repeat(200) { seed ->
            val s = setupRound(1, seeded(seed))
            assertFalse(s.hasBall)
            assertFalse(s.keeper)
            assertTrue(s.player.x in 3.0..37.0 && s.player.y in 3.0..17.0, "player ${s.player}")
            assertTrue(s.ball.x in 2.0..38.0 && s.ball.y in 2.0..18.0, "ball ${s.ball}")
            assertTrue(dist(s.player, s.ball) >= 10, "apart ${dist(s.player, s.ball)}")
        }
    }

    @Test
    fun levels2And3StartThePlayerWithTheBallInTheirOwnHalfAndTheKeeperOnlyInLevel3() {
        for (level in 2..3) {
            repeat(100) { seed ->
                val s = setupRound(level, seeded(seed))
                assertTrue(s.hasBall)
                assertEquals(level == 3, s.keeper)
                assertTrue(s.player.x in 4.0..16.0 && s.player.y in 4.0..16.0, "player ${s.player}")
                assertEquals(Vec(s.player.x + 0.5, s.player.y), s.ball)
            }
        }
    }

    @Test
    fun theDrawsGoInTheOrderTheTrialTookThem() {
        // seeded(11) gives 0.5115870486479253, 0.5299464082345366: x first, then y
        val s = setupRound(2, seeded(11))
        close(s.player.x, 4 + 0.5115870486479253 * 12)
        close(s.player.y, 4 + 0.5299464082345366 * 12)
    }

    @Test
    fun aSetupBuildsItsWorldWithTheKeeperInGoalInLevel3() {
        val w = setupRound(3, seeded(1)).toWorld()
        assertEquals(Vec(KEEPER_X, GOAL.y), w.keeper?.pos)
        assertTrue(w.hasBall)
        assertNull(setupRound(2, seeded(1)).toWorld().keeper)
        assertFalse(setupRound(1, seeded(1)).toWorld().hasBall)
    }

    @Test
    fun newStatsRecordWhereTheBallWasAtTheStart() {
        val behind = newStats(RoundSetup(Vec(10.0, 10.0), Vec(7.0, 14.0), hasBall = false, keeper = false))
        close(behind.startDist, 5.0)
        assertTrue(behind.startBehind)
        assertFalse(newStats(RoundSetup(Vec(10.0, 10.0), Vec(20.0, 10.0), hasBall = false, keeper = false)).startBehind)
        assertEquals(0.0, behind.time)
        assertEquals(emptyList<KickRecord>(), behind.shots)
        assertNull(behind.ending)
        assertFalse(behind.success)
        assertNull(behind.efficiency)
    }

    private fun played(time: Double) =
        RoundStats(startDist = 20.0, startBehind = false, time = time, shots = listOf(KickRecord(18.0, 2.1, Aim.STRAIGHT)))

    @Test
    fun aFoundBallInLevel1HasAnEfficiencyOfTheStartDistanceOverARunAtFullSpeed() {
        val r = closeStats(1, played(8.0), Ending.SUCCESS)
        assertTrue(r.success)
        close(r.efficiency!!, 20 / (PLAYER_SPEED * 8.0))
        assertNull(closeStats(2, played(8.0), Ending.SUCCESS).efficiency, "only level 1 has one")
        assertNull(closeStats(1, played(0.0), Ending.SUCCESS).efficiency, "no time, no efficiency")
    }

    @Test
    fun aRoundThatIsNotWonKeepsItsTimeAndItsEndingAndHasNoEfficiency() {
        for (ending in listOf(Ending.GIVE_UP, Ending.TIMEOUT)) {
            val r = closeStats(1, played(5.0), ending)
            assertEquals(ending, r.ending)
            assertFalse(r.success)
            assertNull(r.efficiency)
            assertEquals(5.0, r.time)
        }
    }

    @Test
    fun aKickThatNeverReachedTheLineStoppedShort() {
        assertEquals(Outcome.SHORT, closeStats(2, played(5.0), Ending.GIVE_UP).shots[0].outcome)
        val settled = RoundStats(1.0, false, shots = listOf(KickRecord(18.0, 0.0, Aim.LEFT, Outcome.POST)))
        assertEquals(Outcome.POST, closeStats(2, settled, Ending.GIVE_UP).shots[0].outcome)
    }

    @Test
    fun closingARoundDoesNotChangeTheOneItWasGiven() {
        val open = played(5.0)
        val closed = closeStats(1, open, Ending.SUCCESS)
        assertFalse(open.success)
        assertNull(open.shots[0].outcome)
        assertNotNull(closed.shots[0].outcome)
    }

    @Test
    fun aRoundIntroIsTheRoundAndTheTaskAndNothingElseInEveryLevel() {
        assertEquals("Round 1. Find the ball.", startLine(1, 1, en))
        assertEquals("Round 2. Score a goal.", startLine(2, 2, en))
        assertEquals("Round 1. Beat the keeper.", startLine(3, 1, en), "level 3 too")
        assertEquals("دور ۱. توپ را پیدا کنید.", startLine(1, 1, fa))
        assertEquals("دور ۳. گل بزنید.", startLine(2, 3, fa))
    }

    private fun roundOf(success: Boolean, time: Double) = closeStats(
        1, RoundStats(startDist = 5.0, startBehind = false, time = time), if (success) Ending.SUCCESS else Ending.GIVE_UP,
    )

    @Test
    fun theSummaryListsEachTimeOrTheMissWordInPersianToo() {
        val rounds = listOf(roundOf(true, 12.44), roundOf(false, 40.0), roundOf(true, 10.0))
        assertEquals("Level 1 done. Times: 12.4, not found, 10.0.", summarySpeech(1, rounds, en))
        assertEquals("مرحله ۱ تمام شد. زمان‌ها: ۱۲٫۴، پیدا نشد، ۱۰٫۰.", summarySpeech(1, rounds, fa))
        assertEquals("Level 2 done. Times: not scored, not scored, not scored.", summarySpeech(2, List(ROUNDS) { roundOf(false, 1.0) }, en))
    }
}
