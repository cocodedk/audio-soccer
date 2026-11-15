package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.ballWorld
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings
import dk.cocode.soccer.core.text.guideText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The lines the cues hand to say(), with the priority, key, gap and wait of the trial's call sites in cues.js.
class CuesTest {
    private val en = gameStrings(Lang.EN)
    private val lines = mutableListOf<Line>()
    private var now = 0L
    private val cues = Cues(en, { lines += it }) { now }

    private fun texts() = lines.map { it.text }

    @Test
    fun aSideCallIsALowPriorityCutLineWithAShortWait() {
        val w = ballWorld(Vec(20.0, 3.0))
        cues.reset(w)
        cues.update(w)
        assertEquals(listOf(Line("left", Priority.LOW, cut = true, key = "side", waitMs = 800)), lines)
    }

    @Test
    fun aBoardCallIsALowPriorityLineWithAGapAndAShortWait() {
        val w = ballWorld(Vec(20.0, 10.0))
        w.player = Vec(10.0, 18.0)
        cues.reset(w)
        w.player = Vec(10.0, 19.0)
        cues.update(w)
        assertEquals(Line("right board", Priority.LOW, key = "board", gapMs = 3000, waitMs = 600), lines.first())
    }

    @Test
    fun theThirdIsNamedWhenYouCrossIntoItWithHalfAMetreOfHysteresis() {
        val w = ballWorld(Vec(10.0, 10.0))
        cues.reset(w)
        for (x in listOf(13.5, 13.9, 13.5, 12.8)) {
            w.player = Vec(x, 10.0)
            w.ball = Vec(x, 10.0)
            cues.update(w)
        }
        assertEquals(listOf(Line(en.thirds[1]), Line(en.thirds[0])), lines.filter { it.text in en.thirds })
        w.player = Vec(30.0, 10.0) // a jump over a whole third lands in the right one
        w.ball = w.player
        cues.update(w)
        assertEquals(en.thirds[2], lines.last { it.text in en.thirds }.text)
    }

    @Test
    fun theBallPassingBehindIsSaidOnceAndTheSideCallBehindDoesNotRepeatIt() {
        val w = ballWorld(Vec(10.5, 10.0))
        cues.reset(w)
        w.player = Vec(12.0, 10.0) // the ball is now 1.5 m behind
        cues.update(w)
        assertEquals(Line(en.behind, key = "behind", gapMs = 4000), lines.single { it.text == en.behind })
        assertEquals(emptyList<String>(), texts().filter { it == "behind" }, "the call says it already")
        w.player = Vec(14.0, 10.0)
        cues.update(w)
        assertEquals(1, texts().count { it == en.behind }, "once it is behind it is not said again")
        w.player = Vec(8.0, 10.0) // the ball is ahead again, then passes behind once more
        cues.update(w)
        w.player = Vec(14.0, 10.0)
        cues.update(w)
        assertEquals(2, texts().count { it == en.behind })
    }

    @Test
    fun withTheBallNothingIsBehindAndInTheFinalThirdTheGoalGuideIsSpoken() {
        val w = ballWorld(Vec(10.5, 10.0), hasBall = true)
        cues.reset(w)
        w.player = Vec(14.0, 10.0)
        cues.update(w)
        assertEquals(emptyList<String>(), texts().filter { it == en.behind }, "you have the ball, so it is not behind you")
        w.player = Vec(30.0, 4.0)
        w.ball = Vec(30.5, 4.0)
        cues.update(w)
        assertEquals(Line(guideText(w.player, en, w.ball), key = "guide", gapMs = 6000), lines.single { it.key == "guide" })
    }
}
