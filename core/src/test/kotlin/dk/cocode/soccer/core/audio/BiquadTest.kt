package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

// The filters must behave as Web Audio's BiquadFilterNode does, which is what the trial's sounds were tuned on.
class BiquadTest {
    // The steady-state gain of the filter at one frequency, from the amplitude of a sine put through it.
    private fun gainAt(type: FilterType, freq: Double, q: Double, hz: Double): Double {
        val filter = Biquad(type, freq, q)
        var peak = 0.0
        for (n in 0 until SAMPLE_RATE) {
            val y = filter.process(sin(2 * PI * hz * n / SAMPLE_RATE))
            if (n > SAMPLE_RATE / 2) peak = maxOf(peak, abs(y))
        }
        return peak
    }

    @Test
    fun bandpassHasUnityGainAtItsCentreAndFallsOffBothSides() {
        assertEquals(1.0, gainAt(FilterType.BANDPASS, 2000.0, 5.0, 2000.0), 0.01)
        assertTrue(gainAt(FilterType.BANDPASS, 2000.0, 5.0, 200.0) < 0.1)
        assertTrue(gainAt(FilterType.BANDPASS, 2000.0, 5.0, 12000.0) < 0.5)
    }

    @Test
    fun aNarrowerBandpassIsSelectiveAtTheEdges() {
        assertTrue(gainAt(FilterType.BANDPASS, 2000.0, 10.0, 2500.0) < gainAt(FilterType.BANDPASS, 2000.0, 1.0, 2500.0))
    }

    @Test
    fun lowpassPassesLowAndCutsHigh() {
        assertEquals(1.0, gainAt(FilterType.LOWPASS, 1000.0, 0.0, 50.0), 0.01)
        assertTrue(gainAt(FilterType.LOWPASS, 1000.0, 0.0, 8000.0) < 0.05)
    }

    @Test
    fun highpassPassesHighAndCutsLow() {
        assertEquals(1.0, gainAt(FilterType.HIGHPASS, 1000.0, 0.0, 15000.0), 0.01)
        assertTrue(gainAt(FilterType.HIGHPASS, 1000.0, 0.0, 100.0) < 0.05)
    }

    // Web Audio gives the lowpass and the highpass their resonance Q in dB, so Q = 20 is a tenfold peak at the cutoff.
    @Test
    fun theLowpassAndHighpassTakeQInDecibels() {
        assertEquals(10.0.pow(20.0 / 20), gainAt(FilterType.LOWPASS, 1000.0, 20.0, 1000.0), 0.5)
        assertEquals(10.0.pow(20.0 / 20), gainAt(FilterType.HIGHPASS, 1000.0, 20.0, 1000.0), 0.5)
    }
}
