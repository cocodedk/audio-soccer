package dk.cocode.soccer.core.speech

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// From prototype/test/speech.test.mjs: who is spoken first, who cuts whom, who is replaced. The trial's browser
// workarounds (the pause after a cancel, the 200 ms pump, stuck-line detection, the live regions) and its onLine
// hook have no counterpart here.
class SpeechQueueTest : SpeechQueueCase() {
    private fun help(text: String) = queue.say(Line(text, key = "help", many = true, waitMs = 30000))

    @Test
    fun helpLinesShareOneKeyAndAllStayQueuedAnAmbientCueDoesNotWipeThem() {
        listOf("h1", "h2", "h3").forEach(::help)
        queue.say(Line("Ball behind you", Priority.LOW, key = "behind", gapMs = 4000))
        queue.say(Line("Middle third", Priority.LOW))
        finishAll()
        assertEquals(listOf("h1", "h2", "h3", "Ball behind you", "Middle third"), voice.spoken)
    }

    @Test
    fun anEventHighPriorityCutsHelpOffAndDropsTheRestOfIt() {
        listOf("h1", "h2", "h3").forEach(::help)
        queue.say(Line("Got it.", Priority.HIGH))
        assertEquals(1, voice.stops)
        finishAll()
        assertEquals(listOf("h1", "Got it."), voice.spoken)
    }

    @Test
    fun clearDropsEverythingWaitingAndStopsTheVoice() {
        listOf("h1", "h2").forEach(::help)
        queue.clear()
        assertEquals(1, voice.stops)
        queue.tick()
        assertEquals(listOf("h1"), voice.spoken)
        queue.say(Line("after"))
        assertEquals(listOf("h1", "after"), voice.spoken, "the queue works again after a clear")
    }

    @Test
    fun aHighLineCutsASpeakingNormalLine() {
        queue.say(Line("a long line"))
        queue.say(Line("urgent", Priority.HIGH))
        assertEquals(listOf("a long line", "urgent"), voice.spoken)
        assertEquals(1, voice.stops)
    }

    @Test
    fun aLineOfEqualOrLowerPriorityWaitsForTheOneBeingSpoken() {
        queue.say(Line("first", Priority.HIGH))
        queue.say(Line("second", Priority.HIGH))
        queue.say(Line("third"))
        assertEquals(listOf("first"), voice.spoken)
        assertEquals(0, voice.stops)
        finishAll()
        assertEquals(listOf("first", "second", "third"), voice.spoken)
    }

    @Test
    fun aHigherLineDropsTheLowerOnesStillWaiting() {
        queue.say(Line("busy", Priority.HIGH))
        queue.say(Line("low", Priority.LOW))
        queue.say(Line("normal"))
        queue.say(Line("last", Priority.HIGH))
        finishAll()
        assertEquals(listOf("busy", "last"), voice.spoken)
    }

    @Test
    fun withoutManyANewerLineWithTheSameKeyReplacesOneStillQueued() {
        queue.say(Line("first", key = "k"))
        queue.say(Line("second", key = "k"))
        queue.say(Line("third", key = "k"))
        finishAll()
        assertEquals(listOf("first", "third"), voice.spoken)
    }

    @Test
    fun anOutcomeLineCutsOffALongAnswerInsteadOfExpiringBehindIt() {
        queue.say(Line("The ball is at 2 o'clock, 14 metres.", Priority.HIGH, key = "where"))
        t = 500
        queue.say(Line("Got it. 12.3 seconds.", Priority.HIGH, cut = true))
        assertEquals(listOf("The ball is at 2 o'clock, 14 metres.", "Got it. 12.3 seconds."), voice.spoken)
    }

    @Test
    fun anOutcomeLineIsNotLeftBehindAHighLineAlreadyWaiting() {
        queue.say(Line("Round one. Find the ball and score.", Priority.HIGH))
        t = 200
        queue.say(Line("The ball is at 2 o'clock.", Priority.HIGH, key = "where")) // waits: high does not cut high
        t = 600
        queue.say(Line("Goal! 4.1 seconds.", Priority.HIGH, cut = true))
        finishAll()
        assertEquals(listOf("Round one. Find the ball and score.", "Goal! 4.1 seconds."), voice.spoken)
    }

    @Test
    fun theLinesPitchReachesTheVoice() {
        queue.say(Line("Voy!", pitch = 1.4f))
        finishAll()
        queue.say(Line("plain"))
        assertEquals(listOf(1.4f, 1f), voice.pitches)
    }
}
