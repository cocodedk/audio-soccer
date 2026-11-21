package dk.cocode.soccer.game

import android.util.Log
import dk.cocode.soccer.core.audio.SoundSink
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MAX_FRAME = 0.1 // seconds: a longer frame (a stall) counts as this long

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
    private val talk = Talk(speech, prefs, scope, nowMs, log) { !visible || reader.on.value }
    private var learn: Learn? = null

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
            stage.saySetting()
        }

        override fun nextEngine() = talk.nextEngine { up ->
            if (!up) stage.noEngine() else if (stage.current == Screen.Settings) stage.saySetting()
        }
    }

    private val book = RecordBook(prefs, scope, talk::save)
    private val stage: Stage = Stage(talk, choices, book::records, log, ::screenChanged)
    private val run: Run = Run(stage, talk, reader, rand, nowMs, sink, haptics, log, book)
    private val fingers = Fingers(stage, run, reader, nowMs, ::home)
    private val mutableAwake = MutableStateFlow(false)

    val screen: StateFlow<Screen> get() = stage.screen

    // The options of the screen, the one the cursor is on, and the text shown above them.
    val items: StateFlow<List<String>> get() = stage.items
    val focus: StateFlow<Int> get() = stage.focus
    val body: StateFlow<List<String>> get() = stage.body
    val lastLine: StateFlow<String> get() = talk.lastLine
    val pitch: StateFlow<Pitch?> get() = run.pitch
    val screenReaderOn: StateFlow<Boolean> get() = reader.on

    // Whether the screen is to stay on: a round is on, or a screen that talks by itself is being listened to (the sound tour, the
    // how-to-play lines being read), where a screen gone dark would take the app to the background.
    val awake: StateFlow<Boolean> = mutableAwake.asStateFlow()

    init {
        stage.refresh()
        book.load(stage::refresh)
        stage.connect()
        scope.launch { reader.on.drop(1).collect(::readerChanged) }
    }

    // The height of the screen the touches come from, in dp: the menu's bands are equal parts of it.
    fun resize(heightDp: Float) = fingers.resize(heightDp)

    // The app is going away: the speech engine is let go.
    fun close() = talk.close()

    // A finger's event, in dp, stamped with the event's own time (the clock of nowMs).
    fun touch(phase: Phase, t: Touch) = fingers.touch(phase, t)

    fun menu(input: MenuInput) = stage.menu(input)

    fun choose(index: Int) = stage.choose(index)

    fun play(input: PlayInput) = run.play(input)

    fun steer(dir: Vec) = run.steer(dir)

    // The system's back: true if the session took it, false on a screen with nowhere to go back to. On Home it leaves the app only
    // for a screen reader's player: a self-voicing one drags in from the edge, the back gesture, and must not lose the app unheard.
    fun back(): Boolean = when (stage.current) {
        Screen.Home -> !reader.on.value
        Screen.NoEngine -> false
        is Screen.Play -> true.also { run.pause() }
        is Screen.Paused -> true.also { run.resume() }
        else -> true.also { home() }
    }

    // The app came to the front or went to the background (a call, the home button): a round pauses and nothing sounds. Coming
    // back says where the player is, or, with no engine speaking, tries the engines again.
    fun foreground(on: Boolean) {
        visible = on
        when {
            !on -> interrupt()
            stage.current == Screen.NoEngine -> stage.connect()
            else -> stage.welcomeBack()
        }
    }

    // Something takes the player away from the game (the background, a screen reader coming on): it is silenced, a round
    // pauses and the sound tour ends. A finger down now may never be reported lifting, so the recognizers drop theirs.
    private fun interrupt() {
        talk.clear()
        fingers.reset()
        run.pause()
        if (stage.current == Screen.Learn) stage.show(Screen.Home)
    }

    // One frame of dtSec seconds: the speech queue, then what the screen has running.
    fun frame(dtSec: Double) {
        talk.tick()
        val dt = dtSec.coerceIn(0.0, MAX_FRAME)
        when (stage.current) {
            is Screen.Play -> run.frame(dt)
            Screen.Learn -> if (learn?.update(dt) == true) stage.show(Screen.Home) // the tour is over
            else -> fingers.tick()
        }
        val s = stage.current
        mutableAwake.value = s.makesSound || (s == Screen.Help && talk.queue.isSaying(HELP_KEY))
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

    // The audio runs only on a screen that makes sound.
    private fun screenChanged(before: Screen, now: Screen) {
        fingers.reset()
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
