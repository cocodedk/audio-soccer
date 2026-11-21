package dk.cocode.soccer.core.sim

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.tan

// Pure ball physics, from prototype/js/ball.js: friction, side boards, end walls, goal, posts and
// keeper saves. The ball's state lives in World (ball, ballVel, shot).

// Where a ball at (x, y) kicked at `angle` (radians, positive toward the player's right) crosses x = 40.
fun crossingY(x: Double, y: Double, angle: Double): Double = y + tan(angle) * (PITCH_L - x)

// Aim help takes a line that crosses x = 40 within the mouth +- AIM_MARGIN (y 7.5 to 12.5) into the goal.
const val AIM_LOW = POST_LEFT - AIM_MARGIN
const val AIM_HIGH = POST_RIGHT + AIM_MARGIN

fun aimedIn(y: Double): Boolean = y >= AIM_LOW && y <= AIM_HIGH

// Which way the goal is from a ball at height y: AHEAD when a straight kick goes in, else LEFT or RIGHT.
enum class GoalSide { AHEAD, LEFT, RIGHT }

fun goalSide(y: Double): GoalSide = if (aimedIn(y)) GoalSide.AHEAD else if (y > GOAL.y) GoalSide.LEFT else GoalSide.RIGHT

// Rolling deceleration at a given speed (m/s^2): friction, plus extra drag at low speed.
fun decelAt(speed: Double): Double = FRICTION + SLOW_DRAG * max(0.0, 1 - speed / SLOW_DRAG_UNTIL)

// Metres a ball kicked at v0 rolls before it slows to vMin: the same law moveBall applies.
fun rollDistance(v0: Double, vMin: Double = 0.0): Double {
    var v = v0
    var d = 0.0
    val dt = 0.001
    while (v > vMin) {
        d += v * dt
        v -= decelAt(v) * dt
    }
    return d
}

// What happens to a ball crossing the line x = 40 at height y.
enum class LineOutcome { POST, GOAL, WALL }

fun lineOutcome(y: Double): LineOutcome = when {
    abs(y - POST_LEFT) <= POST_TOLERANCE || abs(y - POST_RIGHT) <= POST_TOLERANCE -> LineOutcome.POST
    y > POST_LEFT && y < POST_RIGHT -> LineOutcome.GOAL
    else -> LineOutcome.WALL
}

private fun sideBoards(w: World, speed: Double, events: MutableList<Event>) {
    val b = w.ball
    val v = w.ballVel
    if (b.y < 0) {
        w.ball = Vec(b.x, -b.y)
        w.ballVel = Vec(v.x, -v.y * BALL_BOUNCE)
        events.add(Event.Board(Side.LEFT, b.x, 0.0, speed))
    } else if (b.y > PITCH_W) {
        w.ball = Vec(b.x, 2 * PITCH_W - b.y)
        w.ballVel = Vec(v.x, -v.y * BALL_BOUNCE)
        events.add(Event.Board(Side.RIGHT, b.x, PITCH_W, speed))
    }
}

// The ball crossed x = 40 this step, coming from `prev`: it is a post, a save, a goal or the end wall.
private fun goalLine(w: World, prev: Vec, speed: Double, events: MutableList<Event>) {
    val b = w.ball
    val v = w.ballVel
    val u = (PITCH_L - prev.x) / (b.x - prev.x)
    val y = prev.y + (b.y - prev.y) * u
    val shot = w.shot
    val keeper = w.keeper
    w.shot = false
    when (lineOutcome(y)) {
        LineOutcome.POST -> {
            w.ball = Vec(2 * PITCH_L - b.x, b.y)
            w.ballVel = Vec(-v.x * POST_BOUNCE, v.y)
            events.add(Event.Post(PITCH_L, if (y < GOAL.y) POST_LEFT else POST_RIGHT, speed))
        }
        LineOutcome.GOAL ->
            if (keeper != null && abs(keeper.pos.y - y) <= KEEPER_REACH) {
                w.ball = Vec(KEEPER_X - 1.5, y)
                w.ballVel = Vec(-abs(v.x) * SAVE_BOUNCE, v.y * 0.3 + (y - keeper.pos.y) * 2)
                events.add(Event.Save(PITCH_L, y, speed))
            } else {
                w.ball = Vec(PITCH_L + 0.6, y)
                w.ballVel = Vec.ZERO
                w.scored = true
                events.add(Event.Goal(PITCH_L, y, speed))
            }
        LineOutcome.WALL -> {
            w.ball = Vec(2 * PITCH_L - b.x, b.y)
            w.ballVel = Vec(-v.x * BALL_BOUNCE, v.y)
            val wide = if (!shot) null else if (y < GOAL.y) Side.LEFT else Side.RIGHT
            events.add(Event.EndWall(PITCH_L, y, speed, wide))
        }
    }
}

fun moveBall(w: World, dt: Double, events: MutableList<Event>) {
    val v = w.ballVel
    val speed = hypot(v.x, v.y)
    if (speed < STOP_SPEED) {
        w.ballVel = Vec.ZERO
        w.shot = false
        return
    }
    val f = max(0.0, speed - decelAt(speed) * dt) / speed
    val slowed = Vec(v.x * f, v.y * f)
    val prev = w.ball
    w.ballVel = slowed
    w.ball = Vec(prev.x + slowed.x * dt, prev.y + slowed.y * dt)
    sideBoards(w, speed, events)
    val b = w.ball
    if (b.x < 0) {
        w.ball = Vec(-b.x, b.y)
        w.ballVel = Vec(-w.ballVel.x * BALL_BOUNCE, w.ballVel.y)
        events.add(Event.EndWall(0.0, b.y, speed, null))
    } else if (b.x > PITCH_L && prev.x <= PITCH_L) {
        goalLine(w, prev, speed, events)
    }
}
