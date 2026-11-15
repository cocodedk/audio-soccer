package dk.cocode.soccer.platform

import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.concurrent.thread

// Voice.speak and stop: the line goes to the engine, and its onDone fires exactly once whatever happens to it.
class TtsVoiceSpeakTest : TtsTestBase() {
    @Test
    fun speakFlushesTheQueueSetsThePitchAndLogsTheLine() {
        val voice = voice()
        voice.say("hello", 1.4f)
        val spoken = engine.spoken.single()
        assertEquals("hello", spoken.text)
        assertEquals(TextToSpeech.QUEUE_FLUSH, spoken.queueMode)
        assertEquals(140, params.getInt("pitch"))
        assertEquals(listOf("hello"), speechLog())
    }

    @Test
    fun everyLineSetsItsOwnPitchAndNeverTheSpeechRate() {
        val voice = voice()
        voice.say(pitch = 0.5f)
        assertEquals(50, params.getInt("pitch"))
        assertFalse(params.containsKey("rate"))
    }

    @Test
    fun onDoneFiresOnceWhenTheLineEnds() {
        voice().say()
        progress.onStart(lastId())
        idle()
        assertEquals(0, done)
        progress.onDone(lastId())
        progress.onDone(lastId())
        idle()
        assertEquals(1, done)
    }

    // The engine reports one way of ending a line, and then, as engines do, another.
    private fun assertEndsOnce(report: (String) -> Unit) {
        voice().say()
        report(lastId())
        progress.onDone(lastId())
        idle()
        assertEquals(1, done)
    }

    @Test
    fun onDoneFiresOnceWhenTheEngineReportsAnError() = assertEndsOnce { progress.onError(it, TextToSpeech.ERROR_SYNTHESIS) }

    @Test
    fun onDoneFiresOnceWhenTheEngineCutsTheLineOff() = assertEndsOnce { progress.onStop(it, true) }

    @Test
    fun aReportWithoutAnIdChangesNothing() {
        voice().say()
        progress.onDone(null)
        idle()
        assertEquals(0, done)
    }

    @Test
    fun stopStopsTheEngineAndEndsTheLineOnce() {
        val voice = voice()
        voice.say()
        val stopped = engine.stops
        voice.stop()
        assertEquals(stopped + 1, engine.stops)
        idle()
        assertEquals(1, done)
        progress.onStop(lastId(), true)
        progress.onDone(lastId())
        idle()
        assertEquals(1, done)
    }

    @Test
    fun aNewLineFlushesTheOldOneAndEndsIt() {
        var first = 0
        var second = 0
        val voice = voice()
        voice.speak("a", 1f) { first++ }
        val firstId = lastId()
        voice.speak("b", 1f) { second++ }
        idle()
        assertEquals(listOf(1, 0), listOf(first, second))
        progress.onStop(firstId, true)
        idle()
        assertEquals(1, first)
        progress.onDone(lastId())
        idle()
        assertEquals(1, second)
    }

    @Test
    fun aLineBeforeTheEngineIsReadyStillEnds() {
        voice(ready = false).say()
        assertEquals(0, done)
        idle()
        assertEquals(1, done)
        assertTrue(engine.spoken.isEmpty())
        assertTrue(speechLog().isEmpty())
    }

    @Test
    fun aLineWithNoEngineStillEnds() {
        voice(engines = false).say()
        idle()
        assertEquals(1, done)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun aLineTheEngineRefusesStillEndsOnce() {
        val voice = voice()
        engine.speakResult = TextToSpeech.ERROR
        voice.say()
        assertEquals(0, done)
        idle()
        assertEquals(1, done)
        assertTrue(speechLog().isEmpty())
    }

    @Test
    fun shutdownReleasesTheEngineAndEndsTheLine() {
        val voice = voice()
        voice.say()
        voice.shutdown()
        assertTrue(engine.isShutdown)
        idle()
        assertEquals(1, done)
    }

    // Whoever waits for the engine to settle is let go, and a line said to it afterwards is dropped.
    @Test
    fun aShutDownEngineIsNoEngine() {
        val voice = voice()
        voice.shutdown()
        assertEquals(TtsState.NO_ENGINE, voice.state.value)
        voice.say()
        idle()
        assertEquals(1, done)
        assertTrue(engine.spoken.isEmpty())
    }

    @Test
    fun onDoneRunsOnTheMainThreadWhateverThreadTheEngineReportsFrom() {
        var ranOn: Thread? = null
        voice().speak("a", 1f) { ranOn = Thread.currentThread() }
        val id = lastId()
        thread { progress.onDone(id) }.join()
        assertNull(ranOn)
        idle()
        assertSame(Thread.currentThread(), ranOn)
    }
}
