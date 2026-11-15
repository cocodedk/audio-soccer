package dk.cocode.soccer.core.sim

import kotlin.math.abs

// One record per kick in levels 2 and 3, so the report can answer the trial's question: does the
// player line up with the goal by ear? From prototype/js/kicks.js. Pure: the game feeds it world
// events and the direction held.

// Left or Right held while kicking (it angles the kick), or neither.
enum class Aim(val text: String) { LEFT("left"), RIGHT("right"), STRAIGHT("straight") }

enum class Outcome(val text: String) {
    GOAL("goal"), SAVED("saved"), POST("post"), WIDE_LEFT("wide left"), WIDE_RIGHT("wide right"),
    SHORT("short"), UNFINISHED("unfinished"),
}

// dist: metres to the goal line at the kick; offset: the ball's sideways offset from the goal centre
// (left is negative, as y grows to the player's right); outcome: null until the kick is settled.
data class KickRecord(val dist: Double, val offset: Double, val aim: Aim, var outcome: Outcome? = null)

fun kickRecord(e: Event.Kick, dir: Vec): KickRecord =
    KickRecord(PITCH_L - e.x, e.y - GOAL.y, if (dir.y < 0) Aim.LEFT else if (dir.y > 0) Aim.RIGHT else Aim.STRAIGHT)

// A world event settles the latest kick still unsettled; call it before recording a new kick. Only the
// first event counts: a rebound off a post or the keeper that later reaches the line is not that kick.
// A new kick before the last one reached the line, or the player taking the ball back first, means
// the last one stopped short: a ball then dribbled into the goal, a post or the keeper is not that kick.
fun settleKick(kicks: List<KickRecord>, e: Event) {
    val k = kicks.lastOrNull() ?: return
    if (k.outcome != null) return
    k.outcome = when (e) {
        is Event.Goal -> Outcome.GOAL
        is Event.Save -> Outcome.SAVED
        is Event.Post -> Outcome.POST
        is Event.EndWall -> when (e.wide) {
            Side.LEFT -> Outcome.WIDE_LEFT
            Side.RIGHT -> Outcome.WIDE_RIGHT
            else -> null
        }
        is Event.Kick, is Event.Take -> Outcome.SHORT
        else -> null
    }
}

// At the end of a round, a kick that never reached the line stopped short of it, unless the round was
// left with the ball still rolling (`left`). Returns new records; the ones passed in are not changed.
fun closeKicks(kicks: List<KickRecord>, left: Boolean): List<KickRecord> =
    kicks.map { k -> k.copy(outcome = k.outcome ?: if (left) Outcome.UNFINISHED else Outcome.SHORT) }

// "18 m, 2.1 m right, straight, wide right"
fun kickText(k: KickRecord): String {
    val off = if (abs(k.offset) < 0.05) "centre" else "${tenths(abs(k.offset))} m ${if (k.offset < 0) "left" else "right"}"
    return "${roundHalfUp(k.dist).toInt()} m, $off, ${k.aim.text}, ${k.outcome?.text ?: "unfinished"}"
}
