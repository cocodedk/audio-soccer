package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.KickRecord
import dk.cocode.soccer.core.sim.PLAYER_SPEED
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.closeKicks
import dk.cocode.soccer.core.sim.createWorld
import dk.cocode.soccer.core.sim.dist
import dk.cocode.soccer.core.sim.tenths
import dk.cocode.soccer.core.text.GameStrings
import kotlin.random.Random

// Levels, round setup, per-round stats and the lines spoken at the start and end of a level, from
// prototype/js/levels.js. Not ported: the win word (it is for the trial's copied report). All but what the shell needs
// (ROUNDS and the result types) is internal to :core.

const val ROUNDS = 3

// What ends a round of a level: the player getting the ball, or a goal.
internal enum class End { TAKE, GOAL }

internal class LevelSpec(val keeper: Boolean, val endsOn: End)

private val LEVELS = listOf(
    LevelSpec(keeper = false, endsOn = End.TAKE),
    LevelSpec(keeper = false, endsOn = End.GOAL),
    LevelSpec(keeper = true, endsOn = End.GOAL),
)

internal fun levelSpec(level: Int): LevelSpec = LEVELS[level - 1]

internal class RoundSetup(val player: Vec, val ball: Vec, val hasBall: Boolean, val keeper: Boolean)

internal fun RoundSetup.toWorld(): World = createWorld(player, ball, hasBall, keeper)

// Starting positions for one round, drawn in the trial's order. Level 1: player and a still ball at least 10 m apart.
// Levels 2 and 3: the player starts with the ball somewhere in their own half.
internal fun setupRound(level: Int, rand: Random): RoundSetup {
    if (level == 1) {
        val player = Vec(rand.nextDouble(3.0, 37.0), rand.nextDouble(3.0, 17.0))
        var ball: Vec
        do {
            ball = Vec(rand.nextDouble(2.0, 38.0), rand.nextDouble(2.0, 18.0))
        } while (dist(ball, player) < 10)
        return RoundSetup(player, ball, hasBall = false, keeper = false)
    }
    val player = Vec(rand.nextDouble(4.0, 16.0), rand.nextDouble(4.0, 16.0))
    return RoundSetup(player, Vec(player.x + 0.5, player.y), hasBall = true, keeper = levelSpec(level).keeper)
}

// One round's record. startDist and startBehind describe where the ball was at the start (they matter in level 1;
// in levels 2 and 3 the player starts with the ball). shots: one record per kick. ending and efficiency are set when
// the round is closed (closeStats); anything but SUCCESS counts as not found / not scored.
data class RoundStats(
    val startDist: Double,
    val startBehind: Boolean,
    var time: Double = 0.0,
    var where: Int = 0,
    var bumps: Int = 0,
    var kicks: Int = 0,
    var wide: Int = 0,
    var saves: Int = 0,
    var shots: List<KickRecord> = emptyList(),
    val ending: Ending? = null,
    val efficiency: Double? = null,
) {
    val success: Boolean get() = ending == Ending.SUCCESS
}

// How a round ended: won, given up (G), or the time cap.
enum class Ending { SUCCESS, GIVE_UP, TIMEOUT }

// What a finished level hands the shell: its closed rounds and the summary line that was spoken.
data class LevelResult(val level: Int, val rounds: List<RoundStats>, val summary: String)

internal fun newStats(setup: RoundSetup) = RoundStats(dist(setup.ball, setup.player), setup.ball.x < setup.player.x)

// The closed record of a round, a copy: anything but SUCCESS counts as not found / not scored and keeps the elapsed
// time. Level 1 efficiency = startDist / (PLAYER_SPEED x time): 1 means a straight run at full speed.
internal fun closeStats(level: Int, s: RoundStats, ending: Ending): RoundStats = s.copy(
    ending = ending,
    shots = closeKicks(s.shots),
    efficiency = if (level == 1 && ending == Ending.SUCCESS && s.time > 0) s.startDist / (PLAYER_SPEED * s.time) else null,
)

// Spoken at the start of a round: the round and the task.
internal fun startLine(level: Int, round: Int, s: GameStrings): String = s.roundIntro(round, s.levelTask(level))

// "Level 1 done. Times: 12.4, not found, 10.2."
internal fun summarySpeech(level: Int, rounds: List<RoundStats>, s: GameStrings): String {
    val list = rounds.joinToString(s.summarySep) { if (it.success) tenths(it.time) else s.levelMiss(level) }
    return s.summaryTimes(s.summaryDone(level), list)
}
