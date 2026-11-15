package dk.cocode.soccer.platform

import android.media.AudioTrack
import dk.cocode.soccer.core.audio.Mixer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioTrack

// A phone that will not play the track: the adapter stays stopped, logs once, and holds on to nothing.
class AudioOutFailureTest : AudioTestBase() {
    @Test
    fun aTrackThatWillNotPlayIsReleasedAndAnotherStartMayTryAgain() {
        var attempted: AudioTrack? = null
        RecordingAudioTrackShadow.onPlay = { attempted = it }
        ShadowAudioTrack.enableIllegalStateOnPlay(true)
        val out = AudioOut(Mixer(1L))
        out.start()
        assertEquals(0, audioThreads())
        assertEquals("the track was released", AudioTrack.STATE_UNINITIALIZED, attempted?.state)
        assertEquals(1, warnings().size)
        ShadowAudioTrack.enableIllegalStateOnPlay(false)
        out.start()
        assertEquals(AudioTrack.PLAYSTATE_PLAYING, awaitWrite().playState)
        stopAndWait(out)
        assertEquals(1, warnings().size)
    }
}

@Config(shadows = [FailingBuildShadow::class])
class AudioOutBuildFailureTest : AudioTestBase() {
    @Test
    fun aTrackThePhoneCannotBuildLeavesTheAdapterStopped() {
        val out = AudioOut(Mixer(1L))
        out.start()
        assertEquals(0, audioThreads())
        assertEquals(1, warnings().size)
        out.start()
        assertEquals("a later start tries again", 2, warnings().size)
    }
}
