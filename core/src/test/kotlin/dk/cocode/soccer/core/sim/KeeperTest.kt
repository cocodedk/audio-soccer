package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot

// From prototype/test/keeper.test.mjs.
class KeeperTest {
    private fun between(rand: () -> Double, a: Double, b: Double) = a + rand() * (b - a)

    // levels.js setupRound for levels 2 and 3 (Levels.kt arrives with the game): the player starts with
    // the ball in their own half, the keeper in goal.
    private fun setupRound3(rand: () -> Double): World {
        val x = between(rand, 4.0, 16.0)
        val y = between(rand, 4.0, 16.0)
        return createWorld(Vec(x, y), Vec(x + 0.5, y), hasBall = true, keeper = true)
    }

    // One level 3 round played by a scripted player: run straight to (sx, sy) with the ball, optionally
    // settle for a second, kick holding `keys`, then watch 3 s. Returns goal, save, post or wide.
    private fun play(rand: () -> Double, sx: Double, sy: Double, keys: List<String>, settle: Boolean): String {
        val w = setupRound3(rand)
        for (i in 0 until 4000) {
            val dx = sx - w.player.x
            val dy = sy - w.player.y
            val d = hypot(dx, dy)
            if (d < 0.08) break
            step(w, DT, Vec(dx / d, dy / d))
        }
        if (settle) run(w, 1.0)
        step(w, DT, arrows(*keys.toTypedArray()), true)
        val ev = run(w, 3.0)
        return when {
            ev.has<Event.Goal>() -> "goal"
            ev.has<Event.Save>() -> "save"
            ev.has<Event.Post>() -> "post"
            else -> "wide"
        }
    }

    // The outcomes of n rounds with a random start and a shooting spot picked by spot(rand).
    private fun outcomes(
        n: Int, seed: Int, settle: Boolean, keys: (Double) -> List<String>, spot: (() -> Double) -> Pair<Double, Double>,
    ): Set<String> {
        val rand = seeded(seed)
        val seen = mutableSetOf<String>()
        repeat(n) {
            val (x, y) = spot(rand)
            seen += play(rand, x, y, keys(y), settle)
        }
        return seen
    }

    @Test
    fun keeperTuningSlowShortReachNoRandomError() {
        assertTrue(KEEPER_SPEED in 1.2..1.4)
        assertTrue(KEEPER_REACH in 0.6..0.7)
        val k = setupRound3(seeded(1)).keeper
        assertEquals(Vec(KEEPER_X, GOAL.y), k?.pos)
    }

    @Test
    fun headOnAStraightCentredShotFrom8To12mIsAlwaysSaved() {
        for (settle in listOf(false, true)) {
            val got = outcomes(200, 11, settle, { emptyList() }) { r -> between(r, 28.0, 32.0) to 10.0 }
            assertEquals(setOf("save"), got, "settled $settle")
        }
    }

    @Test
    fun standSlightlyOffToOneSideAndHoldThatArrowAlwaysAGoalFrom10m() {
        // y 7.5 to 8.6, 10 m out, Right: the 15 degree line crosses at 10.2 to 11.3 and aim help pulls it inside the post
        for (settle in listOf(false, true)) {
            assertEquals(setOf("goal"), outcomes(200, 12, settle, { listOf("Right") }) { r -> 30.0 to between(r, 7.5, 8.6) })
            assertEquals(setOf("goal"), outcomes(200, 13, settle, { listOf("Left") }) { r -> 30.0 to between(r, 11.4, 12.5) })
        }
    }

    // A player settled at (x, y) with the ball on y, the keeper already on it; kick holding `keys`.
    private fun settled(x: Double, y: Double, vararg keys: String): String {
        val w = createWorld(Vec(x - 0.5, y), Vec(x, y), hasBall = true, keeper = true)
        run(w, 3.0)
        step(w, DT, arrows(*keys), true)
        val ev = run(w, 3.0)
        return when {
            ev.has<Event.Goal>() -> "goal"
            ev.has<Event.Save>() -> "save"
            else -> "wide"
        }
    }

    @Test
    fun fromTheCentreLeftScoresFrom8To9mButGoesWideFrom95mOutTo12m() {
        for (x in listOf(32.0, 31.5, 31.0)) assertEquals("goal", settled(x, 10.0, "Left"), "x $x")
        for (x in listOf(30.5, 30.0, 29.0, 28.0)) assertEquals("wide", settled(x, 10.0, "Left"), "x $x")
        for (x in listOf(32.0, 31.0)) assertEquals("goal", settled(x, 10.0, "Right"), "x $x")
        for (x in listOf(30.0, 28.0)) assertEquals("wide", settled(x, 10.0, "Right"), "x $x")
    }

    @Test
    fun theKeeperFollowsTheBallAtKeeperSpeedAndStaysInsideTheBand() {
        val w = createWorld(Vec(30.0, 2.0), Vec(30.4, 2.0), keeper = true)
        val k = w.keeper!!
        assertEquals(KEEPER_X, k.pos.x)
        run(w, 1.0)
        assertTrue(abs(k.pos.y - (10 - KEEPER_SPEED)) < 0.05, "after 1 s ${k.pos.y}")
        run(w, 4.0)
        assertEquals(KEEPER_MIN_Y, k.pos.y)
        w.ball = Vec(w.ball.x, 10.0)
        w.ballVel = Vec.ZERO
        run(w, 4.0)
        assertTrue(abs(k.pos.y - 10) < 0.01, "settles on the ball")
        w.ball = Vec(w.ball.x, 19.0)
        w.ballVel = Vec.ZERO
        run(w, 10.0)
        assertEquals(KEEPER_MAX_Y, k.pos.y)
    }

    @Test
    fun theKeeperStopsTheMomentAShotIsKickedShoutsVoyAndFollowsAgainAfterTheSave() {
        val w = createWorld(Vec(29.5, 10.0), Vec(30.0, 10.0), hasBall = true, keeper = true)
        val k = w.keeper!!
        k.pos = Vec(k.pos.x, 9.0) // not yet on the ball: he is still walking to it
        step(w, DT, Vec.ZERO, true)
        val y0 = k.pos.y
        assertFalse(k.moving)
        val ev = run(w, 0.4)
        assertEquals(y0, k.pos.y, "he does not move while the shot is on its way")
        assertTrue(ev.has<Event.Voy>())
        val rest = run(w, 2.0)
        assertTrue(rest.has<Event.Goal>(), "a straight shot 1 m from where he stopped is a goal")
        // a save clears the shot flag, and he tracks the ball again
        val s = createWorld(Vec(29.5, 10.0), Vec(30.0, 10.0), hasBall = true, keeper = true)
        step(s, DT, Vec.ZERO, true)
        assertTrue(run(s, 2.0).has<Event.Save>())
        assertFalse(s.shot)
        s.ballVel = Vec.ZERO
        s.ball = Vec(s.ball.x, 8.0)
        run(s, 1.0)
        assertTrue(s.keeper!!.pos.y < 10 - 1, "keeper follows again: ${s.keeper!!.pos.y}")
    }

    @Test
    fun voyIsShoutedOncePerApproachAndRearmsWhenTheBallLeaves() {
        val w = createWorld(Vec(30.0, 10.0), Vec(35.0, 10.0), keeper = true)
        val k = w.keeper!!
        assertEquals(1, run(w, 1.0).ofType<Event.Voy>().size)
        assertFalse(k.voyArmed)
        w.ball = Vec(20.0, 10.0)
        w.ballVel = Vec.ZERO
        run(w, 0.1)
        assertTrue(k.voyArmed)
    }

    @Test
    fun aSavePushesTheBallWellOutSoDribblingIntoTheKeeperDoesNotSaveAgainAndAgain() {
        val w = createWorld(Vec(36.0, 10.0), Vec(36.5, 10.0), hasBall = true, keeper = true)
        val ev = run(w, 4.0, arrows("Up"))
        val saves = ev.ofType<Event.Save>().size
        assertTrue(saves in 1..3, "saves $saves")
    }
}
