package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

// snap8 from prototype/phone/js/gestures.js (its test is in phone-gestures.test.mjs).
class VecTest {
    private val up = Vec(1.0, 0.0)
    private val none = Vec(0.0, 0.0)

    @Test
    fun snappingGivesTheEightDirectionsWithExactZeros() {
        assertEquals(up, snap8(1.0, 0.3)) // 17 degrees off still runs straight up the pitch
        assertEquals(0.0.toRawBits(), snap8(1.0, 0.3).y.toRawBits(), "an exact zero, so a straight run kicks straight")
        assertEquals(Vec(0.0, -1.0), snap8(0.0, -2.0))
        assertEquals(Vec(-1.0, 0.0), snap8(-1.0, 0.1))
        assertEquals(up, snap8(1.0, 0.0))
        assertEquals(none, snap8(0.0, 0.0))
        val d = sqrt(0.5)
        assertEquals(Vec(d, d), snap8(1.0, 1.0))
        assertEquals(Vec(d, d), snap8(1.0, 0.9))
        assertEquals(Vec(d, -d), snap8(1.0, -1.0))
        assertEquals(Vec(-d, d), snap8(-1.0, 1.0))
        assertEquals(Vec(-d, -d), snap8(-1.0, -1.0))
        assertEquals(Vec(0.0, 1.0), snap8(0.0, 5.0))
    }

    @Test
    fun distanceAndClamp() {
        close(dist(Vec(0.0, 0.0), Vec(3.0, 4.0)), 5.0)
        assertEquals(1.0, clamp(0.0, 1.0, 2.0))
        assertEquals(2.0, clamp(3.0, 1.0, 2.0))
        assertEquals(1.5, clamp(1.5, 1.0, 2.0))
    }

    @Test
    fun roundingGoesHalfUpAsInTheTrial() {
        assertEquals(19.0, roundHalfUp(18.5))
        assertEquals(0.0, roundHalfUp(-0.5))
        assertEquals(-1.0, roundHalfUp(-1.2))
    }
}
