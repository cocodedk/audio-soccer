package dk.cocode.soccer.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.core.audio.Sound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.util.ReflectionHelpers
import kotlin.concurrent.thread

class AudioOutTest : AudioTestBase() {
    private val mixer = Mixer(1L)

    private fun firstWrite(out: AudioOut = AudioOut(mixer)): Written {
        out.start()
        return awaitWrite().also { stopAndWait(out) }
    }

    @Test
    fun streamsTheMixerAsMonoPcmAt44100OnAGameTrack() {
        mixer.play(Sound.TICK)
        val write = firstWrite()
        val format = write.track.format
        assertEquals(44100, format.sampleRate)
        assertEquals(AudioFormat.CHANNEL_OUT_MONO, format.channelMask)
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, format.encoding)
        val attributes = ReflectionHelpers.getField<AudioAttributes>(write.track, "mAttributes")
        assertEquals(AudioAttributes.USAGE_GAME, attributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION, attributes.contentType)
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, write.playState)
        assertFalse("the first buffer holds the tick", write.samples.isSilent())
    }

    @Test
    fun writesTwentyMillisecondsAtATime() {
        assertEquals(882, firstWrite().samples.size)
    }

    @Test
    fun writesTheTrackMinimumWhenItIsLarger() {
        ShadowAudioTrack.setMinBufferSize(20_000)
        assertEquals(10_000, firstWrite().samples.size)
    }

    @Test
    fun stopEndsTheThreadAndReleasesTheTrack() {
        val write = firstWrite()
        assertEquals(AudioTrack.STATE_UNINITIALIZED, write.track.state)
        written.clear()
        Thread.sleep(50)
        assertTrue("nothing is written once stopped", written.isEmpty())
    }

    @Test
    fun startAfterStopPlaysAgain() {
        val out = AudioOut(mixer)
        firstWrite(out)
        written.clear()
        val again = firstWrite(out)
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, again.playState)
    }

    @Test
    fun startingOrStoppingTwiceIsHarmless() {
        val out = AudioOut(mixer)
        out.stop()
        out.start()
        out.start()
        awaitWrite()
        assertEquals(1, audioThreads())
        stopAndWait(out)
        out.stop()
    }

    // The mixer is rendered by one thread at a time, so a start() right after a stop() waits for the old thread to end.
    @Test
    fun startWaitsForTheThreadOfTheLastRunToEnd() {
        val out = AudioOut(mixer)
        val gate = heldInWrite(out)
        thread {
            Thread.sleep(100)
            gate.countDown()
        }
        out.start()
        assertEquals("the old thread ended before the new one began", 1, audioThreads())
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, awaitWrite().playState)
        stopAndWait(out)
    }

    @Test
    fun startDoesNotRenderAgainWhileTheOldThreadIsStuckAndMayTryAgainLater() {
        val out = AudioOut(mixer)
        val gate = heldInWrite(out)
        out.start() // gives up after waiting a while
        assertEquals("still only the old thread", 1, audioThreads())
        assertEquals(1, warnings().size)
        assertTrue("no new track was opened", written.isEmpty())
        gate.countDown()
        out.start()
        awaitWrite()
        assertEquals(1, warnings().size)
        stopAndWait(out)
    }
}
