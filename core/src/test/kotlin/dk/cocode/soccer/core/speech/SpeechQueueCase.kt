package dk.cocode.soccer.core.speech

// The queue under test with a voice that keeps each line "playing" until finish() is called, as the fake engine of
// prototype/test/helpers.mjs does, and a clock the test sets.
abstract class SpeechQueueCase {
    protected class FakeVoice : Voice {
        val spoken = mutableListOf<String>()
        val pitches = mutableListOf<Float>()
        val dones = mutableListOf<() -> Unit>()
        var stops = 0
        var playing = false
        var instant = false // a voice that is done before speak() returns

        override fun speak(text: String, pitch: Float, onDone: () -> Unit) {
            spoken += text
            pitches += pitch
            dones += onDone
            playing = true
            if (instant) finish()
        }

        override fun stop() {
            stops += 1
            playing = false
        }

        fun finish() {
            playing = false
            dones.last()()
        }
    }

    protected val voice = FakeVoice()
    protected var t = 0L
    protected val queue = SpeechQueue(voice) { t }

    // Let every line play out, one after another.
    protected fun finishAll() {
        repeat(30) {
            if (voice.playing) {
                voice.finish()
                queue.tick()
            }
        }
    }
}
