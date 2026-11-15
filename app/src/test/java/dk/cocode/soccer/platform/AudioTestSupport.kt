package dk.cocode.soccer.platform

import android.media.AudioTrack
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAudioTrack

// An AudioTrack that hands every buffer written to it to the test: the stock shadow only sees byte writes, and the game
// writes shorts. It then waits a moment, as a real track does until it has room, so the audio thread does not spin.
@Implements(AudioTrack::class)
class RecordingAudioTrackShadow : ShadowAudioTrack() {
    @RealObject
    private lateinit var track: AudioTrack

    @Implementation
    fun native_write_short(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int, format: Int, isBlocking: Boolean): Int {
        onWrite?.invoke(track, audioData.copyOfRange(offsetInShorts, offsetInShorts + sizeInShorts))
        Thread.sleep(WRITE_MS)
        return sizeInShorts
    }

    companion object {
        private const val WRITE_MS = 5L

        // Called on the audio thread with the track and a copy of what was written.
        @Volatile
        var onWrite: ((AudioTrack, ShortArray) -> Unit)? = null

        // The stock shadow keeps the minimum buffer size from one test to the next.
        fun reset() {
            onWrite = null
            setMinBufferSize(DEFAULT_MIN_BUFFER_SIZE)
        }
    }
}

fun ShortArray.isSilent() = all { it.toInt() == 0 }
