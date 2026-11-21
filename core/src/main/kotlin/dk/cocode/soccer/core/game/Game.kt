package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.ROUND_CAP
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.kickRecord
import dk.cocode.soccer.core.sim.settleKick
import dk.cocode.soccer.core.sim.step
import dk.cocode.soccer.core.sim.tenths
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.GameStrings
import dk.cocode.soccer.core.text.whereText
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

private const val SUB_STEP = 1.0 / 120 // physics sub-step, seconds
private const val BETWEEN_ROUNDS = 3.0 // pause between rounds, seconds
private const val MAX_FRAME = 0.05 // a longer frame (a stall) is stepped as this long, so the physics never skips
private const val LONG_WAIT_MS = 20000L // the level summary may wait this long behind other lines

// What the player can do in a round. (The trial's H, help, has no phone gesture.)
enum class Action { KICK, WHERE, GIVE_UP }

// IDLE: no level, or one that is finished. PLAY: a round. BETWEEN: the pause between rounds. PAUSED: paused from
// PLAY or BETWEEN.
enum class Phase { IDLE, PLAY, BETWEEN, PAUSED }

// One play session, from prototype/js/game.js: level and round flow, the world step, and the world events turned into
// stats and spoken lines. Sound, cues and drawing are the shell's: update() hands back the events of the frame.
// The shell also owns the speech queue: it clears it (and stops the sounds) before startLevel, which speaks the round
// intro, and when it pauses, as the trial's game.js did with cancel().
class Game(
    private val rand: Random,
    private val strings: GameStrings,
    private val say: (Line) -> Unit,
    private val onLevelDone: (LevelResult) -> Unit,
    private val records: Records, // the player's, as the level starts
    private val onWon: () -> Unit, // a round was won: the shell plays the fanfare
) {
    var level = 0
        private set
    var round = 0
        private set
    var phase = Phase.IDLE
        private set
    var world: World? = null
        private set

    // Playing: a round is on, or the pause between rounds runs down.
    val running: Boolean get() = phase == Phase.PLAY || phase == Phase.BETWEEN
    val time: Double get() = stats.time

    // The round in play (its counters are live), and the rounds of this level closed so far.
    internal var stats = RoundStats(startDist = 0.0, startBehind = false) // a blank one until the first round
        private set
    private val closed = mutableListOf<RoundStats>()
    internal val rounds: List<RoundStats> get() = closed

    private var wait = 0.0
    private var resumeTo = Phase.PLAY
    private var kickQueued = false
    private var clock = false // the round clock starts at the first arrow press (or kick)

    private fun side(s: Side) = if (s == Side.LEFT) strings.dirLeft else strings.dirRight

    private fun whereLine() = strings.whereRound(round, ROUNDS, whereText(checkNotNull(world), strings))

    private fun nextRound() {
        round += 1
        val setup = setupRound(level, rand)
        world = setup.toWorld()
        stats = newStats(setup)
        phase = Phase.PLAY
        clock = false
        kickQueued = false
        say(Line(startLine(level, round, strings), Priority.HIGH))
    }

    fun startLevel(n: Int) {
        level = n
        round = 0
        closed.clear()
        nextRound()
    }

    private fun endRound(ending: Ending, text: String) {
        closed += closeStats(level, stats, ending)
        phase = Phase.BETWEEN
        wait = BETWEEN_ROUNDS
        if (ending == Ending.SUCCESS) onWon()
        say(Line(text, Priority.HIGH, cut = true))
    }

    private fun finishLevel() {
        phase = Phase.IDLE
        val result = levelResult(level, closed.toList(), records, strings)
        say(Line(result.summary, Priority.HIGH, waitMs = LONG_WAIT_MS))
        onLevelDone(result)
    }

    // A ball hitting the end wall in front of the goal (levels 2 and 3): a shot is counted and called wide; a dribbled
    // one is called too, at a lower rate.
    private fun endWall(e: Event.EndWall) {
        if (level < 2 || e.x != PITCH_L) return
        if (e.wide != null) {
            stats.wide += 1
            say(Line(strings.wide(side(e.wide)), key = "wide", gapMs = 1000))
        } else {
            say(Line(strings.wideGoal(side(if (e.y < GOAL.y) Side.LEFT else Side.RIGHT)), key = "wide", gapMs = 2500))
        }
    }

    // dir: the direction held this step (a kick records whether it angled left or right).
    private fun onEvent(e: Event, dir: Vec) {
        settleKick(stats.shots, e)
        when (e) {
            is Event.Take ->
                if (levelSpec(level).endsOn == End.TAKE) endRound(Ending.SUCCESS, strings.gotIt(tenths(stats.time)))
                else say(Line(strings.gotBall, key = "got", gapMs = 1500))
            is Event.Kick -> {
                stats.kicks += 1
                stats.shots += kickRecord(e, dir)
            }
            is Event.Bump -> stats.bumps += 1
            is Event.Goal -> endRound(Ending.SUCCESS, strings.goal)
            is Event.Save -> {
                stats.saves += 1
                say(Line(strings.saved, Priority.HIGH, cut = true, key = "saved", gapMs = 1500))
            }
            is Event.Post -> say(Line(strings.post, key = "post", gapMs = 800))
            is Event.EndWall -> endWall(e)
            is Event.Voy -> say(Line(strings.voy, pitch = 1.4f))
            is Event.NoKick -> say(Line(strings.noKick, Priority.LOW, key = "nokick", gapMs = 2000))
            else -> Unit
        }
    }

    private fun play(dt: Double, dir: Vec, events: MutableList<Event>) {
        val w = checkNotNull(world)
        if (!clock && (dir.x != 0.0 || dir.y != 0.0 || kickQueued)) clock = true
        if (clock) stats.time += dt
        val n = max(1, ceil(dt / SUB_STEP).toInt())
        val h = dt / n
        val kick = kickQueued
        kickQueued = false
        for (i in 0 until n) {
            for (e in step(w, h, dir, kick && i == 0)) {
                events += e
                onEvent(e, dir)
                if (phase != Phase.PLAY) return
            }
        }
        if (stats.time >= ROUND_CAP) endRound(Ending.TIMEOUT, strings.timeUp)
    }

    // Advances the game by one frame of dt seconds with the direction dir held; returns the world events of the frame.
    fun update(dt: Double, dir: Vec): List<Event> {
        val events = mutableListOf<Event>()
        val frame = dt.coerceIn(0.0, MAX_FRAME)
        if (phase == Phase.PLAY) {
            play(frame, dir, events)
        } else if (phase == Phase.BETWEEN) {
            wait -= frame
            if (wait <= 0) {
                if (round < ROUNDS) nextRound() else finishLevel()
            }
        }
        return events
    }

    fun action(a: Action) {
        if (phase != Phase.PLAY) return
        when (a) {
            Action.KICK -> kickQueued = true
            Action.WHERE -> {
                stats.where += 1
                say(Line(whereLine(), Priority.HIGH, key = "where"))
            }
            Action.GIVE_UP -> endRound(Ending.GIVE_UP, strings.gaveUp)
        }
    }

    fun pause() {
        if (!running) return
        resumeTo = phase
        phase = Phase.PAUSED
    }

    fun resume() {
        if (phase != Phase.PAUSED) return
        phase = resumeTo
        say(Line(if (resumeTo == Phase.BETWEEN) strings.nextRound else whereLine(), Priority.HIGH))
    }
}
