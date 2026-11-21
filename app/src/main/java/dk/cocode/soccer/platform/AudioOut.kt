package dk.cocode.soccer.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.core.audio.SAMPLE_RATE
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max

private const val TAG = "AudioOut"
private const val CHUNK_FRAMES = SAMPLE_RATE / 50 // 20 ms
private const val BYTES_PER_FRAME = Short.SIZE_BYTES // mono
private const val JOIN_MS = 500L

// The audio thread's loop: render the next buffer of the mix, write it, again, until keepGoing says stop or a write fails
// (the track is dead). write returns what AudioTrack.write returns: the count written, or a negative error.
internal fun pumpAudio(mixer: Mixer, buffer: ShortArray, keepGoing: () -> Boolean, write: (ShortArray) -> Int) {
    while (keepGoing()) {
        mixer.render(buffer, buffer.size)
        if (write(buffer) < 0) return
    }
}

// Plays the mixer through one AudioTrack (44100 Hz, mono, 16 bits) fed by its own daemon thread. The mixer must be
// rendered for as long as the game can play sounds, since a sound played while nothing renders piles up in its pending
// list: so start() before the first sound and stop() only when none will come. A buffer is 20 ms, or the track's minimum
// when that is longer. The thread owns its track: stop() only tells it to end, and it stops and releases the track itself
// within a buffer or two, so stop() never waits for the audio hardware. The mixer is rendered by one thread at a time:
// a start() right after a stop() waits for the old thread to end first (500 ms at most, so start() may block that
// long). If the track dies the thread ends the same way
// and the output stays silent until stop() and start() open a new one. A phone that will not open or play the track
// leaves the adapter stopped (logged); a later start() tries again.
class AudioOut(private val mixer: Mixer) : Audio {
    // The running thread's own flag, so a thread slow to end can never be revived by the next start().
    private var live: AtomicBoolean? = null

    // The latest thread, which may still be ending after stop().
    private var audioThread: Thread? = null

    @Synchronized
    override fun start() {
        if (live != null) return
        audioThread?.let {
            it.join(JOIN_MS)
            if (it.isAlive) {
                Log.w(TAG, "The last audio thread has not ended: not starting")
                return
            }
        }
        val minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val frames = max(CHUNK_FRAMES, minBytes / BYTES_PER_FRAME)
        val track = openTrack(bufferBytes = 2 * frames * BYTES_PER_FRAME) ?: return
        val flag = AtomicBoolean(true)
        audioThread = thread(isDaemon = true, name = "audio-out") {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            try {
                pumpAudio(mixer, ShortArray(frames), flag::get) { track.write(it, 0, it.size) }
            } finally {
                track.stop()
                track.release()
            }
        }
        live = flag
    }

    @Synchronized
    override fun stop() {
        live?.set(false)
        live = null
    }

    // A track that is playing, or null (logged). build() throws when the track comes up uninitialised, which leaves
    // nothing to release; play() throws when the track is not in a state to play.
    private fun openTrack(bufferBytes: Int): AudioTrack? {
        val track = try {
            newTrack(bufferBytes)
        } catch (e: UnsupportedOperationException) {
            return failed(e)
        }
        try {
            track.play()
        } catch (e: IllegalStateException) {
            track.release()
            return failed(e)
        }
        return track
    }

    private fun failed(e: Exception): AudioTrack? {
        Log.w(TAG, "The audio output cannot be opened: the game stays silent", e)
        return null
    }

    private fun newTrack(bufferBytes: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setBufferSizeInBytes(bufferBytes)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
}
