package dk.cocode.soccer.core.text

import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.createWorld
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/describe-fa.test.mjs. A ZWNJ (U+200C) is written as the escape, so it can be seen.
class DescribeFaTest {
    private val fa = gameStrings(Lang.FA)
    private val en = gameStrings(Lang.EN)

    @Test
    fun persianClockPositionsAndMetresUsePersianDigits() {
        assertEquals("ساعت ۳، ۳ متر", clockText(0.0, 3.0, fa))
        assertEquals("ساعت ۱۲، ۵ متر", clockText(5.0, 0.0, fa))
        assertEquals("ساعت ۶، ۴ متر", clockText(-4.0, 0.0, fa))
        assertEquals("ساعت ۹، ۷ متر", clockText(0.0, -7.0, fa))
        assertEquals("ساعت ۱، ۳ متر", clockText(2.6, 1.5, fa))
        assertEquals("جلوی پایتان", clockText(0.5, 0.0, fa))
        assertEquals("ساعت ۳، ۲ متر", clockTo(Vec(10.0, 10.0), Vec(10.0, 12.0), fa))
        assertEquals("۱۲ متر", metresText(12.4, fa))
        assertEquals("12 metres", metresText(12.4, en))
    }

    @Test
    fun persianThirdsAndSides() {
        assertEquals(listOf("یک‌سوم دفاعی", "یک‌سوم میانی", "یک‌سوم حمله"), listOf(0, 1, 2).map { thirdName(it, fa) })
        assertEquals("کنار دیواره چپ", sideText(1.0, fa))
        assertEquals("کنار دیواره راست", sideText(19.0, fa))
        assertEquals("وسط", sideText(10.0, fa))
        assertEquals("Attacking third", thirdName(2, en))
    }

    @Test
    fun persianGuideFarStraightAheadSlightlyAndMetresToMove() {
        assertEquals("دروازه، ۳۰ متر، برای شوت زدن خیلی دور است", guideText(Vec(10.0, 10.0), fa))
        assertEquals("دروازه، ۹ متر، درست جلو", guideText(Vec(31.0, 10.0), fa))
        assertEquals("دروازه، ۱۱ متر، کمی به راست", guideText(Vec(30.0, 5.0), fa))
        assertEquals("دروازه، ۱۱ متر، کمی به چپ", guideText(Vec(30.0, 15.0), fa))
        assertEquals("دروازه، ۱۰ متر، ۶ متر به راست بروید", guideText(Vec(34.0, 2.0), fa))
        assertEquals("دروازه، ۱۰ متر، ۶ متر به چپ بروید", guideText(Vec(34.0, 18.5), fa))
        assertTrue(guideText(Vec(40 - SHOT_RANGE - 0.1, 10.0), fa).endsWith("خیلی دور است"))
    }

    @Test
    fun thePersianGuideSaysWhatTheEnglishOneDoesSameBranchForEverySpot() {
        fun branch(text: String, words: List<Regex>) = words.indexOfFirst { it.containsMatchIn(text) }
        val english = listOf("too far", "straight ahead", "slightly", "move").map { Regex(it) }
        val persian = listOf("خیلی دور", "درست جلو", "کمی", "بروید").map { Regex(it) }
        for (x in listOf(10.0, 20.0, 25.0, 30.0, 35.0)) {
            for (y in (2..38).map { it / 2.0 }) { // 1 to 19 in halves
                assertEquals(branch(guideText(Vec(x, y), en), english), branch(guideText(Vec(x, y), fa), persian), "$x,$y")
            }
        }
    }

    @Test
    fun persianWherePlaceBallGoalAndKeeperInOneGo() {
        val w = createWorld(Vec(20.0, 1.0), Vec(20.0, 4.0), hasBall = false, keeper = true)
        assertEquals(
            "یک‌سوم میانی، کنار دیواره چپ. توپ ساعت ۳، ۳ متر. دروازه، ۲۲ متر، ۷ متر به راست بروید. دروازه‌بان ساعت ۱، ۲۲ متر.",
            whereText(w, fa),
        )
        val mine = createWorld(Vec(34.0, 10.0), Vec(34.5, 10.0), hasBall = true)
        assertEquals("یک‌سوم حمله، وسط. توپ جلوی پایتان. دروازه، ۶ متر، درست جلو.", whereText(mine, fa))
    }
}
