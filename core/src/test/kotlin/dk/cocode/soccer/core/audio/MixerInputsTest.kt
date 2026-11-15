package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// A value that is not a number must not reach render(), where it would make the conversion to 16 bits throw.
class MixerInputsTest {
    @Test
    fun aSoundWithAGainOrStretchThatIsNotFiniteIsDroppedAndTheRestOfTheBatchPlays() {
        val mixer = Mixer(1)
        mixer.play(Sound.CHIME, stretch = Double.NaN)
        mixer.play(Sound.CHIME, stretch = Double.POSITIVE_INFINITY)
        mixer.play(Sound.TAP, gain = Double.NaN)
        mixer.play(Sound.TAP, gain = Double.NEGATIVE_INFINITY)
        mixer.play(Sound.TICK)
        assertArrayEquals(playOnce(Sound.TICK, 0.1), mixer.renderSeconds(0.1))
    }

    @Test
    fun aCrowdLevelThatIsNotFiniteIsIgnoredSoTheBedPlaysOnAtItsLevel() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(2.0)
        val before = mixer.renderSeconds(1.0).rms()
        mixer.crowd(Double.NaN)
        mixer.renderSeconds(0.1) // in separate renders, so that one cannot hide the other
        mixer.crowd(Double.POSITIVE_INFINITY)
        mixer.renderSeconds(0.1)
        assertEquals(before, mixer.renderSeconds(1.0).rms(), 0.3 * before)
    }

    @Test
    fun aNegativeCrowdLevelStopsTheBedLikeZero() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(2.0)
        mixer.crowd(-1.0)
        assertTrue(mixer.renderSeconds(2.0).between(1.3, 2.0).isSilent())
        assertEquals(0, mixer.voiceCount)
    }
}
