package dk.cocode.soccer.core.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

// Shared helpers for the audio tests.

fun Mixer.renderSeconds(seconds: Double): ShortArray {
    val frames = frames(seconds)
    return ShortArray(frames).also { render(it, frames) }
}

// One sound on a fresh mixer, rendered for the given time.
fun playOnce(
    sound: Sound,
    seconds: Double,
    gain: Double = 1.0,
    behind: Boolean = false,
    stretch: Double = 1.0,
    seed: Long = 1L,
): ShortArray {
    val mixer = Mixer(seed)
    mixer.play(sound, gain, behind, stretch)
    return mixer.renderSeconds(seconds)
}

// The samples between two times, in seconds.
fun ShortArray.between(from: Double, to: Double): ShortArray =
    copyOfRange(frames(from), minOf(size, frames(to)))

fun ShortArray.rms(): Double = sqrt(sumOf { it.toDouble() * it } / size)

fun ShortArray.peak(): Int = maxOf { abs(it.toInt()) }

fun ShortArray.isSilent(): Boolean = all { it.toInt() == 0 }

// The time of the last sample that is not zero, in seconds.
fun ShortArray.lastSoundSeconds(): Double = (indexOfLast { it.toInt() != 0 } + 1).toDouble() / SAMPLE_RATE

// The mean Goertzel energy over the bins from lo to hi: noise gives a wildly varying energy in any one bin, so a band
// says more about a noise sound than a single frequency does.
fun ShortArray.bandEnergy(lo: Double, hi: Double, step: Double = 25.0): Double =
    generateSequence(lo) { it + step }.takeWhile { it <= hi }.map { energyAt(it) }.average()

// Goertzel: the energy at one frequency, as the squared magnitude of that DFT bin. Windows of the same length
// compare with each other.
fun ShortArray.energyAt(hz: Double): Double {
    val coeff = 2 * cos(2 * PI * hz / SAMPLE_RATE)
    var s1 = 0.0
    var s2 = 0.0
    for (x in this) {
        val s = x + coeff * s1 - s2
        s2 = s1
        s1 = s
    }
    return s1 * s1 + s2 * s2 - coeff * s1 * s2
}
