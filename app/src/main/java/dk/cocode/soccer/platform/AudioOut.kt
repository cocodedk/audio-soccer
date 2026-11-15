package dk.cocode.soccer.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.core.audio.SAMPLE_RATE
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max

private const val CHUNK_FRAMES = SAMPLE_RATE / 50 // 20 ms
private const val BYTES_PER_FRAME = Short.SIZE_BYTES // mono

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
// within a buffer or two, so stop() never waits for the audio hardware. If the track dies the thread ends the same way and
// the output stays silent until stop() and start() open a new one.
class AudioOut(private val mixer: Mixer) : Audio {
    // The running thread's own flag, so a thread slow to end can never be revived by the next start().
    private var live: AtomicBoolean? = null

    @Synchronized
    override fun start() {
        if (live != null) return
        val minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val frames = max(CHUNK_FRAMES, minBytes / BYTES_PER_FRAME)
        val track = newTrack(bufferBytes = 2 * frames * BYTES_PER_FRAME)
        val flag = AtomicBoolean(true)
        track.play()
        thread(isDaemon = true, name = "audio-out") {
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
