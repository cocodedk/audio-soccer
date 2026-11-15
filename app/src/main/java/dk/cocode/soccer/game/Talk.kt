package dk.cocode.soccer.game

import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.SpeechQueue
import dk.cocode.soccer.core.speech.Voice
import dk.cocode.soccer.core.text.GameStrings
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.UiStrings
import dk.cocode.soccer.core.text.gameStrings
import dk.cocode.soccer.core.text.uiStrings
import dk.cocode.soccer.platform.Engine
import dk.cocode.soccer.platform.Prefs
import dk.cocode.soccer.platform.Speech
import dk.cocode.soccer.platform.TtsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Everything the game says: the speech engine in use, the language it speaks, and the queue in front of it. A line is
// kept as lastLine for the screen, and goes to the queue unless muted() (a screen reader is on, or the app is in the
// background). The engine is built from factory, which takes the package of the engine to use, null for the system's.
internal class Talk(
    private val factory: (String?) -> Speech,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
    nowMs: () -> Long,
    private val muted: () -> Boolean,
) {
    private var speech: Speech? = null
    private var wantLang = Lang.EN // what the player chose
    private var wantEngine: String? = null // null: the system's
    private var switching = false

    // The language the engine really speaks. It differs from wantLang when the engine has no voice for it.
    var lang = Lang.EN
        private set

    val strings: GameStrings get() = gameStrings(lang)
    val ui: UiStrings get() = uiStrings(lang)

    // The player asked for a language the engine cannot speak, so everything is English.
    val fellBack: Boolean get() = lang != wantLang

    // The engine in use, as the player knows it.
    val engineLabel: String
        get() {
            val engines = speech?.engines().orEmpty()
            return engines.getOrNull(current(engines))?.label.orEmpty()
        }

    private val mutableLastLine = MutableStateFlow("")
    val lastLine: StateFlow<String> = mutableLastLine

    // The queue speaks through whichever engine is current; with none up yet a line is finished at once.
    val queue = SpeechQueue(
        object : Voice {
            override fun speak(text: String, pitch: Float, onDone: () -> Unit) = speech?.speak(text, pitch, onDone) ?: onDone()

            override fun stop() {
                speech?.stop()
            }
        },
        nowMs,
    )

    fun say(line: Line) {
        mutableLastLine.value = line.text
        if (!muted()) queue.say(line)
    }

    fun clear() = queue.clear()

    fun close() {
        speech?.shutdown()
    }

    // Reads what the player chose and starts the engine. False: no engine speaks.
    suspend fun start(): Boolean {
        wantLang = prefs.lang.first()
        wantEngine = prefs.enginePkg.first()
        return connect(wantEngine)
    }

    // Builds the engine, waits until it is up and tells it the language. An engine the player chose that will not start
    // (it was uninstalled) gives way to the system's.
    private suspend fun connect(pkg: String?): Boolean {
        var ready = open(pkg)
        if (!ready && pkg != null) ready = open(null)
        if (ready) lang = checkNotNull(speech).useLanguage(wantLang)
        return ready
    }

    private suspend fun open(pkg: String?): Boolean {
        speech?.shutdown()
        val s = factory(pkg)
        speech = s
        return s.state.first { it != TtsState.LOADING } == TtsState.READY
    }

    // Where the engine in use is in the list: with none chosen, the system's, which Android ranks first.
    private fun current(engines: List<Engine>) = engines.indexOfFirst { it.pkg == wantEngine }.coerceAtLeast(0)

    // The next language. The caller says what it is, in the new language.
    fun nextLanguage() {
        wantLang = Lang.entries[(wantLang.ordinal + 1) % Lang.entries.size]
        scope.launch { prefs.setLang(wantLang) }
        lang = checkNotNull(speech).useLanguage(wantLang)
    }

    // Switches to the next installed engine, and calls done(true) once it speaks, done(false) if none does. With one
    // engine only there is nothing to switch to.
    fun nextEngine(done: (Boolean) -> Unit) {
        val engines = speech?.engines().orEmpty()
        if (switching) return
        if (engines.size < 2) return done(true)
        val next = engines[(current(engines) + 1) % engines.size].pkg
        wantEngine = next
        switching = true
        scope.launch {
            queue.clear()
            val ready = connect(next)
            switching = false
            if (ready) prefs.setEngine(next)
            done(ready)
        }
    }
}
