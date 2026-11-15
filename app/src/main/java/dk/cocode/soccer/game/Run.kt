package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.SoundSink
import dk.cocode.soccer.core.game.LevelResult
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.platform.Haptics
import dk.cocode.soccer.platform.Reader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

private const val INTRO_KEY = "intro"
private const val INTRO_WAIT_MS = 60000L // how long an intro line may wait behind the one before it

// The level being played, from its start to its summary, and the screens that go with it: Play, Paused, HandOver (a level
// or a pause that waits for the screen reader to go off) and Summary. The first level a run starts is led in by the drag intro,
// spoken on the Play screen before the round exists: the level begins once its lines have ended, and a finger meanwhile does
// nothing, for there is no round to take it.
internal class Run(
    private val stage: Stage,
    private val talk: Talk,
    private val reader: Reader,
    private val rand: Random,
    private val nowMs: () -> Long,
    private val sink: SoundSink,
    private val haptics: Haptics,
    private val log: (String) -> Unit,
) {
    private var round: Round? = null
    private var introLevel: Int? = null // the level waiting for the intro to end
    private var introHeard = false
    private val mutablePitch = MutableStateFlow<Pitch?>(null)
    val pitch: StateFlow<Pitch?> = mutablePitch.asStateFlow() // while a round is on

    fun start(n: Int) {
        if (reader.on.value) return stage.show(Screen.HandOver(n))
        talk.clear()
        stage.show(Screen.Play(n))
        if (introHeard) return begin(n)
        introLevel = n
        talk.ui.dragIntro.forEach { talk.say(menuLine(it).copy(key = INTRO_KEY, many = true, waitMs = INTRO_WAIT_MS)) }
    }

    private fun begin(n: Int) {
        val r = Round(n, rand, talk, nowMs, sink, haptics, log, ::pause, ::done)
        round = r
        r.start()
        mutablePitch.value = r.pitch
    }

    private fun done(result: LevelResult) {
        round = null
        stage.show(Screen.Summary(result))
    }

    fun pause() {
        if (introLevel != null) return leaveIntro()
        val r = round ?: return
        if (stage.current !is Screen.Play) return
        r.pause()
        talk.clear()
        stage.show(Screen.Paused(r.level))
        stage.announce()
    }

    // Back or the background during the intro: the level never started, so there is nothing to pause. Back to the menu, and the
    // intro, not heard to the end, is said at the next start.
    private fun leaveIntro() {
        introLevel = null
        talk.clear()
        stage.show(Screen.Home)
        stage.announce()
    }

    // Goes on with the paused round, or the one just given up, once the player's screen reader is off.
    fun resume() {
        val r = round ?: return
        if (reader.on.value) return stage.show(Screen.HandOver(r.level))
        stage.show(Screen.Play(r.level))
        r.resume()
    }

    // The screen reader went off on the hand-over: go on with the paused round of that level, or start it.
    fun handOver(level: Int) {
        if (round?.level == level) resume() else start(level)
    }

    fun giveUp() {
        round?.giveUp()
        resume()
    }

    // The level is left behind, unfinished.
    fun end() {
        round = null
        introLevel = null
    }

    fun touch(phase: Phase, t: Touch) {
        round?.touch(phase, t)
    }

    fun play(input: PlayInput) {
        round?.play(input)
    }

    fun steer(dir: Vec) {
        round?.steer(dir)
    }

    fun reset() {
        round?.reset()
    }

    // The intro is over once its lines are: the level begins.
    private fun waitForIntro(n: Int) {
        if (talk.queue.isSaying(INTRO_KEY)) return
        introLevel = null
        introHeard = true
        begin(n)
    }

    fun frame(dt: Double) {
        introLevel?.let { return waitForIntro(it) }
        val r = round ?: return
        r.frame(dt)
        mutablePitch.value = r.pitch
    }
}
