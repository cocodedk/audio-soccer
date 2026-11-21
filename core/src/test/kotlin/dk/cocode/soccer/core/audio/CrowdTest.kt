package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrowdTest {
    @Test
    fun crowdFadesInInsteadOfClicking() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        val out = mixer.renderSeconds(2.0)
        assertTrue(out.between(0.0, 0.01).rms() < out.between(1.5, 2.0).rms() / 10, "the first 10 ms are almost silent")
    }

    @Test
    fun crowdFollowsTheLatestLevelSmoothly() {
        val mixer = Mixer(1)
        mixer.crowd(0.045)
        mixer.renderSeconds(2.0)
        val low = mixer.renderSeconds(1.0).rms()
        mixer.crowd(0.3)
        val swell = mixer.renderSeconds(1.2)
        mixer.renderSeconds(0.3)
        val high = mixer.renderSeconds(1.0).rms()
        assertTrue(high / low in 5.0..8.5, "0.3 is 6.7 times 0.045, got ${high / low}")
        assertTrue(swell.between(0.0, 0.1).rms() < 0.5 * high, "the swell takes a moment")
        assertTrue(swell.between(0.5, 0.6).rms() in 0.35 * high..0.8 * high, "and is about half way after half of it")
    }

    @Test
    fun stoppingACrowdThatIsNotPlayingChangesNothing() {
        val mixer = Mixer(1)
        mixer.crowd(0.0)
        assertTrue(mixer.renderSeconds(0.1).isSilent())
        assertEquals(0, mixer.voiceCount)
    }

    @Test
    fun aCrowdStartedAgainAfterStoppingPlays() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(2.0)
        mixer.crowd(0.0)
        mixer.renderSeconds(2.0)
        mixer.crowd(0.3)
        assertFalse(mixer.renderSeconds(1.0).between(0.5, 1.0).isSilent())
    }

    @Test
    fun aCrowdStartedRightAfterSilenceIsANewBedNotTheFadingOne() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(1.0)
        mixer.silence()
        mixer.crowd(0.3)
        assertFalse(mixer.renderSeconds(1.0).between(0.5, 1.0).isSilent())
        assertEquals(1, mixer.voiceCount)
    }

    @Test
    fun stopAndStartInOneBlockKeepsTheCrowd() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(1.0)
        mixer.crowd(0.0)
        mixer.crowd(0.3)
        assertFalse(mixer.renderSeconds(2.0).between(1.5, 2.0).isSilent())
        assertEquals(1, mixer.voiceCount)
    }
}
