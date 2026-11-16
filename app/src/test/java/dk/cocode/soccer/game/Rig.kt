package dk.cocode.soccer.game

import dk.cocode.soccer.core.game.Records
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.dist
import dk.cocode.soccer.core.text.GameStrings
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.UiStrings
import dk.cocode.soccer.core.text.gameStrings
import dk.cocode.soccer.core.text.uiStrings
import dk.cocode.soccer.platform.Engine
import dk.cocode.soccer.platform.TtsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random

// A session on fakes, with the clock in the test's hands. The scope is Unconfined, so what a session launches runs at once.
class Rig(
    lang: Lang = Lang.EN,
    savedEngine: String? = null,
    readerOn: Boolean = false,
    var state: TtsState = TtsState.READY, // what the engines not in states report
    private val voices: Set<Lang> = setOf(Lang.EN, Lang.FA),
    private val engines: List<Engine> = listOf(ESPEAK, GOOGLE),
    private val defaultEngine: String? = engines.firstOrNull()?.pkg, // what the system's default stands for; null: it has none
    var states: Map<String?, TtsState> = emptyMap(), // what a given engine reports; null is the system's default
    failWrites: Boolean = false, // the choices cannot be written
    records: Records = Records(), // the records kept on the phone
) {
    val speeches = mutableListOf<FakeSpeech>()
    val reader = FakeReader(readerOn)
    val haptics = FakeHaptics()
    val audio = FakeAudio()
    val sink = FakeSink()
    val prefs = FakePrefs(lang, savedEngine, failWrites, records)
    val log = mutableListOf<String>()
    var now = 0L
    val session = Session(
        speech = { pkg ->
            FakeSpeech(pkg, states[pkg] ?: state, voices, engines, defaultEngine).also { speeches += it }
        },
        reader = reader,
        haptics = haptics,
        audio = audio,
        sink = sink,
        prefs = prefs,
        scope = CoroutineScope(Dispatchers.Unconfined),
        nowMs = { now },
        rand = Random(7),
        log = { log += it },
    )

    val speech get() = speeches.last()
    val spoken get() = speeches.flatMap { it.said }
    val screen get() = session.screen.value
    val items get() = session.items.value
    val player get() = session.pitch.value!!.player
    val en: UiStrings = uiStrings(Lang.EN)
    val fa: UiStrings = uiStrings(Lang.FA)
    val game: GameStrings = gameStrings(Lang.EN)

    fun frames(seconds: Double, dt: Double = 1.0 / 60) {
        var left = seconds
        while (left > 1e-9) {
            now += (dt * 1000).toLong()
            session.frame(dt)
            left -= dt
        }
    }

    fun choose(text: String) = session.choose(items.indexOf(text).also { check(it >= 0) { "no option $text in $items" } })

    // Starts level n from Home with the screen reader off. The first level of a run waits for the drag intro (see beginRound).
    fun start(n: Int) {
        choose(en.levelNames[n - 1])
        if (screen is Screen.Play && session.pitch.value == null) beginRound()
    }

    // The first level of a run begins with the frame after the drag intro has been said, which this engine does at once.
    fun beginRound() = session.frame(0.0)

    // The engine keeps each line until its release().
    fun holding() = apply { speech.hold = true }

    fun leaveAndReturn() {
        session.foreground(false)
        session.foreground(true)
    }

    fun touch(phase: Phase, id: Int, x: Float, y: Float) = session.touch(phase, Touch(id, x, y, now))

    fun tap(id: Int, x: Float, y: Float) {
        touch(Phase.DOWN, id, x, y)
        now += 50
        touch(Phase.UP, id, x, y)
        now += 50
    }

    // Two taps in a row: the touch-to-hear menu's double-tap.
    fun doubleTap(x: Float, y: Float) {
        tap(1, x, y)
        tap(2, x, y)
    }

    fun logged(prefix: String) = log.filter { it.startsWith(prefix) }

    fun pause() = session.play(PlayInput.PAUSE)

    // Steers straight at a point.
    fun steerToward(target: Vec) {
        val from = player
        val d = dist(from, target)
        session.steer(Vec((target.x - from.x) / d, (target.y - from.y) / d))
    }

    // Runs toward the ball until the round ends or ten seconds pass.
    fun runToBall() {
        repeat(600) {
            val p = session.pitch.value ?: return
            if (screen !is Screen.Play || dist(p.player, p.ball) < 0.5) return
            steerToward(p.ball)
            frames(1.0 / 60)
        }
    }
}
