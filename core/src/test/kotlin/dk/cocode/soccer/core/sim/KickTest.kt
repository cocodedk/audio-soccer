package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot
import kotlin.math.tan

// From prototype/test/kick.test.mjs.
class KickTest {
    private class Kicked(val w: World, val ev: List<Event>)

    private fun kickFrom(x: Double, y: Double, vararg keys: String): Kicked {
        val w = createWorld(Vec(x - 0.5, y), Vec(x, y), hasBall = true)
        return Kicked(w, step(w, DT, arrows(*keys), true))
    }

    // Where the kicked ball's line crosses x = 40.
    private fun crossing(k: Kicked): Double = k.w.ball.y + (k.w.ballVel.y / k.w.ballVel.x) * (PITCH_L - k.w.ball.x)

    private fun slope(vararg keys: String): Double = kickFrom(10.0, 10.0, *keys).w.ballVel.let { it.y / it.x }

    @Test
    fun spaceKicksStraightUpThePitchEverywhereWithNoAimAtTheGoalFromTheAttackingThird() {
        for (x in listOf(5.0, 15.0, 25.0, 30.0, 35.0)) {
            val k = kickFrom(x, 4.0)
            assertTrue(k.ev.has<Event.Kick>())
            close(k.w.ballVel.y, 0.0)
            close(k.w.ballVel.x, KICK_SPEED, 0.1) // one step of friction has already acted
        }
    }

    @Test
    fun leftOrRightHeldAnglesTheKick15DegreesTowardThatSideUpAndDownAreIgnored() {
        close(slope("Right"), tan(KICK_ANGLE), 1e-9)
        close(slope("Left"), -tan(KICK_ANGLE), 1e-9)
        close(slope("Up", "Right"), slope("Right"), 1e-9)
        close(slope("Down", "Left"), slope("Left"), 1e-9)
        close(slope("Up"), 0.0)
        close(slope("Down"), 0.0)
        close(slope("Left", "Right"), 0.0)
        val v = kickFrom(10.0, 10.0, "Right").w.ballVel
        close(hypot(v.x, v.y), KICK_SPEED, 0.1)
    }

    @Test
    fun aKickNeverGoesBackwardWhateverIsHeldAndWhereverTheBallIs() {
        val sets = listOf(
            listOf(), listOf("Up"), listOf("Down"), listOf("Left"), listOf("Right"),
            listOf("Down", "Right"), listOf("Down", "Left"),
        )
        for (keys in sets) {
            for ((x, y) in listOf(2.0 to 10.0, 20.0 to 1.0, 30.0 to 19.0, 38.0 to 8.0, 39.5 to 10.0)) {
                val vx = kickFrom(x, y, *keys.toTypedArray()).w.ballVel.x
                assertTrue(vx > 0, "$keys from $x,$y: vx $vx")
            }
        }
    }

    @Test
    fun aimHelpALineThatAlreadyCrossesWithinTheMouthPlusMinus1mIsCorrectedToJustInsideThePost() {
        fun at(y: Double, x: Double = 35.0) = crossing(kickFrom(x, y))
        close(at(10.0), 10.0, 1e-6) // already in the goal: untouched
        close(at(9.5), 9.5, 1e-6)
        close(at(8.0), 8.9, 1e-6) // wide of the post by 0.5 m: pulled in to 0.4 m inside it
        close(at(8.6), 8.9, 1e-6) // inside the post by 0.1 m
        close(at(7.6), 8.9, 1e-6)
        close(at(12.4), 11.1, 1e-6)
        close(at(11.3), 11.1, 1e-6)
        close(at(11.0), 11.0, 1e-6)
    }

    @Test
    fun aimHelpLeavesALineAloneThatIsMoreThan1mOutsideTheMouth() {
        for (y in listOf(7.4, 5.0, 1.0, 12.6, 15.0, 19.0)) {
            val k = kickFrom(35.0, y)
            close(k.w.ballVel.y, 0.0)
            close(crossing(k), y, 1e-6)
        }
    }

    @Test
    fun aimHelpActsOnAnAngledLineTooAndAnAngledLineFarOutIsNotCorrected() {
        val near = kickFrom(32.0, 8.0, "Right") // 8 + 8 tan 15 = 10.14: in the goal already
        close(crossing(near), 8 + 8 * tan(KICK_ANGLE), 1e-6)
        val edge = kickFrom(32.0, 10.0, "Right") // 10 + 2.14 = 12.14: pulled in to 11.1
        close(crossing(edge), 11.1, 1e-6)
        val far = kickFrom(20.0, 10.0, "Right").w.ballVel // 10 + 5.4 = 15.4: no help, wide
        close(far.y / far.x, tan(KICK_ANGLE), 1e-9)
    }

    @Test
    fun kickDirIsAUnitVectorPointingUpThePitch() {
        for ((y, side) in listOf(10.0 to 0.0, 4.0 to 1.0, 8.0 to -1.0, 12.4 to 1.0)) {
            val d = kickDir(Vec(33.0, y), Vec(0.0, side))
            close(hypot(d.x, d.y), 1.0, 1e-9)
            assertTrue(d.x > 0)
        }
    }

    @Test
    fun aStraightShotFromOutsideTheMouthHitsTheEndWallAndIsCalledWide() {
        val w = createWorld(Vec(29.5, 4.0), Vec(30.0, 4.0), hasBall = true)
        step(w, DT, Vec.ZERO, true)
        val ev = run(w, 2.0)
        assertFalse(ev.has<Event.Goal>())
        assertEquals(Side.LEFT, ev.ofType<Event.EndWall>().first().wide)
    }

    @Test
    fun aStraightShotFromCentre14mOutIsAGoalYouCannotKickABallYouDoNotHave() {
        val w = createWorld(Vec(25.5, 10.0), Vec(26.0, 10.0), hasBall = true)
        step(w, DT, Vec.ZERO, true)
        assertTrue(run(w, 5.0).has<Event.Goal>())
        val none = createWorld(Vec(10.0, 10.0), Vec(30.0, 10.0))
        assertEquals(listOf<Event>(Event.NoKick), step(none, DT, Vec.ZERO, true))
    }
}
