package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/world.test.mjs. The arrow-key map, the audio mapping (headphones) and the
// clock and where-am-I texts are not ported here: the first two are out of scope for the app and
// the texts belong to Describe.kt.
class WorldTest {
    @Test
    fun arrowLeftDecreasesYAndArrowUpIncreasesXAt4MetresPerSecond() {
        val w = createWorld(Vec(20.0, 10.0), Vec(5.0, 5.0))
        run(w, 0.5, arrows("Left"))
        close(w.player.y, 8.0, 0.05)
        close(w.player.x, 20.0)
        run(w, 0.5, arrows("Up"))
        close(w.player.x, 22.0, 0.05)
    }

    @Test
    fun sideBoardsClampThePlayerAndBumpOncePerContact() {
        val w = createWorld(Vec(20.0, 1.0), Vec(5.0, 15.0))
        val ev = run(w, 1.0, arrows("Left"))
        close(w.player.y, PLAYER_RADIUS)
        assertEquals(listOf(Side.LEFT to 0.0), ev.ofType<Event.Bump>().map { it.side to it.y })
        val ev2 = run(w, 6.0, arrows("Right"))
        close(w.player.y, 20 - PLAYER_RADIUS)
        assertEquals(listOf(Side.RIGHT), ev2.ofType<Event.Bump>().map { it.side })
        val ev3 = run(w, 6.0, arrows("Up"))
        close(w.player.x, 40 - PLAYER_RADIUS)
        assertTrue(ev3.any { it is Event.Bump && it.side == Side.END && it.x == 40.0 })
    }

    @Test
    fun theBallKnocksOffASideBoardAndStaysOnThePitch() {
        val w = createWorld(Vec(2.0, 18.0), Vec(20.0, 2.0))
        w.ballVel = Vec(0.0, -8.0)
        val ev = run(w, 1.0)
        assertTrue(ev.ofType<Event.Board>().any { it.side == Side.LEFT })
        assertTrue(w.ball.y >= 0 && w.ballVel.y >= 0)
    }

    @Test
    fun theBallKnocksOffTheRightBoardAndTheEndWallBehindThePlayer() {
        val w = createWorld(Vec(2.0, 2.0), Vec(20.0, 18.0))
        w.ballVel = Vec(0.0, 8.0)
        val right = run(w, 1.0)
        assertTrue(right.ofType<Event.Board>().any { it.side == Side.RIGHT && it.y == PITCH_W })
        assertTrue(w.ball.y <= PITCH_W && w.ballVel.y <= 0)
        val back = createWorld(Vec(30.0, 18.0), Vec(0.5, 10.0))
        back.ballVel = Vec(-8.0, 0.0)
        val ev = run(back, 1.0)
        assertTrue(ev.ofType<Event.EndWall>().any { it.x == 0.0 && it.wide == null })
        assertTrue(back.ball.x >= 0 && back.ballVel.x >= 0)
    }

    @Test
    fun lineOutcomeIsGoalBetweenThePostsPostWithin25cmWallOutside() {
        assertEquals(LineOutcome.GOAL, lineOutcome(10.0))
        assertEquals(LineOutcome.GOAL, lineOutcome(8.8))
        assertEquals(LineOutcome.POST, lineOutcome(8.5))
        assertEquals(LineOutcome.POST, lineOutcome(8.3))
        assertEquals(LineOutcome.POST, lineOutcome(11.7))
        assertEquals(LineOutcome.WALL, lineOutcome(8.2))
        assertEquals(LineOutcome.WALL, lineOutcome(12.0))
    }

    private fun shoot(y: Double, keeper: Boolean = false): Pair<World, List<Event>> {
        val w = createWorld(Vec(35.0, y), Vec(35.5, y), hasBall = true, keeper = keeper)
        w.ballVel = Vec(KICK_SPEED, 0.0) // straight along x
        w.shot = true
        w.hasBall = false
        return w to run(w, 2.0)
    }

    @Test
    fun aShotBetweenThePostsIsAGoal() {
        val (w, ev) = shoot(10.0)
        assertTrue(ev.has<Event.Goal>())
        assertTrue(w.scored)
    }

    @Test
    fun aShotAtAPostClangsAndBouncesBack() {
        val (w, ev) = shoot(8.45)
        assertTrue(ev.has<Event.Post>())
        assertFalse(ev.has<Event.Goal>())
        assertTrue(w.ball.x < 40)
        assertEquals(POST_LEFT, ev.ofType<Event.Post>().first().y)
        assertEquals(POST_RIGHT, shoot(11.55).second.ofType<Event.Post>().first().y)
    }

    @Test
    fun aShotOutsideThePostsKnocksOffTheEndWallAndIsCalledWide() {
        assertEquals(Side.LEFT, shoot(5.0).second.ofType<Event.EndWall>().first().wide)
        assertEquals(Side.RIGHT, shoot(16.0).second.ofType<Event.EndWall>().first().wide)
    }

    @Test
    fun theKeeperSavesAShotWithinReachAndShoutsVoy() {
        val (w, ev) = shoot(10.0, keeper = true)
        assertTrue(ev.has<Event.Voy>())
        assertTrue(ev.has<Event.Save>())
        assertFalse(w.scored)
        assertTrue(w.ballVel.x <= 0)
    }

    @Test
    fun noEventsAreEmittedOnceTheGoalIsScored() {
        val (w, _) = shoot(10.0)
        val t = w.t
        assertEquals(emptyList<Event>(), step(w, DT, arrows("Up"), true))
        assertEquals(t, w.t)
    }
}
