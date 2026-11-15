package dk.cocode.soccer.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.core.audio.Sound
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

// One write of the audio thread, taken on that thread.
private class Written(val track: AudioTrack, val samples: ShortArray, val playState: Int)

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingAudioTrackShadow::class])
class AudioOutTest {
    private val mixer = Mixer(1L)
    private val written = LinkedBlockingQueue<Written>()

    init {
        RecordingAudioTrackShadow.onWrite = { track, samples -> written.offer(Written(track, samples, track.playState)) }
    }

    @After
    fun tearDown() = RecordingAudioTrackShadow.reset()

    private fun awaitWrite(): Written = checkNotNull(written.poll(5, TimeUnit.SECONDS)) { "the audio thread never wrote" }

    private fun audioThreads() = Thread.getAllStackTraces().keys.count { it.name == "audio-out" && it.isAlive }

    // stop() only tells the thread to end: wait until it has, and so has released its track.
    private fun stopAndWait(out: AudioOut) {
        out.stop()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (audioThreads() > 0 && System.nanoTime() < deadline) Thread.sleep(2)
        assertEquals("the audio thread did not end", 0, audioThreads())
    }

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
}
