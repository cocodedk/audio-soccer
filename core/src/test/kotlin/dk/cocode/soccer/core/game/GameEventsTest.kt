package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.tenths
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// What the game says for each thing that happens, with the priority, cut, key, gap and wait of the say() call of
// game.js that spoke it. Every line is a Line the shell's SpeechQueue takes. (GameTest has the lines of a save, a wide
// shot, taking the ball back, W, giving up and the time cap.)
class GameEventsTest {
    @Test
    fun eachRoundStartsWithItsIntroAsAHighLineAndNothingElse() {
        val h = GameHarness()
        h.game.startLevel(2)
        assertEquals(listOf(Line("Round 1. Score a goal.", Priority.HIGH)), h.said)
        h.game.action(Action.GIVE_UP)
        h.nextRound()
        assertEquals(Line("Round 2. Score a goal.", Priority.HIGH), h.said.last())
        val fa = GameHarness(lang = Lang.FA)
        fa.game.startLevel(1)
        assertEquals(listOf("دور ۱. توپ را پیدا کنید."), fa.texts())
    }

    @Test
    fun findingTheBallInLevel1EndsTheRoundAndSaysGotItWithTheTime() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.place(Vec(10.0, 10.0), Vec(10.5, 10.0))
        val events = h.game.update(0.05, UP)
        assertTrue(events.any { it is Event.Take })
        assertEquals(Line(h.strings.gotIt(tenths(h.game.time)), Priority.HIGH, cut = true), h.said.last())
        assertEquals(Phase.BETWEEN, h.game.phase)
        assertTrue(h.game.rounds[0].success)
    }

    @Test
    fun aGoalEndsTheRoundAndSaysGoal() {
        val h = GameHarness(2)
        h.game.startLevel(2)
        h.place(Vec(35.0, 10.0), Vec(35.5, 10.0))
        h.game.action(Action.KICK)
        val events = mutableListOf<Event>()
        repeat(60) { events += h.game.update(0.05, Vec.ZERO) }
        assertTrue(events.any { it is Event.Goal }, "the frame's events come back")
        assertEquals(Line("Goal!", Priority.HIGH, cut = true), h.said.last())
        assertEquals(listOf(true, 1), listOf(h.game.rounds[0].success, h.game.rounds[0].kicks))
    }

    @Test
    fun aPostSaysPostAtNormalPriority() {
        val h = GameHarness(2)
        h.game.startLevel(2)
        h.place(Vec(10.0, 10.0), Vec(39.9, 8.5), Vec(5.0, 0.0))
        h.tick(0.2)
        assertTrue(Line("Post!", key = "post", gapMs = 800) in h.said)
    }

    @Test
    fun aWideShotToTheRightSaysWideRightAndADribbledBallAgainstTheEndWallSaysWideOfTheGoal() {
        val shot = GameHarness(2)
        shot.game.startLevel(2)
        shot.place(Vec(29.5, 16.0), Vec(30.0, 16.0))
        shot.kick()
        shot.tick(2.0)
        assertTrue(Line("Wide right", key = "wide", gapMs = 1000) in shot.said)
        assertEquals(1, shot.game.stats.wide)
        for ((y, side) in listOf(3.0 to "left", 17.0 to "right")) {
            val h = GameHarness(2)
            h.game.startLevel(2)
            h.place(Vec(10.0, 10.0), Vec(39.9, y), Vec(5.0, 0.0))
            h.tick(0.2)
            assertTrue(Line("Wide $side of the goal", key = "wide", gapMs = 2500) in h.said, side)
            assertEquals(0, h.game.stats.wide, "a dribbled ball is called, not counted")
        }
    }

    @Test
    fun endWallsAreOnlyCalledInLevels2And3AndOnlyAtTheGoalEnd() {
        val one = GameHarness()
        one.game.startLevel(1)
        one.place(Vec(10.0, 10.0), Vec(39.9, 3.0), Vec(5.0, 0.0))
        one.tick(0.2)
        assertEquals(1, one.said.size, "level 1 says only its intro")
        val two = GameHarness(2)
        two.game.startLevel(2)
        two.place(Vec(30.0, 10.0), Vec(0.1, 10.0), Vec(-5.0, 0.0))
        two.tick(0.2)
        assertEquals(1, two.said.size, "the other end wall is silent")
    }

    @Test
    fun theKeepersVoyIsSaidInAHighPitch() {
        val h = GameHarness(3)
        h.game.startLevel(3)
        h.place(Vec(35.5, 10.0), Vec(36.0, 10.0))
        h.game.update(0.05, Vec.ZERO)
        assertEquals(Line("Voy!", pitch = 1.4f), h.said.last())
    }

    @Test
    fun kickingWithoutTheBallSaysYouNeedItAtLowPriority() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.kick()
        assertEquals(Line("You need the ball to kick", Priority.LOW, key = "nokick", gapMs = 2000), h.said.last())
    }

    @Test
    fun aBumpIsCountedOncePerBoardAndComesBackWithTheFrame() {
        val h = GameHarness()
        h.game.startLevel(1)
        h.place(Vec(10.0, 0.4), Vec(30.0, 15.0))
        val events = h.game.update(0.05, LEFT)
        assertEquals(listOf(Side.LEFT), events.filterIsInstance<Event.Bump>().map { it.side })
        h.tick(1.0, LEFT)
        assertEquals(1, h.game.stats.bumps)
    }
}
