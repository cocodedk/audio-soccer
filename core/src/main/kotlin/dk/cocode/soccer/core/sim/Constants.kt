package dk.cocode.soccer.core.sim

import kotlin.math.PI

// Pitch geometry and tuning, from prototype/js/constants.js. Pure data, shared by the whole simulation.
// x runs 0..40 toward the goal the player attacks; y runs 0..20 from the player's LEFT to RIGHT.
// (ARROWS, the trial's key map, is not ported: the app has no keyboard.)
const val PITCH_L = 40.0
const val PITCH_W = 20.0
val GOAL = Vec(40.0, 10.0)
const val POST_LEFT = 8.5 // y of the left post (player's left, smaller y)
const val POST_RIGHT = 11.5 // y of the right post
const val POST_TOLERANCE = 0.25
const val AIM_INSIDE_POST = 0.4 // aim help moves a shot no further than this far inside the nearer post
const val AIM_MARGIN = 1.0 // aim help only acts when the line crosses x = 40 within the mouth +- this
const val KICK_ANGLE = PI * 15 / 180 // Left or Right held while kicking angles the kick this far

const val PLAYER_RADIUS = 0.3
const val PLAYER_SPEED = 4.0 // m/s, normalised on diagonals
const val KICK_SPEED = 14.0 // m/s
const val CONTROL_DIST = 1.5 // with the ball this close you can tap it on and turn with it
const val DRIBBLE_DIST = 0.8 // a free ball is taken within this distance
const val TAKE_MAX_SPEED = 5.0 // a ball faster than this can't be taken
const val TAP_SPEED = 5.0 // speed of a dribble tap
const val TAP_AHEAD = 0.9 // a tap starts the ball this far ahead of the player
const val TAP_GAP = 0.35 // seconds between taps, in step with the footsteps
const val LOSE_DIST = 3.0 // possession is lost beyond this distance
const val FRICTION = 3.5 // m/s^2 of rolling deceleration
const val SLOW_DRAG = 6.0 // extra m/s^2 at a standstill, fading out by SLOW_DRAG_UNTIL,
const val SLOW_DRAG_UNTIL = 6.0 // so a dribbled ball stops near you but a kick still carries
const val STOP_SPEED = 0.05

const val BALL_BOUNCE = 0.7 // restitution off boards and end walls
const val POST_BOUNCE = 0.6
const val SAVE_BOUNCE = 0.4

const val KEEPER_X = 39.6
const val KEEPER_SPEED = 1.3 // m/s: slow enough that a lateral dribble leaves him behind
const val KEEPER_MIN_Y = POST_LEFT - 1
const val KEEPER_MAX_Y = POST_RIGHT + 1
const val KEEPER_REACH = 0.65
const val VOY_DIST = 6.0 // the keeper shouts when the ball is this close to goal

const val ROUND_CAP = 120.0 // seconds of play after which a round ends as not found / not scored
const val THIRD = PITCH_L / 3
const val BOARD_NEAR = 1.5 // footsteps brush the board within this distance
