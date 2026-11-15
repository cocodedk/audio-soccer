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

// The level being played, from its start to its summary, and the screens that go with it: Play, Paused, HandOver (a level
// or a pause that waits for the screen reader to go off) and Summary.
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
    private val mutablePitch = MutableStateFlow<Pitch?>(null)
    val pitch: StateFlow<Pitch?> = mutablePitch.asStateFlow() // while a round is on

    fun start(n: Int) {
        if (reader.on.value) return stage.show(Screen.HandOver(n))
        talk.clear()
        val r = Round(n, rand, talk, nowMs, sink, haptics, log, ::pause, ::done)
        round = r
        stage.show(Screen.Play(n))
        r.start()
        mutablePitch.value = r.pitch
    }

    private fun done(result: LevelResult) {
        round = null
        stage.show(Screen.Summary(result))
    }

    fun pause() {
        val r = round ?: return
        if (stage.current !is Screen.Play) return
        r.pause()
        talk.clear()
        stage.show(Screen.Paused(r.level))
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

    fun frame(dt: Double) {
        val r = round ?: return
        r.frame(dt)
        mutablePitch.value = r.pitch
    }
}
