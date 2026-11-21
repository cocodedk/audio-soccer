package dk.cocode.soccer.platform

import android.media.AudioTrack
import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

// An AudioTrack that tells the test what happens to it: the stock shadow only sees byte writes, and the game writes
// shorts. A write waits a moment, as a real track does until it has room, so the audio thread does not spin; with a gate
// set it waits for the gate instead, which holds the audio thread inside its write.
@Implements(AudioTrack::class)
class RecordingAudioTrackShadow : ShadowAudioTrack() {
    @RealObject
    private lateinit var track: AudioTrack

    @Implementation
    fun native_write_short(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int, format: Int, isBlocking: Boolean): Int {
        onWrite?.invoke(track, audioData.copyOfRange(offsetInShorts, offsetInShorts + sizeInShorts))
        gate?.await()
        Thread.sleep(WRITE_MS)
        return sizeInShorts
    }

    @Implementation
    override fun play() {
        onPlay?.invoke(track)
        super.play()
    }

    companion object {
        private const val WRITE_MS = 5L

        // Called on the audio thread with the track and a copy of what was written.
        @Volatile
        var onWrite: ((AudioTrack, ShortArray) -> Unit)? = null

        @Volatile
        var onPlay: ((AudioTrack) -> Unit)? = null

        @Volatile
        var gate: CountDownLatch? = null

        // The stock shadow keeps the minimum buffer size from one test to the next.
        fun reset() {
            gate?.countDown()
            gate = null
            onWrite = null
            onPlay = null
            setMinBufferSize(DEFAULT_MIN_BUFFER_SIZE)
        }
    }
}

// AudioTrack.Builder.build() on a phone that cannot open the track: the real one throws this when the track comes up
// uninitialised.
@Implements(AudioTrack.Builder::class)
class FailingBuildShadow {
    @Implementation
    fun build(): AudioTrack = throw UnsupportedOperationException("Cannot create AudioTrack")
}

// One write of the audio thread, taken on that thread.
class Written(val track: AudioTrack, val samples: ShortArray, val playState: Int)

fun ShortArray.isSilent() = all { it.toInt() == 0 }

// What the AudioOut tests share: the writes the audio thread makes, and waiting for it to end.
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingAudioTrackShadow::class])
abstract class AudioTestBase {
    protected val written = LinkedBlockingQueue<Written>()

    init {
        RecordingAudioTrackShadow.onWrite = { track, samples -> written.offer(Written(track, samples, track.playState)) }
    }

    @After
    fun tearDownAudio() = RecordingAudioTrackShadow.reset()

    protected fun awaitWrite(): Written = checkNotNull(written.poll(5, TimeUnit.SECONDS)) { "the audio thread never wrote" }

    protected fun audioThreads() = Thread.getAllStackTraces().keys.count { it.name == "audio-out" && it.isAlive }

    // The warnings AudioOut logged.
    protected fun warnings() = ShadowLog.getLogsForTag("AudioOut").filter { it.type == Log.WARN }

    // Starts the output, waits for its first write and stops it again: its thread is now held inside that write, until the
    // returned gate is opened.
    protected fun heldInWrite(out: AudioOut): CountDownLatch {
        val gate = CountDownLatch(1)
        RecordingAudioTrackShadow.gate = gate
        out.start()
        awaitWrite()
        out.stop()
        return gate
    }

    // stop() only tells the thread to end: wait until it has, and so has released its track.
    protected fun stopAndWait(out: AudioOut) {
        out.stop()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (audioThreads() > 0 && System.nanoTime() < deadline) Thread.sleep(2)
        assertEquals("the audio thread did not end", 0, audioThreads())
    }
}
