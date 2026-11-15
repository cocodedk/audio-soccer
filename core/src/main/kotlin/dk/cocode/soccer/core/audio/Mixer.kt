package dk.cocode.soccer.core.audio

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

const val SAMPLE_RATE = 44100

// The game's sounds. Every one is synthesized (recipes in Sfx.kt); the app has no audio files.
enum class Sound { RATTLE, TICK, TAP, KICK, BOARD, END_WALL, BRUSH, STEP, CHIME, POST, SAVE, GOAL, KEEPER_STEP }

// What the soundscape plays into. behind runs the sound through a 2000 Hz lowpass; stretch (0.35 to 1) shortens the chime.
interface SoundSink {
    fun play(sound: Sound, gain: Double = 1.0, behind: Boolean = false, stretch: Double = 1.0)

    // The crowd bed's level, reached in a short ramp; 0.0 stops it.
    fun crowd(level: Double)
}

// Frames mixed at a time, so a render of any length needs no more than this one scratch buffer.
private const val CHUNK = 1024

private sealed interface Command {
    class Play(val sound: Sound, val gain: Double, val behind: Boolean, val stretch: Double) : Command
    class SetCrowd(val level: Double) : Command
    data object Silence : Command
}

// Mixes every sound into mono 16-bit samples: voices, then the limiter. play(), crowd() and silence() may come from any
// thread: each only pushes an immutable command onto a pending list with compare-and-set, and render() takes the list at
// its start. Voices, filters, the limiter and the random generator are touched by render() alone, so a render needs no
// lock and allocates only for the commands it takes. All randomness comes from Random(seed): each noise voice gets its own
// generator from it, so the same calls render the same samples whatever the block sizes.
@OptIn(ExperimentalAtomicApi::class)
class Mixer(seed: Long) : SoundSink {
    private val rng = Random(seed)
    private val pending = AtomicReference<List<Command>>(emptyList())
    private val voices = ArrayList<Voice>()
    private val sfx = Sfx(rng, voices)
    private val limiter = Limiter()
    private val scratch = FloatArray(CHUNK)
    private var crowd: Crowd? = null

    internal val voiceCount get() = voices.size

    override fun play(sound: Sound, gain: Double, behind: Boolean, stretch: Double) =
        push(Command.Play(sound, gain, behind, stretch))

    override fun crowd(level: Double) = push(Command.SetCrowd(level))

    // Fades everything that sounds out over 60 ms; sounds played after it are not touched.
    fun silence() = push(Command.Silence)

    private fun push(command: Command) {
        while (true) {
            val before = pending.load()
            if (pending.compareAndSet(before, before + command)) return
        }
    }

    // Fills out[0, frames) with the next frames of the mix.
    fun render(out: ShortArray, frames: Int) {
        drain()
        var start = 0
        while (start < frames) {
            val n = min(CHUNK, frames - start)
            scratch.fill(0f, 0, n)
            mixVoices(n)
            for (i in 0 until n) {
                out[start + i] = (limiter.process(scratch[i].toDouble()) * Short.MAX_VALUE).roundToInt().toShort()
            }
            start += n
        }
    }

    private fun drain() {
        val commands = pending.exchange(emptyList())
        for (i in commands.indices) {
            when (val c = commands[i]) {
                is Command.Play -> sfx.play(c.sound, c.gain, c.behind, c.stretch)
                is Command.SetCrowd -> setCrowd(c.level)
                Command.Silence -> silenceAll()
            }
        }
    }

    // The crowd bed fades with the rest, and a crowd() after it starts a new bed, as the trial's stopAll() does.
    private fun silenceAll() {
        crowd = null
        for (i in voices.indices) voices[i].fadeOut()
    }

    private fun setCrowd(level: Double) {
        var bed = crowd
        if (bed == null) {
            if (level <= 0.0) return
            bed = Crowd(Random(rng.nextLong()))
            crowd = bed
            voices += bed
        }
        bed.ramp(level)
    }

    private fun mixVoices(n: Int) {
        var i = voices.size - 1
        while (i >= 0) {
            val v = voices[i]
            v.mix(scratch, n)
            if (v.done) {
                if (v === crowd) crowd = null
                voices.removeAt(i)
            }
            i--
        }
    }
}
