package dk.cocode.soccer.core.text

import dk.cocode.soccer.core.sim.AIM_HIGH
import dk.cocode.soccer.core.sim.AIM_LOW
import dk.cocode.soccer.core.sim.DRIBBLE_DIST
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.GoalSide
import dk.cocode.soccer.core.sim.KICK_ANGLE
import dk.cocode.soccer.core.sim.KICK_SPEED
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.TAKE_MAX_SPEED
import dk.cocode.soccer.core.sim.THIRD
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.aimedIn
import dk.cocode.soccer.core.sim.crossingY
import dk.cocode.soccer.core.sim.dist
import dk.cocode.soccer.core.sim.goalSide
import dk.cocode.soccer.core.sim.rollDistance
import dk.cocode.soccer.core.sim.roundHalfUp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

// Pure text descriptions: clock positions, thirds, where-am-I, the goal guide. From prototype/js/describe.js. No audio.
// The words come from GameStrings, so every function takes the strings of the language to speak.

// How far a kick reaches the goal line with the speed of a real shot (it slows to a pass below TAKE_MAX_SPEED):
// about 24 m. Further out the goal is too far to shoot at.
val SHOT_RANGE = rollDistance(KICK_SPEED, TAKE_MAX_SPEED)

// 12 o'clock = toward the goal you attack (+x), 3 = your right (+y), 6 = behind, 9 = your left.
fun clockHour(ahead: Double, right: Double): Int {
    val h = roundHalfUp(atan2(right, ahead) / (PI / 6)).toInt()
    val m = ((h % 12) + 12) % 12
    return if (m == 0) 12 else m
}

fun metresText(d: Double, s: GameStrings): String = s.metres(roundHalfUp(d).toInt())

// A relative offset (metres ahead, metres to the right) as "2 o'clock, 3 metres", or "at your feet" (within
// DRIBBLE_DIST, the distance at which a free ball is taken).
fun clockText(ahead: Double, right: Double, s: GameStrings): String {
    val d = hypot(ahead, right)
    return if (d < DRIBBLE_DIST) s.clockFeet else s.clockAt(clockHour(ahead, right), metresText(d, s))
}

fun clockTo(from: Vec, to: Vec, s: GameStrings): String = clockText(to.x - from.x, to.y - from.y, s)

fun thirdIndex(x: Double): Int = if (x < THIRD) 0 else if (x < 2 * THIRD) 1 else 2

fun thirdName(i: Int, s: GameStrings): String = s.thirds[i]

fun sideText(y: Double, s: GameStrings): String = when {
    y < 3 -> s.sideLeft
    y > 17 -> s.sideRight
    else -> s.sideCentre
}

// "Middle third, by the left board. Ball 2 o'clock, 3 metres. Goal, 23 metres, slightly right."
// The goal part is guideText: it names the direction in words, which is what lining up needs.
fun whereText(w: World, s: GameStrings): String {
    val p = w.player
    val from = if (w.hasBall) w.ball else p // a kick leaves from the ball, which can sit 2 m to one side
    return listOfNotNull(
        s.wherePlace(thirdName(thirdIndex(p.x), s), sideText(p.y, s)),
        s.whereBall(clockTo(p, w.ball, s)),
        s.whereGuide(guideText(p, s, from)),
        w.keeper?.let { s.whereKeeper(clockTo(p, it.pos, s)) },
    ).joinToString(" ")
}

// What the goal part of W says, judged by the kick itself: "straight ahead" only when a straight kick would score
// (with aim help), "slightly left/right" only when the 15 degree kick toward the goal would, otherwise the side and
// how many metres to move. Beyond SHOT_RANGE: too far to shoot.
//   "Goal, 9 metres, straight ahead" / "Goal, 23 metres, slightly right" /
//   "Goal, 10 metres, move 6 metres right" / "Goal, 30 metres, too far to shoot"
// p is where the player stands (the distance they hear); from is where a kick would leave (the ball).
fun guideText(p: Vec, s: GameStrings, from: Vec = p): String {
    val head = s.guideHead(metresText(dist(p, GOAL), s))
    if (PITCH_L - from.x > SHOT_RANGE) return s.guideFar(head)
    val way = goalSide(from.y)
    if (way == GoalSide.AHEAD) return s.guideAhead(head)
    val left = way == GoalSide.LEFT
    val side = if (left) s.dirLeft else s.dirRight
    if (aimedIn(crossingY(from.x, from.y, if (left) -KICK_ANGLE else KICK_ANGLE))) return s.guideSlightly(head, side)
    // to the nearest line a straight kick would score from
    val move = if (from.y > AIM_HIGH) from.y - AIM_HIGH else AIM_LOW - from.y
    return s.guideMove(head, metresText(max(1.0, move), s), side)
}
