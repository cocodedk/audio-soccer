package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.close
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.SHOT_RANGE
import dk.cocode.soccer.core.text.gameStrings
import dk.cocode.soccer.core.text.guideText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/speakers.test.mjs. Not ported: the sound mode (the app has speakers only).
class SpeakersTest {
    @Test
    fun distanceGainIsOneWithinTwoMetresThenFallsGentlyAndNeverBelowAThird() {
        for (d in listOf(0.0, 1.0, 2.0)) close(distanceGain(d), 1.0)
        close(distanceGain(12.0), 0.4)
        close(distanceGain(20.0), 0.3)
        close(distanceGain(60.0), 0.3)
        close(distanceGain(500.0), 0.3)
        var prev = 2.0
        var d = 0.0
        while (d <= 70) {
            assertTrue(distanceGain(d) <= prev)
            prev = distanceGain(d)
            d += 0.5
        }
    }

    @Test
    fun beaconIntervalIsTwoSecondsFrom30mHalfASecondWithin6mLinearBetween() {
        for (d in listOf(30.0, 45.0, 60.0)) close(beaconEvery(d), 2.0)
        for (d in listOf(0.0, 3.0, 6.0)) close(beaconEvery(d), 0.5)
        close(beaconEvery(18.0), 1.25)
        var prev = 0.0
        var d = 0.0
        while (d <= 40) {
            assertTrue(beaconEvery(d) >= prev)
            prev = beaconEvery(d)
            d += 0.5
        }
    }

    private val me = Vec(20.0, 10.0)

    private fun word(dx: Double, dy: Double, heard: SideWord?) = sideWord(me, Vec(me.x + dx, me.y + dy), false, heard)

    @Test
    fun sideWordForTheBallAfterASideWordAheadOnlyWithin03mOfYourLine() {
        for (heard in listOf(null, SideWord.LEFT, SideWord.RIGHT, SideWord.BEHIND)) {
            assertEquals(SideWord.AHEAD, word(10.0, 0.29, heard))
            assertEquals(SideWord.AHEAD, word(10.0, -0.29, heard))
            assertEquals(SideWord.RIGHT, word(10.0, 0.4, heard), "the wide band holds only once ahead is heard")
            assertEquals(SideWord.LEFT, word(1.0, -0.4, heard))
            assertEquals(SideWord.AHEAD, word(-0.5, 0.0, heard), "half a metre back is in reach, not behind")
            assertEquals(SideWord.BEHIND, word(-0.6, 0.0, heard))
        }
    }

    @Test
    fun sideWordForTheBallOnceAheadIsHeardTheBandNarrowsAsItGetsClose() {
        val a = SideWord.AHEAD
        assertEquals(SideWord.AHEAD, word(10.0, 0.0, a))
        assertEquals(SideWord.AHEAD, word(10.0, 1.5, a))
        assertEquals(SideWord.AHEAD, word(10.0, -1.5, a))
        assertEquals(SideWord.RIGHT, word(10.0, 1.6, a))
        assertEquals(SideWord.LEFT, word(10.0, -1.6, a))
        assertEquals(SideWord.AHEAD, word(4.0, 1.0, a), "the band narrows linearly: 1 m wide at 4 m")
        assertEquals(SideWord.RIGHT, word(4.0, 1.1, a))
        assertEquals(SideWord.AHEAD, word(1.0, 0.5, a))
        assertEquals(SideWord.RIGHT, word(1.0, 0.6, a), "close by, a ball the take distance off your line is to the side")
        assertEquals(SideWord.LEFT, word(0.5, -0.6, a))
        assertEquals(SideWord.LEFT, word(0.0, -8.0, a))
        assertEquals(SideWord.RIGHT, word(0.0, 8.0, a))
        assertEquals(SideWord.AHEAD, word(-0.5, 0.0, a), "half a metre back is in reach, not behind")
        assertEquals(SideWord.BEHIND, word(-0.6, 0.0, a), "close by, behind starts at 0.5 m")
        assertEquals(SideWord.BEHIND, word(-10.0, 6.0, a))
        assertEquals(SideWord.BEHIND, word(-1.1, 0.0, a))
    }

    @Test
    fun sideWordForTheGoalIsAheadExactlyWhenAStraightKickLinesUpAtAnyDistance() {
        val en = gameStrings(Lang.EN)
        for (x in listOf(5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0)) {
            var y = 0.5
            while (y <= 19.5) {
                val ball = Vec(x, y)
                val player = Vec(x - 0.5, y)
                val text = guideText(player, en, ball)
                val word = sideWord(player, ball, true)
                if (PITCH_L - x <= SHOT_RANGE) assertEquals("straight ahead" in text, word == SideWord.AHEAD, "$x,$y $text")
                else assertTrue("too far to shoot" in text, "beyond range W says so, whatever the word")
                assertTrue(word != SideWord.BEHIND, "the goal is never behind, and always has a word")
                if (word == SideWord.LEFT) assertTrue(y > GOAL.y)
                if (word == SideWord.RIGHT) assertTrue(y < GOAL.y)
                y += 0.25
            }
        }
        assertEquals(SideWord.AHEAD, sideWord(Vec(5.0, 10.0), Vec(5.5, 10.0), true), "far off and lined up: ahead stops a sideways run")
        assertEquals(SideWord.RIGHT, sideWord(Vec(5.0, 3.0), Vec(5.5, 3.0), true), "far off, the sides still steer")
    }

    @Test
    fun nearBoardIsWithinBoardNearOfTheLeftOrRightBoardElseNone() {
        assertEquals(Side.LEFT, nearBoard(Vec(0.0, 1.4)))
        assertEquals(Side.LEFT, nearBoard(Vec(0.0, 0.3)))
        assertNull(nearBoard(Vec(0.0, 1.5)))
        assertNull(nearBoard(Vec(0.0, 10.0)))
        assertEquals(Side.RIGHT, nearBoard(Vec(0.0, 18.6)))
    }

    @Test
    fun sideCallerSpeaksOneWordPerGapAndNeverTheSameWordTwiceInARow() {
        val c = SideCaller()
        assertEquals(SideWord.LEFT, c.next(SideWord.LEFT, 0), "the first call goes at once")
        assertEquals(SideWord.LEFT, c.next(SideWord.LEFT, 100), "not yet spoken, so still offered")
        c.spoke(SideWord.LEFT, 100)
        assertNull(c.next(SideWord.RIGHT, 100 + SIDE_GAP - 1), "too soon")
        assertEquals(SideWord.RIGHT, c.next(SideWord.RIGHT, 100 + SIDE_GAP))
        assertNull(c.next(SideWord.LEFT, 100 + SIDE_GAP), "the same word again")
        assertNull(c.next(SideWord.LEFT, 99999), "never repeated, however long ago")
        c.spoke(SideWord.RIGHT, 2000)
        assertEquals(SideWord.LEFT, c.next(SideWord.LEFT, 4000), "the side changed back")
        c.reset()
        assertEquals(SideWord.LEFT, c.next(SideWord.LEFT, 4001), "a new round starts afresh")
        assertNull(c.heard)
        c.spoke(SideWord.LEFT, 5000)
        assertEquals(SideWord.LEFT, c.heard)
        assertEquals(SideWord.AHEAD, c.next(SideWord.AHEAD, 5001), "ahead, the word to stop stepping sideways, skips the gap")
        c.spoke(SideWord.AHEAD, 5001)
        assertNull(c.next(SideWord.AHEAD, 9999), "but is not repeated either")
    }
}
