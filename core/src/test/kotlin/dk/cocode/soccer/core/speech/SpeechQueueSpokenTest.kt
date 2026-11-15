package dk.cocode.soccer.core.speech

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Line.onSpoken, the trial's onSpoken: it fires when a line starts being spoken, and for no line that is only offered.
// And isSaying, the trial's: whether a line with a key is speaking or waiting.
class SpeechQueueSpokenTest : SpeechQueueCase() {
    private val fired = mutableListOf<String>()

    private fun line(text: String, priority: Priority = Priority.NORMAL, key: String? = null, gapMs: Long = 0, waitMs: Long = 1500) =
        Line(text, priority, key = key, gapMs = gapMs, waitMs = waitMs, onSpoken = { fired += text })

    @Test
    fun firesAtOnceForALineThatStartsAtOnce() {
        queue.say(line("now"))
        assertEquals(listOf("now"), fired)
        finishAll()
        assertEquals(listOf("now"), fired, "once")
    }

    @Test
    fun aQueuedLineFiresWhenItStartsNotWhenItIsQueued() {
        queue.say(line("busy"))
        queue.say(line("next"))
        assertEquals(listOf("busy"), fired)
        voice.finish()
        queue.tick()
        assertEquals(listOf("busy", "next"), fired)
    }

    @Test
    fun aLineDroppedUnsaidNeverFires() {
        queue.say(line("busy"))
        queue.say(line("late", waitMs = 800))
        t = 1000
        finishAll()
        assertEquals(listOf("busy"), voice.spoken)
        assertEquals(listOf("busy"), fired)
    }

    @Test
    fun aLineCutOffBeforeItStartsNeverFires() {
        queue.say(line("busy"))
        queue.say(line("low", Priority.LOW))
        queue.say(line("high", Priority.HIGH))
        finishAll()
        assertEquals(listOf("busy", "high"), fired)
    }

    @Test
    fun aLineReplacedByOneOfItsKeyNeverFires() {
        queue.say(line("busy"))
        queue.say(line("old", key = "side"))
        queue.say(line("new", key = "side"))
        finishAll()
        assertEquals(listOf("busy", "new"), fired)
    }

    @Test
    fun aLineHeldBackByItsGapNeverFires() {
        queue.say(line("first", key = "behind", gapMs = 4000))
        finishAll()
        t = 1000
        queue.say(line("again", key = "behind", gapMs = 4000))
        finishAll()
        assertEquals(listOf("first"), fired)
    }

    @Test
    fun isSayingWhileALineWithTheKeyIsSpokenOrWaitsAndNotBeforeOrAfter() {
        assertFalse(queue.isSaying("side"))
        queue.say(line("busy"))
        queue.say(line("left", key = "side"))
        assertTrue(queue.isSaying("side"), "waiting")
        assertFalse(queue.isSaying("board"))
        voice.finish()
        queue.tick()
        assertTrue(queue.isSaying("side"), "speaking")
        finishAll()
        assertFalse(queue.isSaying("side"), "said")
    }

    @Test
    fun isSayingIsFalseForALineThatExpiredUnsaid() {
        queue.say(line("busy"))
        queue.say(line("late", key = "side", waitMs = 800))
        t = 1000
        voice.finish()
        queue.tick()
        assertFalse(queue.isSaying("side"), "dropped unsaid")
    }
}
