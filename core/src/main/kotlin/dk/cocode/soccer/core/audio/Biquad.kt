package dk.cocode.soccer.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal enum class FilterType { BANDPASS, LOWPASS, HIGHPASS }

// A second-order filter with the RBJ cookbook coefficients as Web Audio's BiquadFilterNode computes them. q is the
// bandpass's linear Q; for the lowpass and the highpass it is a resonance in dB (the node's own quirk, so the trial's
// default q = 1 there is 1 dB). Transposed direct form II.
internal class Biquad(type: FilterType, freq: Double, q: Double) {
    private val w0 = 2 * PI * freq / SAMPLE_RATE
    private val cosW = cos(w0)
    private val alpha = sin(w0) / (2 * if (type == FilterType.BANDPASS) q else 10.0.pow(q / 20))
    private val a0 = 1 + alpha
    private val b0 = when (type) {
        FilterType.BANDPASS -> alpha
        FilterType.LOWPASS -> (1 - cosW) / 2
        FilterType.HIGHPASS -> (1 + cosW) / 2
    } / a0
    private val b1 = when (type) {
        FilterType.BANDPASS -> 0.0
        FilterType.LOWPASS -> 1 - cosW
        FilterType.HIGHPASS -> -(1 + cosW)
    } / a0
    private val b2 = if (type == FilterType.BANDPASS) -b0 else b0
    private val a1 = -2 * cosW / a0
    private val a2 = (1 - alpha) / a0
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }
}
