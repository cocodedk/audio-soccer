package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.createWorld
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

// From the buzz case of prototype/test/phone-logic.test.mjs.
class LineUpTest {
    private val take = listOf<Event>(Event.Take(10.5, 4.0))
    private val lineUp = LineUp()
    private val none = emptyList<Event>()

    @Test
    fun buzzPatternsAreAnImmediateShortBuzzAndADoubleBuzz() {
        assertArrayEquals(longArrayOf(0, 40), Buzz.TAKE.pattern)
        assertArrayEquals(longArrayOf(0, 40, 80, 40), Buzz.LINED_UP.pattern)
    }

    @Test
    fun onceWhenTheBallComesIntoReachADoubleBuzzOnLiningUpNeverWhileItStaysTheSame() {
        val w = createWorld(Vec(10.0, 4.0), Vec(10.5, 4.0), hasBall = true)
        lineUp.reset(w)
        assertNull(lineUp.update(w, none), "the round starts with the ball: no buzz")
        w.hasBall = false
        assertNull(lineUp.update(w, none))
        w.hasBall = true
        assertEquals(Buzz.TAKE, lineUp.update(w, take))
        assertNull(lineUp.update(w, none), "still in reach: quiet")
        w.ball = Vec(10.5, GOAL.y)
        assertEquals(Buzz.LINED_UP, lineUp.update(w, none))
        repeat(5) { assertNull(lineUp.update(w, none), "still lined up: quiet") }
        w.ball = Vec(10.5, 12.6) // just outside the band, inside the slack: still lined up
        assertNull(lineUp.update(w, none))
        w.ball = Vec(10.5, 12.4)
        assertNull(lineUp.update(w, none), "wobbling at the edge does not buzz again")
        w.ball = Vec(10.5, 13.5)
        assertNull(lineUp.update(w, none))
        w.ball = Vec(10.5, 12.0)
        assertEquals(Buzz.LINED_UP, lineUp.update(w, none), "off the line and back on is a new change")
        w.hasBall = false
        assertNull(lineUp.update(w, none))
        w.hasBall = true // taken back while lined up: the double buzz says both
        assertEquals(Buzz.LINED_UP, lineUp.update(w, take))
    }

    @Test
    fun aNewRoundStartsAfreshWithoutBuzzingForWhatItStartsWith() {
        val w = createWorld(Vec(10.0, 4.0), Vec(10.5, 4.0), hasBall = true)
        lineUp.reset(w)
        val next = createWorld(Vec(10.0, 10.0), Vec(10.5, 10.0), hasBall = true) // starts lined up
        lineUp.reset(next)
        assertNull(lineUp.update(next, none))
        val find = createWorld(Vec(10.0, 10.0), Vec(25.0, 10.0)) // level 1: the ball is somewhere else
        lineUp.reset(find)
        assertNull(lineUp.update(find, none))
    }
}
