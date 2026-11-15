package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

// Web Audio's exponentialRampToValueAtTime: from 0.0001 up to the gain, held if asked, back down to 0.0001.
class EnvelopeTest {
    @Test
    fun rampsUpExponentiallyHoldsThenRampsDown() {
        val env = Envelope(gain = 0.5, attack = 0.01, hold = 0.02, decay = 0.03)
        val values = DoubleArray(frames(0.06)) { env.next() }
        assertEquals(0.0001, values[0], 1e-12)
        // halfway up an exponential ramp is the geometric mean of its ends
        assertEquals(sqrt(0.0001 * 0.5), values[frames(0.005)], 0.002)
        assertEquals(0.5, values[frames(0.01)], 1e-9)
        assertEquals(0.5, values[frames(0.02)], 1e-9)
        assertEquals(0.5, values[frames(0.03) - 1], 1e-9)
        assertEquals(sqrt(0.0001 * 0.5), values[frames(0.045)], 0.002)
        assertTrue(values.last() < 0.0002)
        assertTrue(env.finished)
    }

    @Test
    fun isNotFinishedBeforeItsLastSample() {
        val env = Envelope(gain = 1.0, attack = 0.001, hold = 0.0, decay = 0.001)
        repeat(frames(0.002) - 1) { env.next() }
        assertFalse(env.finished)
        env.next()
        assertTrue(env.finished)
    }
}
