package dk.cocode.soccer.core.sim

// One record per kick in levels 2 and 3, from prototype/js/kicks.js, where the trial's report used it to
// answer: does the player line up with the goal by ear? The app has no report, so nothing reads the records
// yet. Pure: the game feeds it world events and the direction held.

// Left or Right held while kicking (it angles the kick), or neither.
enum class Aim { LEFT, RIGHT, STRAIGHT }

enum class Outcome { GOAL, SAVED, POST, WIDE_LEFT, WIDE_RIGHT, SHORT }

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

// At the end of a round, a kick that never reached the line stopped short of it. Returns new records; the
// ones passed in are not changed.
fun closeKicks(kicks: List<KickRecord>): List<KickRecord> = kicks.map { k -> k.copy(outcome = k.outcome ?: Outcome.SHORT) }
