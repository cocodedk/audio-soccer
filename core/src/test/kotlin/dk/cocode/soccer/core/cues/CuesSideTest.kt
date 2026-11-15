package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.ballWorld
import dk.cocode.soccer.core.text.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// From prototype/test/cues-side.test.mjs, through a real speech queue. Not ported: headphones mode (no side or board calls
// there), and the recorded clips (the app speaks the words).
class CuesSideTest {
    private val h = SpeechHarness()

    private fun calls() = h.spoken.filter { it in listOf("ahead", "left", "right", "behind") }

    private fun boards() = h.spoken.filter { "board" in it }

    @Test
    fun speaksTheSideOfTheBallThenWaitsAndNeverRepeatsAWord() {
        val w = ballWorld(Vec(20.0, 3.0))
        h.cues.reset(w)
        h.cues.update(w)
        assertEquals(listOf("left"), calls())
        h.finish()
        h.cues.update(w)
        assertEquals(listOf("left"), calls(), "the same side is not called again")
        w.ball = Vec(20.0, 17.0)
        h.t += 60
        h.cues.update(w)
        assertEquals(listOf("left"), calls(), "a change inside 1.5 s waits")
        h.t += 1500
        h.cues.update(w)
        assertEquals(listOf("left", "right"), calls())
    }

    @Test
    fun aBallBehindYouFromTheStartIsCalledBehindWithoutTheAmbientLine() {
        val w = ballWorld(Vec(5.0, 10.0))
        h.cues.reset(w)
        h.cues.update(w)
        assertEquals(listOf("behind"), calls())
        assertEquals(listOf("behind"), h.spoken, "the ball never passed behind, it started there")
    }

    @Test
    fun withTheBallTheTargetIsTheGoal() {
        val w = ballWorld(Vec(10.5, 3.0), hasBall = true) // a kick from y 3 passes the goal on the left: the goal is to the right
        h.cues.reset(w)
        h.cues.update(w)
        assertEquals(listOf("right"), calls())
    }

    @Test
    fun theWordsAreSpokenInThePlayersLanguage() {
        val fa = SpeechHarness(Lang.FA)
        val w = ballWorld(Vec(20.0, 3.0))
        fa.cues.reset(w)
        fa.cues.update(w)
        assertEquals(listOf("چپ"), fa.spoken)
    }

    @Test
    fun namesTheBoardWhenYouComeNearItOrBumpItOnce() {
        val w = ballWorld(Vec(20.0, 3.0))
        w.player = Vec(10.0, 3.0)
        h.cues.reset(w)
        h.cues.update(w)
        h.finishAll()
        assertEquals(emptyList<String>(), boards(), "nothing 3 m from the board")
        w.player = Vec(10.0, 1.2)
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board"), boards())
        h.cues.update(w)
        w.player = Vec(10.0, 0.3)
        w.playerTouching = Side.LEFT
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board"), boards(), "a bump just after the call is not called again")
        w.player = Vec(10.0, 18.8)
        w.playerTouching = null
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board"), boards(), "within the gap the next board call is dropped")
    }

    @Test
    fun aBoardCallComesAgainAfterTheGapAndAStepOffABumpIsNotNews() {
        val w = ballWorld(Vec(20.0, 10.0))
        w.player = Vec(10.0, 0.3)
        h.cues.reset(w) // a round that starts at the board names nothing
        h.cues.update(w)
        h.finishAll()
        assertEquals(emptyList<String>(), boards())
        w.playerTouching = Side.LEFT // a bump is news
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board"), boards())
        w.playerTouching = null // steps off the bump, still near the board
        h.t += 4000
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board"), boards())
        w.player = Vec(10.0, 18.8)
        h.cues.update(w)
        h.finishAll()
        assertEquals(listOf("left board", "right board"), boards())
    }
}
