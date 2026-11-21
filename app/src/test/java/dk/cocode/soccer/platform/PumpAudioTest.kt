package dk.cocode.soccer.platform

import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.core.audio.Sound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PumpAudioTest {
    @Test
    fun rendersTheMixerIntoTheBufferBeforeEveryWrite() {
        val mixer = Mixer(1L)
        mixer.play(Sound.TICK)
        var rounds = 0
        val heard = mutableListOf<Boolean>()
        pumpAudio(mixer, ShortArray(882), { rounds++ < 3 }) { heard += !it.isSilent(); it.size }
        assertEquals(3, heard.size)
        assertTrue("the first buffer holds the tick", heard.first())
    }

    @Test
    fun writesTheWholeBuffer() {
        var length = 0
        pumpAudio(Mixer(1L), ShortArray(882), { length == 0 }) { length = it.size; it.size }
        assertEquals(882, length)
    }

    @Test
    fun neverRendersOrWritesOnceTold() {
        var writes = 0
        pumpAudio(Mixer(1L), ShortArray(882), { false }) { writes++; it.size }
        assertEquals(0, writes)
    }

    @Test
    fun endsWhenAWriteFails() {
        var writes = 0
        pumpAudio(Mixer(1L), ShortArray(882), { true }) { writes++; -6 }
        assertEquals(1, writes)
    }
}
