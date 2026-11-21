package dk.cocode.soccer.core.speech

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/speech.test.mjs: how long a line may wait, how often a key may be spoken, and which
// report of the engine ends which line.
class SpeechQueueTimingTest : SpeechQueueCase() {
    @Test
    fun aLineStillUnsaidAfterItsWaitIsDroppedNeverSaidLate() {
        queue.say(Line("busy"))
        queue.say(Line("kept", waitMs = 1500))
        queue.say(Line("dropped", waitMs = 1000))
        t = 1200
        voice.finish()
        queue.tick()
        assertEquals(listOf("busy", "kept"), voice.spoken)
        t = 5000
        queue.say(Line("fresh"))
        queue.say(Line("expired", waitMs = 100))
        t = 5200
        finishAll()
        assertEquals(listOf("busy", "kept", "fresh"), voice.spoken)
    }

    @Test
    fun aLineThatExpiresUnsaidDoesNotStartItsRateLimit() {
        queue.say(Line("busy"))
        queue.say(Line("late", key = "late", gapMs = 4000, waitMs = -1)) // queued, then dropped unsaid
        finishAll()
        assertFalse("late" in voice.spoken)
        queue.say(Line("late again", key = "late", gapMs = 4000))
        finishAll()
        assertTrue("late again" in voice.spoken, "not blocked by the line that never played")
        queue.say(Line("too soon", key = "late", gapMs = 4000))
        finishAll()
        assertFalse("too soon" in voice.spoken, "but once spoken, the gap holds")
    }

    @Test
    fun gapRateLimitsAKeyMeasuredFromWhenItWasLastSpoken() {
        queue.say(Line("a", key = "k", gapMs = 1000))
        voice.finish()
        t = 999
        queue.say(Line("blocked", key = "k", gapMs = 1000))
        t = 1000
        queue.say(Line("allowed", key = "k", gapMs = 1000))
        queue.say(Line("other key", key = "j", gapMs = 1000))
        queue.say(Line("no key", gapMs = 1000))
        finishAll()
        assertEquals(listOf("a", "allowed", "other key", "no key"), voice.spoken)
    }

    @Test
    fun theDoneOfALineThatWasCutDoesNotEndTheLineThatReplacedIt() {
        queue.say(Line("a"))
        queue.say(Line("b", Priority.HIGH))
        queue.say(Line("c", Priority.HIGH))
        voice.dones[0]() // the engine reports the cut line late
        queue.tick()
        assertEquals(listOf("a", "b"), voice.spoken, "b is still being spoken")
        voice.finish()
        queue.tick()
        assertEquals(listOf("a", "b", "c"), voice.spoken)
    }

    @Test
    fun aVoiceThatFinishesAtOnceNeverBlocksTheQueue() {
        voice.instant = true
        queue.say(Line("a"))
        queue.say(Line("b"))
        assertEquals(listOf("a", "b"), voice.spoken)
    }
}
