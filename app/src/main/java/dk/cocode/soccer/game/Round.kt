package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.SoundSink
import dk.cocode.soccer.core.audio.Soundscape
import dk.cocode.soccer.core.cues.Cues
import dk.cocode.soccer.core.cues.LineUp
import dk.cocode.soccer.core.game.Action
import dk.cocode.soccer.core.game.Game
import dk.cocode.soccer.core.game.LevelResult
import dk.cocode.soccer.core.game.Phase
import dk.cocode.soccer.core.input.PlayGestures
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.core.input.Phase as TouchPhase
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.platform.Haptics
import java.util.Locale
import kotlin.random.Random

private const val POSITION_EVERY = 1.0 // seconds between the player's position in the log

// Where the pieces on the pitch are, for drawing: the world as the screen needs it.
data class Pitch(val player: Vec, val ball: Vec, val keeper: Vec?, val hasBall: Boolean)

// One level in play: the game, and what the shell adds around it each frame. The game's events go to the soundscape, the line-up
// and the log, the cues speak what they see, and the line-up buzzes the phone. Each of them is told at the start of a round
// what the round starts with. The caller clears the speech queue before start(), which speaks the first round's intro.
internal class Round(
    val level: Int,
    rand: Random,
    talk: Talk,
    private val nowMs: () -> Long,
    sink: SoundSink,
    private val haptics: Haptics,
    private val log: (String) -> Unit,
    private val onPause: () -> Unit,
    onDone: (LevelResult) -> Unit,
) {
    private val game = Game(rand, talk.strings, talk::say, onDone)
    private val cues = Cues(talk.strings, talk::say, talk.queue::isSaying, nowMs)
    private val soundscape = Soundscape(sink)
    private val lineUp = LineUp()
    private val gestures = PlayGestures()
    private var dir = Vec.ZERO
    private var world: World? = null
    private var sinceLog = 0.0

    val pitch: Pitch? get() = game.world?.let { Pitch(it.player, it.ball, it.keeper?.pos, it.hasBall) }

    fun start() {
        game.startLevel(level)
        newRound()
    }

    // A finger's event, in dp, on the clock of nowMs. The gestures steer and tap.
    fun touch(phase: TouchPhase, t: Touch) {
        gestures.on(phase, t)?.let(::play)
        dir = gestures.dir
    }

    fun play(input: PlayInput) {
        when (input) {
            PlayInput.KICK -> game.action(Action.KICK)
            PlayInput.WHERE -> game.action(Action.WHERE)
            PlayInput.PAUSE -> onPause()
        }
    }

    fun steer(dir: Vec) {
        this.dir = dir
    }

    // The screen changed: a finger still down must not act on the new one.
    fun reset() {
        gestures.reset()
        dir = Vec.ZERO
    }

    fun pause() = game.pause()

    // Goes on from a pause; says where the player is. The mixer was silenced, so the soundscape starts over.
    fun resume() {
        soundscape.reset(level)
        game.resume()
    }

    // Gives up the round in play, even from the pause menu.
    fun giveUp() {
        game.resume()
        game.action(Action.GIVE_UP)
    }

    fun frame(dt: Double) {
        gestures.tick(nowMs())?.let(::play)
        val playing = game.phase == Phase.PLAY
        val events = game.update(dt, dir)
        for (e in events) log("event ${e::class.simpleName}")
        val w = game.world
        if (playing && w != null) {
            soundscape.update(w, dt, events)
            lineUp.update(w, events)?.let(haptics::buzz)
            if (game.phase == Phase.PLAY) cues.update(w)
            logPosition(w, dt)
        }
        if (game.world !== world) newRound()
    }

    private fun newRound() {
        val w = checkNotNull(game.world)
        world = w
        sinceLog = 0.0
        soundscape.reset(level)
        cues.reset(w)
        lineUp.reset(w)
        reset() // a finger held through the gap between rounds is dropped, and the new round starts standing still
        log("round ${game.round} level=$level")
    }

    private fun logPosition(w: World, dt: Double) {
        sinceLog += dt
        if (sinceLog < POSITION_EVERY) return
        sinceLog -= POSITION_EVERY
        log(String.format(Locale.ROOT, "player x=%.1f y=%.1f", w.player.x, w.player.y))
    }
}
