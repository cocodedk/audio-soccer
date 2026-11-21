package dk.cocode.soccer.core.speech

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// An engine service that dies mid-line sends no callback, so the voice never reports the line's end. A line is released
// once it has been speaking for more than 5000 ms plus 200 ms a character, measured on the queue's own clock.
class SpeechQueueStuckTest : SpeechQueueCase() {
    private fun next(text: String) = queue.say(Line(text, waitMs = 60_000))

    @Test
    fun aVoiceThatNeverReportsDoesNotBlockTheNextLinePastTheLimit() {
        queue.say(Line("abcde")) // 5000 + 5 * 200 = 6000 ms
        next("next")
        t = 6000
        queue.tick()
        assertEquals(listOf("abcde"), voice.spoken, "not yet: the limit is exceeded, not reached")
        assertEquals(0, voice.stops)
        t = 6001
        queue.tick()
        assertEquals(1, voice.stops, "the stuck line is stopped")
        assertEquals(listOf("abcde", "next"), voice.spoken)
    }

    @Test
    fun theLimitRunsFromWhenTheLineStartedSpeakingNotFromWhenItWasSaid() {
        queue.say(Line("first"))
        next("second") // said at 0, but only starts when the first has ended
        t = 4000
        voice.finish()
        queue.tick()
        t = 4000 + 5000 + 6 * 200 // the limit of "second", counted from 4000
        queue.tick()
        assertEquals(listOf("first", "second"), voice.spoken)
        assertEquals(0, voice.stops)
        t += 1
        queue.tick()
        assertEquals(1, voice.stops)
    }

    @Test
    fun aLineThatEndsInTimeIsUnaffected() {
        queue.say(Line("abcde"))
        next("next")
        t = 5999
        voice.finish()
        queue.tick()
        t = 7000 // far past the limit of the first line, well inside that of the second
        queue.tick()
        assertEquals(0, voice.stops)
        assertEquals(listOf("abcde", "next"), voice.spoken)
    }

    @Test
    fun theLateEndOfAReleasedLineDoesNotEndItsReplacement() {
        queue.say(Line("a"))
        next("b")
        next("c")
        t = 5201
        queue.tick()
        assertEquals(listOf("a", "b"), voice.spoken)
        voice.dones[0]() // the engine wakes up and reports the line it lost
        queue.tick()
        assertEquals(listOf("a", "b"), voice.spoken, "b is still being spoken")
        voice.finish()
        queue.tick()
        assertEquals(listOf("a", "b", "c"), voice.spoken)
    }

    @Test
    fun aStuckLineIsReleasedEvenWhenNothingWaitsAndTheQueueWorksAfterwards() {
        queue.say(Line("a"))
        t = 5201
        queue.tick()
        assertEquals(1, voice.stops)
        queue.say(Line("b"))
        assertEquals(listOf("a", "b"), voice.spoken)
    }

}
