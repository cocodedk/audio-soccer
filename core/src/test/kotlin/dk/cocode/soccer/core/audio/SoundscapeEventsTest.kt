package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.cues.distanceGain
import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.ballWorld
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.sim.dist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The sound of each world event in prototype/js/soundscape.js (event()), speakers path only: the trial's gain times the
// distance gain from the player to where it happened, and ball sounds behind the player play dull.
class SoundscapeEventsTest {
    private val sink = RecordingSink()
    private val scape = Soundscape(sink)
    private val me = Vec(10.0, 10.0)
    private val w = ballWorld(Vec(30.0, 10.0), player = me) // a ball far off and still, whose ticks the tests ignore

    // The first frame of a round, with these events.
    private fun frame(vararg events: Event) {
        scape.reset(2)
        sink.plays.clear()
        sink.run(scape, w, 0.01, events = events.toList())
    }

    // The one sound this event makes.
    private fun played(e: Event, sound: Sound): RecordingSink.Played {
        frame(e)
        return sink.of(sound).single()
    }

    private fun gain(g: Double, x: Double, y: Double) = g * distanceGain(dist(me, Vec(x, y)))

    @Test
    fun takeAndTapPlayTheTapAtSeventyPercent() {
        close(gain(0.7, 14.0, 10.0), played(Event.Take(14.0, 10.0), Sound.TAP).gain)
        close(gain(0.7, 5.0, 3.0), played(Event.Tap(5.0, 3.0), Sound.TAP).gain)
    }

    @Test
    fun aKickPlaysTheKick() {
        close(gain(1.0, 12.0, 10.0), played(Event.Kick(12.0, 10.0), Sound.KICK).gain)
    }

    @Test
    fun aBumpIsAKnockOnTheBoardOrTheEndWallAtEightyPercent() {
        close(gain(0.8, 10.0, 0.0), played(Event.Bump(Side.LEFT, 10.0, 0.0), Sound.BOARD).gain)
        close(gain(0.8, 10.0, 20.0), played(Event.Bump(Side.RIGHT, 10.0, 20.0), Sound.BOARD).gain)
        close(gain(0.8, 40.0, 10.0), played(Event.Bump(Side.END, 40.0, 10.0), Sound.END_WALL).gain)
    }

    @Test
    fun aBallHittingABoardOrTheEndWallIsAsLoudAsItIsFast() {
        close(gain(0.5, 20.0, 0.0), played(Event.Board(Side.LEFT, 20.0, 0.0, speed = 4.0), Sound.BOARD).gain)
        close(gain(0.25, 20.0, 0.0), played(Event.Board(Side.LEFT, 20.0, 0.0, speed = 0.0), Sound.BOARD).gain)
        close(gain(1.0, 20.0, 20.0), played(Event.Board(Side.RIGHT, 20.0, 20.0, speed = 20.0), Sound.BOARD).gain)
        close(gain(0.75, 40.0, 5.0), played(Event.EndWall(40.0, 5.0, speed = 6.0, wide = null), Sound.END_WALL).gain)
    }

    @Test
    fun aPostASaveAndAGoalHaveTheirOwnSounds() {
        close(gain(1.0, 40.0, 8.5), played(Event.Post(40.0, 8.5, speed = 9.0), Sound.POST).gain)
        close(gain(1.0, 39.0, 10.0), played(Event.Save(39.0, 10.0, speed = 9.0), Sound.SAVE).gain)
    }

    @Test
    fun theGoalPlaysAtFullGainWhateverTheDistanceSoItsCheerAndWhistleDoNotDim() {
        val goal = played(Event.Goal(40.0, 10.0, speed = 9.0), Sound.GOAL)
        assertEquals(1.0, goal.gain)
        assertFalse(goal.behind)
        assertEquals(0.3, sink.crowds.last(), "the crowd swells to 0.3, and nothing in the same frame takes it back")
    }

    @Test
    fun aBallSoundBehindThePlayerPlaysDullAndOneInFrontOrASoundOfTheBoardsDoesNot() {
        val kick = played(Event.Kick(8.0, 10.0), Sound.KICK)
        assertTrue(kick.behind)
        close(gain(1.0, 8.0, 10.0), kick.gain) // only the rattle and the tick get a makeup
        assertFalse(played(Event.Kick(10.0, 10.0), Sound.KICK).behind)
        assertFalse(played(Event.Bump(Side.LEFT, 5.0, 0.0), Sound.BOARD).behind)
        assertFalse(played(Event.Post(5.0, 8.5, speed = 9.0), Sound.POST).behind)
        assertTrue(played(Event.Board(Side.LEFT, 5.0, 0.0, speed = 8.0), Sound.BOARD).behind)
        assertTrue(played(Event.Tap(5.0, 10.0), Sound.TAP).behind)
        assertTrue(played(Event.EndWall(0.0, 10.0, speed = 8.0, wide = null), Sound.END_WALL).behind)
    }

    @Test
    fun theKeepersVoyAndANoKickMakeNoSoundHere() {
        frame(Event.Voy(39.6, 10.0), Event.NoKick)
        assertEquals(emptyList<Sound>(), sink.plays.map { it.sound })
    }
}
