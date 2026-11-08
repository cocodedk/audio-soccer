// Pitch geometry and tuning. Pure data, shared by world.js, ball.js and describe.js.
// x runs 0..40 toward the goal the player attacks; y runs 0..20 from the player's LEFT to RIGHT.
export const PITCH_L = 40;
export const PITCH_W = 20;
export const GOAL = { x: 40, y: 10 };
export const POST_LEFT = 8.5;    // y of the left post (player's left, smaller y)
export const POST_RIGHT = 11.5;  // y of the right post
export const POST_TOLERANCE = 0.25;
export const AIM_INSIDE_POST = 0.4; // aim help moves a shot no further than this far inside the nearer post
export const AIM_MARGIN = 1;         // aim help only acts when the line crosses x = 40 within the mouth +- this
export const KICK_ANGLE = Math.PI * 15 / 180; // Left or Right held while kicking angles the kick this far

export const PLAYER_RADIUS = 0.3;
export const PLAYER_SPEED = 4;        // m/s, normalised on diagonals
export const KICK_SPEED = 14;         // m/s
export const CONTROL_DIST = 1.5;      // with the ball this close you can tap it on and turn with it
export const DRIBBLE_DIST = 0.8;      // a free ball is taken within this distance
export const TAKE_MAX_SPEED = 5;      // a ball faster than this can't be taken
export const TAP_SPEED = 5;           // speed of a dribble tap
export const TAP_AHEAD = 0.9;         // a tap starts the ball this far ahead of the player
export const TAP_GAP = 0.35;          // seconds between taps, in step with the footsteps
export const LOSE_DIST = 3;           // possession is lost beyond this distance
export const FRICTION = 3.5;          // m/s^2 of rolling deceleration
export const SLOW_DRAG = 6;           // extra m/s^2 at a standstill, fading out by SLOW_DRAG_UNTIL,
export const SLOW_DRAG_UNTIL = 6;     // so a dribbled ball stops near you but a kick still carries
export const STOP_SPEED = 0.05;

export const BALL_BOUNCE = 0.7;       // restitution off boards and end walls
export const POST_BOUNCE = 0.6;
export const SAVE_BOUNCE = 0.4;

export const KEEPER_X = 39.6;
export const KEEPER_SPEED = 1.3;      // m/s: slow enough that a lateral dribble leaves him behind
export const KEEPER_MIN_Y = POST_LEFT - 1;
export const KEEPER_MAX_Y = POST_RIGHT + 1;
export const KEEPER_REACH = 0.65;
export const VOY_DIST = 6;            // the keeper shouts when the ball is this close to goal

export const ROUND_CAP = 120;         // seconds of play after which a round ends as not found / not scored
export const THIRD = PITCH_L / 3;
export const BOARD_NEAR = 1.5;        // footsteps brush the board within this distance

export const ARROWS = {
  ArrowUp: { x: 1, y: 0 },     // toward the goal
  ArrowDown: { x: -1, y: 0 },  // away from the goal
  ArrowLeft: { x: 0, y: -1 },  // player's left
  ArrowRight: { x: 0, y: 1 },  // player's right
};
