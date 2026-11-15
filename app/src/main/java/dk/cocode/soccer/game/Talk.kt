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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val LOAD_LIMIT_MS = 5000L // an engine still starting after this long counts as one that will not

// Everything the game says: the speech engine in use, the language it speaks, and the queue in front of it. A line is
// kept as lastLine for the screen, and goes to the queue unless muted() (a screen reader is on, or the app is in the
// background). The engine is built from factory, which takes the package of the engine to use, null for the system's.
// nowMs is the clock of the queue and of the limit an engine is given to start; tick() counts it. Each time the engine or
// its language is set, log gets a "voice engine=<package> lang=<EN or FA>" line (the package is "system" if the phone does
// not say): what the end-to-end script reads.
internal class Talk(
    private val factory: (String?) -> Speech,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long,
    private val log: (String) -> Unit,
    private val muted: () -> Boolean,
) {
    private var speech: Speech? = null
    private var wantLang = Lang.EN // what the player chose
    private var switching = false
    private var deadline = Long.MAX_VALUE // when the engine being waited for is given up on
    private val late = MutableStateFlow(false) // the deadline has passed

    // The language the engine really speaks. It differs from wantLang when the engine has no voice for it.
    var lang = Lang.EN
        private set

    val strings: GameStrings get() = gameStrings(lang)
    val ui: UiStrings get() = uiStrings(lang)

    // The engine that speaks, by its package; null if the phone does not say.
    private val engine: String? get() = speech?.currentEngine()

    // The engines installed on the phone.
    private val installed: List<Engine> get() = speech?.engines().orEmpty()

    // The player asked for a language the engine cannot speak, so everything is English.
    val fellBack: Boolean get() = lang != wantLang

    // The engine that speaks, as the player knows it.
    val engineLabel: String
        get() {
            val engines = installed
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

    // Once a frame: the speech queue, and the limit an engine has to start.
    fun tick() {
        queue.tick()
        if (nowMs() >= deadline) late.value = true
    }

    fun close() {
        speech?.shutdown()
    }

    // Reads what the player chose and starts the engine. False: no engine speaks (yet: see awaitEngine).
    suspend fun start(): Boolean {
        wantLang = prefs.lang.first()
        return connect(prefs.enginePkg.first())
    }

    // Builds the engine, waits until it is up and tells it the language. An engine the player chose that will not start
    // (it was uninstalled, or is too slow) gives way to the system's, and the system's, if it will not start either, to the
    // first installed engine that does. An engine that gave way is kept as the choice: the one that speaks in the end.
    private suspend fun connect(pkg: String?): Boolean {
        val failed = mutableSetOf<String?>()
        // The system's engine counts as the package it stands for.
        suspend fun start(p: String?) = open(p).also { if (!it) failed += engine }
        var keep = pkg != null
        var up = start(pkg) || (pkg != null && start(null))
        if (!up) {
            // Android makes only a system engine the default, so a phone with none of its own, and eSpeak installed by the
            // player, has no engine to start until it is picked here.
            up = installed.any { it.pkg !in failed && start(it.pkg) }
            keep = true
        }
        if (!up) return false
        speakLanguage()
        if (keep) engine?.let { prefs.setEngine(it) }
        return true
    }

    // Starts an engine and waits for it to be READY, or for it to fail or to be too slow.
    private suspend fun open(pkg: String?): Boolean {
        speech?.shutdown()
        val s = factory(pkg)
        speech = s
        late.value = false
        deadline = nowMs() + LOAD_LIMIT_MS
        val state = combine(s.state, late) { st, over -> if (over && st == TtsState.LOADING) TtsState.NO_ENGINE else st }
            .first { it != TtsState.LOADING }
        deadline = Long.MAX_VALUE
        return state == TtsState.READY
    }

    private fun speakLanguage() {
        lang = checkNotNull(speech).useLanguage(wantLang)
        log("voice engine=${engine ?: "system"} lang=${lang.name}")
    }

    // After start() or a switch gave up on an engine: waits for it to settle. True if it comes up, and then it speaks.
    suspend fun awaitEngine(): Boolean {
        val s = speech ?: return false
        if (s.state.first { it != TtsState.LOADING } != TtsState.READY) return false
        speakLanguage()
        return true
    }

    // Where the engine that speaks is in the list; if the phone does not say, the system's, which Android ranks first.
    private fun current(engines: List<Engine>) = engines.indexOfFirst { it.pkg == engine }.coerceAtLeast(0)

    // The next language. The caller says what it is, in the new language.
    fun nextLanguage() {
        wantLang = Lang.entries[(wantLang.ordinal + 1) % Lang.entries.size]
        scope.launch { prefs.setLang(wantLang) }
        // Lines still queued are in the old language, and an engine changes its language only when idle: set mid-line, eSpeak
        // read the next line in the old voice (Persian text in its English one).
        queue.clear()
        speakLanguage()
    }

    // Switches to the next installed engine, and calls done(true) once it speaks, done(false) if none does. With one
    // engine only there is nothing to switch to.
    fun nextEngine(done: suspend (Boolean) -> Unit) {
        val engines = installed
        if (switching) return
        scope.launch {
            if (engines.size < 2) return@launch done(true)
            val next = engines[(current(engines) + 1) % engines.size].pkg
            switching = true
            queue.clear()
            val up = connect(next)
            switching = false
            done(up)
        }
    }
}
