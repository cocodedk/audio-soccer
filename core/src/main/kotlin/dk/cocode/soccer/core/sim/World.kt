package dk.cocode.soccer.core.sim

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

// PURE game world, from prototype/js/world.js: player and ball physics, possession, keeper, events.
// No Android, no audio.

// The keeper: he stands at KEEPER_X and slides along the goal line. `voyArmed` is false from the moment
// he shouts until the ball has gone away again, so he shouts once per approach.
class Keeper(var pos: Vec, var moving: Boolean = false, var voyArmed: Boolean = true)

class World(
    var player: Vec,
    var ball: Vec,
    var ballVel: Vec = Vec.ZERO,
    var hasBall: Boolean = false,
    var keeper: Keeper? = null,
) {
    var t = 0.0
    var playerMoving = false
    var playerTouching: Side? = null // the board the player is pressed against, so a bump is heard once
    var shot = false // the ball is a kicked shot, until it is saved, hits a post or the wall, or stops
    var lastTap = Double.NEGATIVE_INFINITY
    var lastTapDir: Vec? = null
    var scored = false
}

fun createWorld(player: Vec, ball: Vec, hasBall: Boolean = false, keeper: Boolean = false): World =
    World(player, ball, hasBall = hasBall, keeper = if (keeper) Keeper(Vec(KEEPER_X, GOAL.y)) else null)

fun movePlayer(w: World, dir: Vec, dt: Double, events: MutableList<Event>) {
    val p = w.player
    val r = PLAYER_RADIUS
    val nx = p.x + dir.x * PLAYER_SPEED * dt
    val ny = p.y + dir.y * PLAYER_SPEED * dt
    val x = clamp(nx, r, PITCH_L - r)
    val y = clamp(ny, r, PITCH_W - r)
    val bump = when {
        y != ny -> if (ny < y) Event.Bump(Side.LEFT, x, 0.0) else Event.Bump(Side.RIGHT, x, PITCH_W)
        x != nx -> Event.Bump(Side.END, if (nx < x) 0.0 else PITCH_L, y)
        else -> null
    }
    if (bump != null && bump.side != w.playerTouching) events.add(bump)
    w.playerTouching = bump?.side
    w.player = Vec(x, y)
    w.playerMoving = dir.x != 0.0 || dir.y != 0.0
}

private fun tap(w: World, dir: Vec, events: MutableList<Event>) {
    val p = w.player
    val b = Vec(clamp(p.x + dir.x * TAP_AHEAD, 0.1, PITCH_L - 0.1), clamp(p.y + dir.y * TAP_AHEAD, 0.1, PITCH_W - 0.1))
    w.ball = b
    w.ballVel = Vec(dir.x * TAP_SPEED, dir.y * TAP_SPEED)
    w.lastTap = w.t
    w.lastTapDir = dir
    events.add(Event.Tap(b.x, b.y))
}

// Take a still or slow ball within DRIBBLE_DIST. In possession and within CONTROL_DIST, tap it on
// in the direction you run whenever you catch up with it or turn, so it stays about 1 m ahead.
fun updatePossession(w: World, dir: Vec, events: MutableList<Event>) {
    val b = w.ball
    val d = dist(w.player, b)
    if (w.hasBall && d > LOSE_DIST) w.hasBall = false
    if (!w.hasBall) {
        if (d >= DRIBBLE_DIST || hypot(w.ballVel.x, w.ballVel.y) > TAKE_MAX_SPEED) return
        w.hasBall = true
        w.shot = false
        events.add(Event.Take(b.x, b.y))
        if (w.playerMoving) tap(w, dir, events) else w.ballVel = Vec.ZERO
        return
    }
    if (!w.playerMoving || d >= CONTROL_DIST) return
    // A turn taps at once; repeated taps the same way wait TAP_GAP (a ball pinned on a board).
    val last = w.lastTapDir
    val turned = last == null || dir.x * last.x + dir.y * last.y < 0.7
    if (!turned && w.t - w.lastTap < TAP_GAP) return
    if (w.ballVel.x * dir.x + w.ballVel.y * dir.y < PLAYER_SPEED) tap(w, dir, events)
}

// Space never kicks backward: straight up the pitch (+x), or KICK_ANGLE toward the side while Left or
// Right is held (Up and Down are ignored). Aim help acts only when that line already crosses x = 40
// within the mouth +- AIM_MARGIN: it is then corrected to cross no further than AIM_INSIDE_POST inside
// the nearer post. Everywhere else, lining up with the goal is the player's job. Any ball you have
// (within LOSE_DIST) can be kicked, so a dribbled ball still rolling on after you stop is yours to shoot.
fun kickDir(b: Vec, dir: Vec): Vec {
    val a = sign(dir.y) * KICK_ANGLE
    var dx = cos(a)
    var dy = sin(a)
    val run = PITCH_L - b.x
    val yc = crossingY(b.x, b.y, a)
    if (run > 0.2 && aimedIn(yc)) {
        dx = run
        dy = clamp(yc, POST_LEFT + AIM_INSIDE_POST, POST_RIGHT - AIM_INSIDE_POST) - b.y
    }
    val len = hypot(dx, dy)
    return Vec(dx / len, dy / len)
}

fun kick(w: World, dir: Vec, events: MutableList<Event>): Boolean {
    if (!w.hasBall) {
        events.add(Event.NoKick)
        return false
    }
    val d = kickDir(w.ball, dir)
    w.ballVel = Vec(d.x * KICK_SPEED, d.y * KICK_SPEED)
    w.shot = true
    w.hasBall = false
    events.add(Event.Kick(w.ball.x, w.ball.y))
    return true
}

// The keeper follows the ball's y (inside the band in front of the goal) at KEEPER_SPEED while the
// player dribbles, and stands still from the moment a shot is kicked until the ball is saved, hits a
// post or stops. He still shouts voy as a shot comes in.
fun moveKeeper(w: World, dt: Double, events: MutableList<Event>) {
    val k = w.keeper ?: return
    if (w.shot) {
        k.moving = false
    } else {
        val target = clamp(w.ball.y, KEEPER_MIN_Y, KEEPER_MAX_Y)
        val move = clamp(target - k.pos.y, -KEEPER_SPEED * dt, KEEPER_SPEED * dt)
        k.pos = Vec(k.pos.x, k.pos.y + move)
        k.moving = abs(move) > 1e-6
    }
    val dg = dist(w.ball, GOAL)
    if (k.voyArmed && dg < VOY_DIST) {
        k.voyArmed = false
        events.add(Event.Voy(k.pos.x, k.pos.y))
    } else if (!k.voyArmed && dg > VOY_DIST + 2) {
        k.voyArmed = true
    }
}

// One simulation step: `dir` is the unit direction the player runs in (none when standing), `kick`
// whether Space is pressed. Returns the events that happened, in order.
fun step(w: World, dt: Double, dir: Vec = Vec.ZERO, kick: Boolean = false): List<Event> {
    val events = mutableListOf<Event>()
    if (w.scored) return events
    w.t += dt
    movePlayer(w, dir, dt, events)
    if (kick) kick(w, dir, events)
    updatePossession(w, dir, events)
    moveKeeper(w, dt, events)
    moveBall(w, dt, events)
    return events
}
