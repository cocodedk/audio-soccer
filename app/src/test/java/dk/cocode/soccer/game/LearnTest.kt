package dk.cocode.soccer.game

import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnTest {
    private val s = gameStrings(Lang.EN)
    private val events = mutableListOf<String>()
    private val lines = mutableListOf<Line>()
    private var speaking = false
    private var readerOn = false
    private val sink = FakeSink { events += "play $it" }
    private val learn = Learn(s, { events += "say ${it.text}"; lines += it }, { speaking }, sink) { readerOn }

    private fun runToEnd(dt: Double = 0.05): Int {
        var frames = 0
        while (!learn.update(dt)) frames++
        return frames
    }

    @Test
    fun namesEachSoundThenPlaysIt() {
        learn.start()
        runToEnd()
        val ticks = List(3) { "play TICK" }
        val expected = listOf("say ${s.learnCalls}", "say ${s.callAhead}", "say ${s.callLeft}", "say ${s.callRight}", "say ${s.callBehind}") +
            listOf("say ${s.learnTarget}", "say ${s.learnAhead}") + ticks + listOf("say ${s.learnBehind}") + ticks +
            listOf("say ${s.learnChimeNear}") + List(5) { "play CHIME" } + listOf("say ${s.learnBoard}", "play BOARD") +
            listOf("say ${s.learnFeet}") + List(4) { "play STEP" } + listOf("say ${s.learnVoy}", "say ${s.voy}", "say ${s.learnEnd}")
        assertEquals(expected, events)
    }

    @Test
    fun theBallBehindPlaysDullAndTheChimeQuickens() {
        learn.start()
        runToEnd()
        assertEquals(listOf(false, false, false, true, true, true), sink.behinds.take(6))
        val chimes = sink.stretches.drop(6).take(5)
        assertTrue(chimes.zipWithNext().all { (a, b) -> b <= a })
        assertEquals(1.0, chimes.first(), 0.001)
        assertEquals(0.25, chimes.last(), 0.001)
    }

    @Test
    fun theVoyIsSaidHighAndShrill() {
        learn.start()
        runToEnd()
        val voy = lines.single { it.text == s.voy }
        assertEquals(1.4f, voy.pitch, 0.0f)
        assertEquals(Priority.HIGH, voy.priority)
    }

    @Test
    fun aSoundWaitsForItsNameToBeSpoken() {
        learn.start()
        speaking = true
        repeat(100) { learn.update(0.05) }
        assertEquals(listOf("say ${s.learnCalls}"), events)
        speaking = false
        learn.update(0.05)
        assertEquals("say ${s.callAhead}", events.last())
    }

    @Test
    fun withAScreenReaderOnTheNameIsGivenTimeByLength() {
        readerOn = true
        learn.start()
        var seconds = 0.0
        while (events.size == 1) {
            learn.update(0.05)
            seconds += 0.05
        }
        assertEquals(1.0 + 0.1 * s.learnCalls.length, seconds, 0.1)
    }

    @Test
    fun itEndsOnceAndStaysEnded() {
        learn.start()
        runToEnd()
        val said = events.size
        assertTrue(learn.update(0.05))
        assertTrue(learn.update(0.05))
        assertEquals(said, events.size)
        assertFalse(lines.isEmpty())
    }
}
