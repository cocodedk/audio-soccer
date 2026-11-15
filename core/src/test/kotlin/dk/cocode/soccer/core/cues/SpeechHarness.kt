package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.speech.SpeechQueue
import dk.cocode.soccer.core.speech.Voice
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings

// A speech queue over a voice that keeps each line "playing" until finish() is called, as the fake engine of
// prototype/test/helpers.mjs does, with a clock the test sets and the cues wired to it.
class SpeechHarness(lang: Lang = Lang.EN) {
    val spoken = mutableListOf<String>()
    var t = 0L
    private var playing = false
    private var onDone: () -> Unit = {}

    private val voice = object : Voice {
        override fun speak(text: String, pitch: Float, onDone: () -> Unit) {
            spoken += text
            this@SpeechHarness.onDone = onDone
            playing = true
        }

        override fun stop() {
            playing = false
        }
    }
    val queue = SpeechQueue(voice) { t }
    val cues = Cues(gameStrings(lang), queue::say) { t }

    fun finish() {
        playing = false
        onDone()
        queue.tick()
    }

    // Let every line play out, one after another.
    fun finishAll() {
        repeat(30) { if (playing) finish() }
    }
}
