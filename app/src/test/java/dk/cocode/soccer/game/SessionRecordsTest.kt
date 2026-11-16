package dk.cocode.soccer.game

import dk.cocode.soccer.core.game.Records
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The records: read from the phone onto the level options, and a level that sets one says so, keeps it and shows it.
class SessionRecordsTest {
    // Plays level 1 to its summary, finding the ball in every round.
    private fun Rig.findThreeBalls() {
        start(1)
        repeat(3) {
            runToBall()
            frames(3.1)
        }
    }

    @Test
    fun theRecordsKeptOnThePhoneAreReadOnTheLevelOptions() {
        val r = Rig(records = Records(mapOf(2 to 74), setOf(2)))
        assertEquals(listOf("Find the ball", "Score a goal. Best 7.4 seconds. All three rounds won."), r.items.take(2))
    }

    @Test
    fun aLevelThatSetsARecordSaysSoKeepsItAndShowsItOnItsOption() {
        val r = Rig()
        r.findThreeBalls()
        val summary = (r.screen as Screen.Summary).result.summary
        assertTrue(summary, "New record:" in summary && summary.endsWith("All three rounds won."))
        val best = checkNotNull(r.prefs.recordsNow().best[1])
        assertEquals(setOf(1), r.prefs.recordsNow().allWon)
        r.choose(r.en.backToMenu)
        assertTrue(r.items[0], r.items[0].startsWith("Find the ball. Best "))
        assertEquals(Records(mapOf(1 to best), setOf(1)), r.prefs.recordsNow())
    }

    @Test
    fun aRecordThatCannotBeWrittenIsStillOnTheMenuAndTheFailureIsLogged() {
        val r = Rig(failWrites = true)
        r.findThreeBalls()
        assertTrue(r.log.contains("settings not saved: disk full"))
        assertEquals(Records(), r.prefs.recordsNow())
        r.choose(r.en.backToMenu)
        assertTrue(r.items[0], r.items[0].startsWith("Find the ball. Best "))
    }
}
