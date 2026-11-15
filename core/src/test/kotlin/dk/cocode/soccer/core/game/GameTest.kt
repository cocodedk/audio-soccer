package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Aim
import dk.cocode.soccer.core.sim.Outcome
import dk.cocode.soccer.core.sim.ROUND_CAP
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.sim.dist
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.whereText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/game.test.mjs. The trial's partial() and snapshot() belong to its results table, which the
// app does not have: the rounds and the round in play are read from Game's internal stats and rounds instead.
class GameTest {
    @Test
    fun theRoundClockStartsAtTheFirstArrowPressNotAtTheRoundStart() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.tick(5.0)
        assertEquals(0.0, h.game.time)
        h.tick(1.0, UP)
        close(h.game.time, 1.0, 0.06)
        h.tick(1.0)
        close(h.game.time, 2.0, 0.11)
    }

    @Test
    fun gGivesUpTheRoundNotFoundElapsedTimeKeptAndTheNextRoundStartsAfterThePause() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.tick(2.0, LEFT)
        h.game.action(Action.GIVE_UP)
        assertEquals(Phase.BETWEEN, h.game.phase)
        assertEquals(Line("Round given up.", Priority.HIGH, cut = true), h.said.last())
        val r = h.game.rounds[0]
        assertEquals(listOf(Ending.GIVE_UP, null), listOf(r.ending, r.efficiency))
        close(r.time, 2.0, 0.06)
        h.game.action(Action.GIVE_UP) // ignored between rounds
        assertEquals(1, h.game.rounds.size)
        h.nextRound()
        assertEquals(listOf(Phase.PLAY, 2), listOf(h.game.phase, h.game.round))
        h.game.action(Action.GIVE_UP)
        h.nextRound()
        h.game.action(Action.GIVE_UP)
        h.nextRound()
        assertEquals(Phase.IDLE, h.game.phase)
        assertEquals(1, h.done.size)
        assertEquals(List(ROUNDS) { Ending.GIVE_UP }, h.done[0].rounds.map { it.ending })
    }

    @Test
    fun aRoundAutoEndsAt120sAsNotFoundTheSameWayAsGivingUp() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.place(Vec(10.0, 10.0), Vec(30.0, 15.0)) // a ball the player never reaches
        h.tick(ROUND_CAP - 1, DOWN)
        assertEquals(Phase.PLAY, h.game.phase)
        h.tick(2.0, DOWN)
        assertEquals(Phase.BETWEEN, h.game.phase)
        assertEquals(Line("Time is up.", Priority.HIGH, cut = true), h.said.last())
        assertEquals(Ending.TIMEOUT, h.game.rounds[0].ending)
        close(h.game.rounds[0].time, ROUND_CAP, 0.06)
    }

    @Test
    fun aFoundBallRecordsStartDistanceStartSideTimeEfficiencyAndTheCounters() {
        val h = GameHarness(5)
        h.game.startLevel(1)
        val player = h.world.player
        val ball = h.world.ball
        h.walkToBall()
        val r = h.game.rounds[0]
        assertEquals(Ending.SUCCESS, r.ending)
        assertTrue(r.startDist >= 10)
        close(r.startDist, dist(ball, player))
        assertEquals(ball.x < player.x, r.startBehind)
        val efficiency = checkNotNull(r.efficiency)
        assertTrue(efficiency > 0.6 && efficiency <= 1.1, "efficiency $efficiency")
        close(efficiency, r.startDist / (4 * r.time))
    }

    @Test
    fun level2AWideShotIsCountedCalledAndRecorded() {
        val h = GameHarness(2)
        h.game.startLevel(2)
        h.place(Vec(29.5, 4.0), Vec(30.0, 4.0))
        h.game.update(0.05, UP) // the first press starts the clock
        h.kick()
        h.tick(2.0)
        val w = h.game.stats
        assertEquals(listOf(1, 1, 0), listOf(w.kicks, w.wide, w.saves))
        assertEquals(listOf(Triple(-6.0, Aim.STRAIGHT, Outcome.WIDE_LEFT)), w.shots.map { Triple(it.offset, it.aim, it.outcome) })
        assertTrue(w.shots[0].dist > 9 && w.shots[0].dist < 10.5, "metres to the goal line")
        assertTrue(Line("Wide left", key = "wide", gapMs = 1000) in h.said)
    }

    @Test
    fun level3ASaveIsCountedAndSaidAndEfficiencyIsOnlyForLevel1() {
        val h = GameHarness(3)
        h.game.startLevel(3)
        h.place(Vec(29.5, 10.0), Vec(30.0, 10.0))
        h.kick()
        h.tick(2.0)
        val s = h.game.stats
        assertEquals(listOf(1, 1), listOf(s.kicks, s.saves))
        assertEquals(Outcome.SAVED, s.shots[0].outcome)
        assertTrue(Line("Saved!", Priority.HIGH, cut = true, key = "saved", gapMs = 1500) in h.said)
        h.game.action(Action.GIVE_UP)
        assertNull(h.game.rounds[0].efficiency)
    }

    @Test
    fun wCountsAsAskingWhereAndANewLevelStartsEmpty() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.game.action(Action.WHERE)
        assertEquals(
            Line(h.strings.whereRound(1, ROUNDS, whereText(h.world, h.strings)), Priority.HIGH, key = "where"),
            h.said.last(),
        )
        h.game.action(Action.WHERE)
        assertEquals(2, h.game.stats.where)
        h.game.action(Action.GIVE_UP)
        assertEquals(2, h.game.rounds[0].where)
        h.game.startLevel(2)
        assertEquals(emptyList<RoundStats>(), h.game.rounds, "a new level starts empty")
        assertEquals(1, h.game.round)
    }

    @Test
    fun level2AKickThatStopsShortThenIsTakenAndDribbledInIsRecordedShortNotGoal() {
        val h = GameHarness(2)
        h.game.startLevel(2)
        h.place(Vec(2.5, 10.0), Vec(3.0, 10.0))
        h.game.update(0.05, UP)
        h.kick()
        val shots = h.game.stats.shots
        var i = 0
        while (i < 4000 && !h.world.scored) {
            h.game.update(0.05, UP)
            i++
        }
        assertTrue(h.world.scored, "the player dribbled the ball in")
        assertEquals(listOf(Outcome.SHORT), shots.map { it.outcome })
        assertTrue(Line("Got the ball", key = "got", gapMs = 1500) in h.said, "taking it back is said")
    }
}
