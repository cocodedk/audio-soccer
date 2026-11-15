package dk.cocode.soccer.game

import android.util.Log
import dk.cocode.soccer.core.audio.SoundSink
import dk.cocode.soccer.core.input.MenuGestures
import dk.cocode.soccer.core.input.MenuInput
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.platform.Audio
import dk.cocode.soccer.platform.Haptics
import dk.cocode.soccer.platform.Prefs
import dk.cocode.soccer.platform.Reader
import dk.cocode.soccer.platform.Speech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MAX_FRAME = 0.1 // seconds: a longer frame (a stall) counts as this long
private const val DEFAULT_HEIGHT_DP = 640f

// The game as the screens see it. It joins the phone's side (platform/Ports.kt) to the game of :core and holds no Compose, so
// tests drive it with fakes. Stage.kt has the screen and its options, Run.kt the level being played, Round.kt what runs each
// frame of it, Learn.kt the sound tour, Talk.kt the speaking. All on the main thread.
// Screen reader on: the player touches Compose buttons (choose) and the game says nothing. Off: touches go through the
// recognizers (touch) and the game speaks the menus. Screen changes and game events go to the SoccerEvent log.
class Session(
    speech: (enginePkg: String?) -> Speech,
    private val reader: Reader,
    haptics: Haptics,
    private val audio: Audio,
    private val sink: SoundSink,
    prefs: Prefs,
    scope: CoroutineScope,
    private val nowMs: () -> Long,
    rand: Random,
    log: (String) -> Unit = { Log.d("SoccerEvent", it) },
) {
    private var visible = true // false while the app is in the background
    private val talk = Talk(speech, prefs, scope, nowMs) { !visible || reader.on.value }
    private var learn: Learn? = null
    private var heightDp = DEFAULT_HEIGHT_DP
    private val menuGestures = MenuGestures({ stage.options.size }, { heightDp })

    private val choices: Choices = object : Choices {
        override fun startLevel(n: Int) = run.start(n)
        override fun open(screen: Screen) {
            stage.show(screen)
            stage.announce()
        }
        override fun home() = this@Session.home()
        override fun resume() = run.resume()
        override fun giveUp() = run.giveUp()

        override fun hearAgain() {
            (stage.current as? Screen.Summary)?.let { talk.say(optionLine(it.result.summary)) }
        }

        override fun nextLanguage() {
            talk.nextLanguage()
            saySetting()
        }

        override fun nextEngine() = talk.nextEngine { up ->
            if (!up) stage.show(Screen.NoEngine) else if (stage.current == Screen.Settings) saySetting()
        }
    }

    private val stage: Stage = Stage(talk, choices, log, ::screenChanged)
    private val run: Run = Run(stage, talk, reader, rand, nowMs, sink, haptics, log)

    val screen: StateFlow<Screen> get() = stage.screen

    // The options of the screen, the one the cursor is on, and the text shown above them.
    val items: StateFlow<List<String>> get() = stage.items
    val focus: StateFlow<Int> get() = stage.focus
    val body: StateFlow<List<String>> get() = stage.body
    val lastLine: StateFlow<String> get() = talk.lastLine
    val pitch: StateFlow<Pitch?> get() = run.pitch
    val screenReaderOn: StateFlow<Boolean> get() = reader.on

    init {
        stage.refresh()
        scope.launch { start() }
        scope.launch { reader.on.drop(1).collect(::readerChanged) }
    }

    private suspend fun start() {
        if (!talk.start()) return stage.show(Screen.NoEngine)
        stage.ready = true
        stage.refresh()
        sayEnglishOnly()
        stage.announce()
    }

    // A setting was stepped: the option, now with its new value and in the new language, then the notice if Persian was not had.
    private fun saySetting() {
        stage.refresh()
        stage.sayOption(stage.focus.value)
        sayEnglishOnly()
    }

    private fun sayEnglishOnly() {
        if (talk.fellBack) talk.say(menuLine(talk.ui.englishOnly))
    }

    // The height of the screen the touches come from, in dp: the menu's bands are equal parts of it.
    fun resize(heightDp: Float) {
        this.heightDp = heightDp
    }

    // The app is going away: the speech engine is let go.
    fun close() = talk.close()

    // A finger's event, in dp, stamped with the event's own time (the clock of nowMs).
    fun touch(phase: Phase, t: Touch) {
        if (!stage.ready || reader.on.value) return
        when (stage.current) {
            is Screen.Play -> run.touch(phase, t)
            Screen.Learn -> if (phase == Phase.DOWN) home()
            else -> menuGestures.on(phase, t)?.let(stage::menu)
        }
    }

    fun menu(input: MenuInput) = stage.menu(input)

    fun choose(index: Int) = stage.choose(index)

    fun play(input: PlayInput) = run.play(input)

    fun steer(dir: Vec) = run.steer(dir)

    // The system's back: true if the session took it, false on a screen with nowhere to go back to.
    fun back(): Boolean = when (stage.current) {
        Screen.Home, Screen.NoEngine -> false
        is Screen.Play -> true.also { run.pause() }
        else -> true.also { home() }
    }

    // The app came to the front or went to the background (a call, the home button): a round pauses and nothing sounds.
    fun foreground(on: Boolean) {
        visible = on
        if (!on) interrupt() else if (stage.current is Screen.Paused) stage.announce()
    }

    // Something takes the player away from the game (the background, a screen reader coming on): it is silenced, a round
    // pauses and the sound tour ends.
    private fun interrupt() {
        talk.clear()
        run.pause()
        if (stage.current == Screen.Learn) stage.show(Screen.Home)
    }

    // One frame of dtSec seconds: the speech queue, then what the screen has running.
    fun frame(dtSec: Double) {
        talk.queue.tick()
        val dt = dtSec.coerceIn(0.0, MAX_FRAME)
        when (stage.current) {
            is Screen.Play -> run.frame(dt)
            Screen.Learn -> learn?.update(dt)
            else -> if (!reader.on.value) menuGestures.tick(nowMs())?.let(stage::menu)
        }
    }

    private fun home() {
        run.end()
        talk.clear()
        stage.show(Screen.Home)
        talk.say(menuLine(talk.ui.backToMenu))
    }

    private fun readerChanged(on: Boolean) {
        val s = stage.current
        when {
            on -> interrupt()
            s is Screen.HandOver -> run.handOver(s.level)
            else -> stage.announce()
        }
    }

    // The recognizers forget their fingers, so one still down does not act on the new screen, and the audio runs only on a
    // screen that makes sound.
    private fun screenChanged(before: Screen, now: Screen) {
        menuGestures.reset()
        run.reset()
        if (before.makesSound != now.makesSound) {
            sink.silence()
            if (now.makesSound) audio.start() else audio.stop()
        }
        learn = if (now == Screen.Learn) startTour() else null
    }

    private fun startTour(): Learn {
        talk.clear()
        return Learn(talk.strings, talk::say, talk.queue::isSaying, sink, reader.on::value).also { it.start() }
    }
}
