package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The records: a level's fastest won round, in the tenths a time is spoken in, and the levels won three rounds out of three.
class RecordsTest {
    private val en = gameStrings(Lang.EN)
    private val fa = gameStrings(Lang.FA)

    private fun won(time: Double) = closeStats(2, RoundStats(1.0, false, time = time), Ending.SUCCESS)
    private fun lost(time: Double) = closeStats(2, RoundStats(1.0, false, time = time), Ending.GIVE_UP)
    private fun after(level: Int, rounds: List<RoundStats>, before: Records = Records()) = levelResult(level, rounds, before, en).records

    @Test
    fun aLevelKeepsItsFastestWonRoundInTenthsAndOnlyWonRoundsCount() {
        val r = after(2, listOf(won(9.04), lost(3.0), won(7.43)))
        assertEquals(Records(mapOf(2 to 74)), r)
        assertEquals(mapOf(2 to 74, 1 to 125), after(1, listOf(won(12.45), lost(1.0), lost(1.0)), r).best, "a level of its own")
    }

    @Test
    fun aRecordFallsOnlyToATimeThatSoundsFaster() {
        val r = Records(mapOf(2 to 74))
        assertEquals(r, after(2, listOf(won(7.36), lost(1.0), lost(1.0)), r), "7.36 is said as 7.4: no record")
        assertEquals(r, after(2, listOf(won(8.0), lost(1.0), lost(1.0)), r))
        assertEquals(mapOf(2 to 73), after(2, listOf(won(7.34), lost(1.0), lost(1.0)), r).best)
    }

    @Test
    fun aLevelWithNoWonRoundChangesNothingAndAllThreeWonIsKept() {
        val r = Records(mapOf(1 to 50), setOf(1))
        assertEquals(r, after(3, List(ROUNDS) { lost(2.0) }, r))
        assertEquals(setOf(1, 3), after(3, List(ROUNDS) { won(20.0) }, r).allWon)
        assertEquals(setOf(1), after(1, listOf(won(9.0), won(9.0), lost(9.0)), r).allWon, "once won, it stays")
    }

    @Test
    fun theSummaryAddsANewRecordAndAllThreeWonInBothLanguages() {
        val rounds = listOf(won(9.04), won(7.43), won(8.0))
        val first = levelResult(2, rounds, Records(), en)
        assertEquals("Level 2 done. Times: 9.0, 7.4, 8.0. New record: 7.4 seconds. All three rounds won.", first.summary)
        assertEquals(Records(mapOf(2 to 74), setOf(2)), first.records)
        assertEquals("مرحله ۲ تمام شد. زمان‌ها: ۹٫۰، ۷٫۴، ۸٫۰. رکورد جدید: ۷٫۴ ثانیه. هر سه دور را بردید.", levelResult(2, rounds, Records(), fa).summary)
        val slower = levelResult(2, listOf(won(9.0), lost(1.0), won(8.0)), first.records, en)
        assertEquals("Level 2 done. Times: 9.0, not scored, 8.0.", slower.summary)
        assertEquals(first.records, slower.records)
    }

    @Test
    fun aLevelsOptionIsItsNameThenItsRecords() {
        val r = Records(mapOf(2 to 74, 3 to 210), setOf(3))
        assertEquals("Find the ball", levelOption(1, "Find the ball", r, en), "no records: the name alone")
        assertEquals("Score a goal. Best 7.4 seconds.", levelOption(2, "Score a goal", r, en))
        assertEquals("Beat the keeper. Best 21.0 seconds. All three rounds won.", levelOption(3, "Beat the keeper", r, en))
        assertEquals("گل زدن. بهترین زمان ۷٫۴ ثانیه.", levelOption(2, "گل زدن", r, fa))
    }

    @Test
    fun aGameGivenTheRecordsSaysWhatItsLevelBeatTellsOfEachWonRoundAndHandsBackTheNewRecords() {
        val h = GameHarness(records = Records(mapOf(1 to 990, 2 to 74)))
        h.game.startLevel(1)
        h.walkToBall()
        assertEquals(1, h.wins, "the ball found wins the round")
        h.nextRound()
        h.walkToBall()
        h.nextRound()
        h.walkToBall()
        h.nextRound()
        assertEquals(3, h.wins)
        val result = h.done.single()
        val best = checkNotNull(result.records.best[1])
        assertTrue(best < 990)
        assertEquals(Records(mapOf(1 to best, 2 to 74), setOf(1)), result.records)
        assertEquals(result.summary, h.said.last().text)
        assertTrue("New record:" in result.summary && result.summary.endsWith("All three rounds won."), result.summary)
    }
}
