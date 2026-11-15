package dk.cocode.soccer.core.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class MixerTest {
    @Test
    fun silentMixerRendersZeros() {
        assertTrue(Mixer(1).renderSeconds(0.5).isSilent())
    }

    @Test
    fun everySoundStartsAndEnds() {
        for (sound in Sound.entries) {
            val mixer = Mixer(1)
            mixer.play(sound)
            assertFalse(mixer.renderSeconds(0.05).isSilent(), "$sound is silent at the start")
            mixer.renderSeconds(3.95)
            assertTrue(mixer.renderSeconds(0.1).isSilent(), "$sound still sounds after 4 s")
        }
    }

    @Test
    fun peakStaysUnderTheCap() {
        val cap = 0.9 * Short.MAX_VALUE
        val mixer = Mixer(1)
        repeat(20) { mixer.play(Sound.entries[it % Sound.entries.size]) }
        val peak = mixer.renderSeconds(4.0).peak()
        assertTrue(peak <= cap, "peak $peak of a mixed 20")
        // the worst case for a limiter: twenty of one sound, all starting in step
        for (sound in Sound.entries) {
            val same = Mixer(1)
            repeat(20) { same.play(sound) }
            val samePeak = same.renderSeconds(4.0).peak()
            assertTrue(samePeak <= cap, "peak $samePeak of 20 x $sound")
        }
    }

    @Test
    fun tickIsBrightKickIsLow() {
        val tick = playOnce(Sound.TICK, 0.1)
        val kick = playOnce(Sound.KICK, 0.1)
        assertTrue(tick.energyAt(4800.0) > tick.energyAt(200.0))
        assertTrue(kick.energyAt(200.0) > kick.energyAt(4800.0))
    }

    @Test
    fun gainScalesLoudness() {
        val full = playOnce(Sound.TICK, 0.1, gain = 1.0).rms()
        val half = playOnce(Sound.TICK, 0.1, gain = 0.5).rms()
        assertTrue(half / full in 0.45..0.55, "ratio ${half / full}")
    }

    @Test
    fun behindRemovesTreble() {
        val ahead = playOnce(Sound.TICK, 0.1).energyAt(4800.0)
        val behind = playOnce(Sound.TICK, 0.1, behind = true).energyAt(4800.0)
        assertTrue(behind < ahead / 4, "behind $behind, ahead $ahead")
    }

    @Test
    fun sameSeedRendersTheSameSamplesAndAnotherSeedDoesNot() {
        assertArrayEquals(playOnce(Sound.RATTLE, 0.1, seed = 7), playOnce(Sound.RATTLE, 0.1, seed = 7))
        assertFalse(playOnce(Sound.RATTLE, 0.1, seed = 7).contentEquals(playOnce(Sound.RATTLE, 0.1, seed = 8)))
    }

    @Test
    fun renderFillsOnlyTheFramesAsked() {
        val mixer = Mixer(1)
        mixer.play(Sound.TAP)
        val out = ShortArray(4000) { 77 }
        mixer.render(out, 3000)
        assertTrue(out.copyOfRange(3000, 4000).all { it.toInt() == 77 })
        assertTrue(out.copyOfRange(0, 3000).any { it.toInt() != 77 })
    }

    @Test
    fun aLongRenderEqualsTheSameTimeInSmallBlocks() {
        val whole = playOnce(Sound.GOAL, 1.0) // 44100 frames: several chunks
        val mixer = Mixer(1)
        mixer.play(Sound.GOAL)
        val parts = ShortArray(whole.size)
        val block = ShortArray(441)
        for (i in 0 until 100) {
            mixer.render(block, 441)
            block.copyInto(parts, i * 441)
        }
        assertArrayEquals(whole, parts)
    }

    @Test
    fun silenceFadesEverythingOutWithoutAClick() {
        val mixer = Mixer(1)
        mixer.play(Sound.GOAL)
        mixer.crowd(0.3)
        mixer.renderSeconds(0.5)
        mixer.silence()
        val fade = mixer.renderSeconds(0.2)
        assertTrue(fade.between(0.0, 0.01).rms() > 3 * fade.between(0.05, 0.06).rms(), "the fade is gradual")
        assertTrue(fade.between(0.08, 0.2).isSilent(), "silent after the fade")
        assertEquals(0, mixer.voiceCount)
    }

    @Test
    fun aSoundPlayedAfterSilenceIsNotCutOff() {
        val mixer = Mixer(1)
        mixer.crowd(0.3)
        mixer.renderSeconds(0.2)
        mixer.silence()
        mixer.play(Sound.TICK)
        val out = mixer.renderSeconds(0.3)
        assertNotEquals(0, out.between(0.0, 0.05).peak())
        assertTrue(out.between(0.15, 0.3).isSilent())
    }

    @Test
    fun soundsPlayedFromSeveralThreadsAreAllKept() {
        val mixer = Mixer(1)
        val block = ShortArray(1)
        val threads = List(4) { thread { repeat(250) { mixer.play(Sound.POST) } } }
        repeat(500) { mixer.render(block, 1) } // draining while the others push
        threads.forEach { it.join() }
        mixer.render(block, 1)
        assertEquals(4 * 250 * 5, mixer.voiceCount, "POST is four tones and a burst")
    }
}
