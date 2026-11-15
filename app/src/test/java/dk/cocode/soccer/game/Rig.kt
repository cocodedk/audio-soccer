package dk.cocode.soccer.game

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
    state: TtsState = TtsState.READY,
    private val voices: Set<Lang> = setOf(Lang.EN, Lang.FA),
    private val engines: List<Engine> = listOf(ESPEAK, GOOGLE),
    var states: Map<String?, TtsState> = emptyMap(), // what a given engine reports; the rest report state
) {
    val speeches = mutableListOf<FakeSpeech>()
    val reader = FakeReader(readerOn)
    val haptics = FakeHaptics()
    val audio = FakeAudio()
    val sink = FakeSink()
    val prefs = FakePrefs(lang, savedEngine)
    val log = mutableListOf<String>()
    var now = 0L
    private val initial = state
    val session = Session(
        speech = { pkg ->
            FakeSpeech(pkg, states[pkg] ?: initial, voices, engines).also { speeches += it }
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

    // Starts level n from Home with the screen reader off.
    fun start(n: Int) = choose(en.levelNames[n - 1])

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
