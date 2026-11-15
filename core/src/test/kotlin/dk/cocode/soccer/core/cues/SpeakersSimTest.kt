package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.DT
import dk.cocode.soccer.core.sim.GoalSide
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.arrows
import dk.cocode.soccer.core.sim.createWorld
import dk.cocode.soccer.core.sim.goalSide
import dk.cocode.soccer.core.sim.step
import dk.cocode.soccer.core.text.SHOT_RANGE
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/speakers-sim.test.mjs: a simulated player who obeys the speakers-mode side calls, stepping the real
// world. Following the words alone must always reach a still ball (no spot where the word says ahead but running ahead
// misses it).
class SpeakersSimTest {
    private val dirOf = mapOf(
        SideWord.AHEAD to arrows("Up"), SideWord.BEHIND to arrows("Down"),
        SideWord.LEFT to arrows("Left"), SideWord.RIGHT to arrows("Right"),
    )
    private val start = Vec(10.0, 10.0)

    private class Chase(val secs: Double, val words: Int)

    // As in the game (Cues): the words go through SideCaller, one per SIDE_GAP and never the same twice, and each is heard at
    // once. The player stands until the first word, then keeps running the way the last word he heard says. Null if the
    // ball is not taken in time.
    private fun chase(start: Vec, ball: Vec, secs: Double = 30.0): Chase? {
        val w = createWorld(start, ball)
        val caller = SideCaller()
        var dir = Vec.ZERO
        var words = 0
        var t = 0.0
        while (t < secs) {
            val ms = (t * 1000).toLong()
            val word = caller.next(sideWord(w.player, w.ball, false, caller.heard), ms)
            if (word != null) {
                caller.spoke(word, ms)
                dir = dirOf.getValue(word)
                words++
            }
            step(w, DT, dir)
            if (w.hasBall) return Chase(t, words)
            t += DT
        }
        return null
    }

    // The ideal player who hears every change the moment it happens: an extra check on sideWord alone.
    private fun chaseInstant(start: Vec, ball: Vec, secs: Double = 30.0): Double? {
        val w = createWorld(start, ball)
        var heard: SideWord? = null
        var t = 0.0
        while (t < secs) {
            val word = sideWord(w.player, w.ball, false, heard)
            heard = word
            step(w, DT, dirOf.getValue(word))
            if (w.hasBall) return t
            t += DT
        }
        return null
    }

    private val offsets: List<Pair<Double, Double>> = buildList {
        var dx = -8.0
        while (dx <= 20) {
            var dy = -8.0
            while (dy <= 8) {
                add(dx to dy)
                dy += 0.3
            }
            dx += 0.7
        }
    }

    @Test
    fun obeyingTheSideCallsAsTheCallerLetsThemThroughTakesTheBallWithoutFlapping() {
        val dead = chase(start, Vec(20.0, 11.2))
        assertNotNull(dead, "the reported dead zone")
        assertTrue(dead!!.words <= 3, "ahead, right, ahead: ${dead.words} words")
        assertNotNull(chase(start, Vec(20.0, 8.8)))
        assertNotNull(chase(start, Vec(9.2, 10.0)), "just under 1 m behind")
        for ((dx, dy) in offsets) {
            val r = chase(start, Vec(start.x + dx, start.y + dy))
            assertNotNull(r, "ball at $dx, $dy")
            assertTrue(r!!.words <= 3, "ball at $dx, $dy: ${r.words} words")
        }
    }

    @Test
    fun withEveryWordHeardAtOnceObeyingThemTakesTheBallToo() {
        assertNotNull(chaseInstant(start, Vec(20.0, 11.2)), "the reported dead zone")
        for ((dx, dy) in offsets) {
            assertNotNull(chaseInstant(start, Vec(start.x + dx, start.y + dy)), "ball at $dx, $dy")
        }
    }

    // With the ball, from where level 2 and 3 rounds start (player x 4 to 16, any y, all beyond SHOT_RANGE): obeying the calls
    // for the goal must bring the ball into range lined up with the goal. Ahead has to come out of range too, or a sideways
    // run never stops (right, left, right... for the whole round).
    private fun steer(start: Vec, secs: Double = 20.0): Double? {
        val w = createWorld(start, Vec(start.x + 0.5, start.y), hasBall = true)
        val caller = SideCaller()
        var dir = Vec.ZERO
        var t = 0.0
        while (t < secs) {
            val ms = (t * 1000).toLong()
            val word = caller.next(sideWord(w.player, w.ball, true, caller.heard), ms)
            if (word != null) {
                caller.spoke(word, ms)
                dir = dirOf.getValue(word)
            }
            step(w, DT, dir)
            if (!w.hasBall) return null
            if (PITCH_L - w.ball.x <= SHOT_RANGE && goalSide(w.ball.y) == GoalSide.AHEAD) return t
            t += DT
        }
        return null
    }

    @Test
    fun withTheBallObeyingTheCallsFromALevel2Or3StartBringsItIntoRangeLinedUp() {
        for (x in 4..16 step 2) for (y in 2..18) {
            assertNotNull(steer(Vec(x.toDouble(), y.toDouble())), "start $x, $y")
        }
    }
}
