package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.KEEPER_X
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.whereText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// How a level runs: the end of a round, of a level, a pause, a stall, a restart (game.js).
class GameFlowTest {
    @Test
    fun aFinishedLevelSpeaksItsSummaryOnceAndReportsTheResult() {
        val h = GameHarness()
        h.game.startLevel(1)
        repeat(ROUNDS) {
            h.game.action(Action.GIVE_UP)
            h.nextRound()
        }
        val summary = "Level 1 done. Times: not found, not found, not found."
        assertEquals(Line(summary, Priority.HIGH, waitMs = 20000), h.said.last())
        assertEquals(1, h.said.count { it.text == summary })
        assertEquals(listOf(LevelResult(1, h.game.rounds, summary)), h.done)
        assertEquals(Phase.IDLE, h.game.phase)
        assertFalse(h.game.running)
        assertEquals(1, h.game.level)
        assertEquals(emptyList<Event>(), h.game.update(0.05, UP))
    }

    @Test
    fun aRunningGameIsInPlayOrBetweenRoundsNotPausedOrFinished() {
        val h = GameHarness()
        assertFalse(h.game.running)
        h.game.startLevel(1)
        assertTrue(h.game.running)
        h.game.action(Action.GIVE_UP)
        assertTrue(h.game.running, "between rounds")
        h.game.pause()
        assertFalse(h.game.running)
    }

    @Test
    fun pausingFreezesTheRoundAndResumingSaysWhereYouAre() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.place(Vec(10.0, 10.0), Vec(30.0, 15.0))
        h.tick(1.0, UP)
        val time = h.game.time
        val at = h.world.player
        h.game.pause()
        assertEquals(Phase.PAUSED, h.game.phase)
        val said = h.said.size
        assertEquals(emptyList<Event>(), h.game.update(0.05, UP))
        h.tick(2.0, UP)
        h.game.action(Action.WHERE)
        h.game.action(Action.KICK)
        h.game.action(Action.GIVE_UP)
        assertEquals(listOf(time, at, said), listOf(h.game.time, h.world.player, h.said.size))
        h.game.resume()
        assertEquals(Phase.PLAY, h.game.phase)
        assertEquals(Line(h.strings.whereRound(1, ROUNDS, whereText(h.world, h.strings)), Priority.HIGH), h.said.last())
        h.game.pause()
        h.game.pause() // pausing a paused game changes nothing
        h.game.resume()
        h.game.resume() // resuming a game that is not paused says nothing more
        assertEquals(said + 2, h.said.size)
    }

    @Test
    fun pausedBetweenRoundsResumingSaysTheNextRoundComesAndThenItDoes() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.game.action(Action.GIVE_UP)
        h.game.pause()
        h.tick(5.0)
        assertEquals(1, h.game.round, "the pause does not run down")
        h.game.resume()
        assertEquals(Line("Next round in a moment.", Priority.HIGH), h.said.last())
        assertEquals(Phase.BETWEEN, h.game.phase)
        h.nextRound()
        assertEquals(listOf(Phase.PLAY, 2), listOf(h.game.phase, h.game.round))
    }

    @Test
    fun aFrameLongerThan50msIsCappedSoAStallDoesNotSkipThePhysics() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.place(Vec(10.0, 10.0), Vec(30.0, 15.0))
        h.game.update(1.0, UP)
        close(h.world.player.x, 10.2, 1e-6)
        close(h.game.time, 0.05)
        h.game.update(-1.0, UP) // a clock that ran backwards changes nothing
        close(h.game.time, 0.05)
    }

    @Test
    fun startingALevelWhileOneIsRunningStartsItOver() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.game.action(Action.GIVE_UP)
        h.game.startLevel(3)
        assertEquals(listOf(3, 1, Phase.PLAY), listOf(h.game.level, h.game.round, h.game.phase))
        assertEquals(emptyList<RoundStats>(), h.game.rounds)
        assertEquals(Vec(KEEPER_X, GOAL.y), h.world.keeper?.pos)
        assertEquals(Line("Round 1. Beat the keeper.", Priority.HIGH), h.said.last())
    }
}
