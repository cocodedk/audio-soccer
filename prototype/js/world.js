// PURE game world: vectors, player and ball physics, possession, keeper, events. No DOM, no Web Audio.
import * as C from './constants.js';
import { moveBall, crossingY, aimedIn } from './ball.js';

export * from './constants.js';
export * from './describe.js';
export { moveBall, lineOutcome, crossingY, aimedIn, rollDistance, AIM_LOW, AIM_HIGH } from './ball.js';

export const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
export const dist = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);

// Held arrow keys (any iterable of key names) to a unit direction in pitch coordinates.
export function arrowsToDir(held) {
  let x = 0, y = 0;
  for (const k of held) {
    const a = C.ARROWS[k];
    if (a) { x += a.x; y += a.y; }
  }
  const len = Math.hypot(x, y);
  return len ? { x: x / len, y: y / len } : { x: 0, y: 0 };
}

// Game point to Web Audio space. The listener faces (0, 0, -1), so ahead (+x) is -Z and the
// player's right (+y) is +X: a point on the player's left has a smaller audio X.
export function toAudio(p) {
  return { x: p.y, y: 0, z: -p.x };
}

export function createWorld({ player, ball, hasBall = false, keeper = false }) {
  return {
    t: 0,
    player: { x: player.x, y: player.y, moving: false, touching: null },
    ball: { x: ball.x, y: ball.y, vx: 0, vy: 0, shot: false },
    hasBall,
    lastTap: -Infinity,
    lastTapDir: null,
    keeper: keeper ? { x: C.KEEPER_X, y: C.GOAL.y, moving: false, voyArmed: true } : null,
    scored: false,
  };
}

export function movePlayer(w, dir, dt, events) {
  const p = w.player, r = C.PLAYER_RADIUS;
  const nx = p.x + dir.x * C.PLAYER_SPEED * dt, ny = p.y + dir.y * C.PLAYER_SPEED * dt;
  const x = clamp(nx, r, C.PITCH_L - r), y = clamp(ny, r, C.PITCH_W - r);
  let touch = null, at = null;
  if (y !== ny) { touch = ny < y ? 'left' : 'right'; at = { x, y: ny < y ? 0 : C.PITCH_W }; }
  else if (x !== nx) { touch = 'end'; at = { x: nx < x ? 0 : C.PITCH_L, y }; }
  if (touch && touch !== p.touching) events.push({ type: 'bump', side: touch, ...at });
  p.touching = touch;
  p.x = x; p.y = y;
  p.moving = dir.x !== 0 || dir.y !== 0;
}

function tap(w, dir, events) {
  const p = w.player, b = w.ball;
  b.x = clamp(p.x + dir.x * C.TAP_AHEAD, 0.1, C.PITCH_L - 0.1);
  b.y = clamp(p.y + dir.y * C.TAP_AHEAD, 0.1, C.PITCH_W - 0.1);
  b.vx = dir.x * C.TAP_SPEED; b.vy = dir.y * C.TAP_SPEED;
  w.lastTap = w.t;
  w.lastTapDir = { x: dir.x, y: dir.y };
  events.push({ type: 'tap', x: b.x, y: b.y });
}

// Take a still or slow ball within DRIBBLE_DIST. In possession and within CONTROL_DIST, tap it on
// in the direction you run whenever you catch up with it or turn, so it stays about 1 m ahead.
export function updatePossession(w, dir, events) {
  const p = w.player, b = w.ball;
  const d = dist(p, b);
  if (w.hasBall && d > C.LOSE_DIST) w.hasBall = false;
  if (!w.hasBall) {
    if (d >= C.DRIBBLE_DIST || Math.hypot(b.vx, b.vy) > C.TAKE_MAX_SPEED) return;
    w.hasBall = true; b.shot = false;
    events.push({ type: 'take', x: b.x, y: b.y });
    if (p.moving) tap(w, dir, events);
    else { b.vx = 0; b.vy = 0; }
    return;
  }
  if (!p.moving || d >= C.CONTROL_DIST) return;
  // A turn taps at once; repeated taps the same way wait TAP_GAP (a ball pinned on a board).
  const last = w.lastTapDir;
  const turned = !last || dir.x * last.x + dir.y * last.y < 0.7;
  if (!turned && w.t - w.lastTap < C.TAP_GAP) return;
  if (b.vx * dir.x + b.vy * dir.y < C.PLAYER_SPEED) tap(w, dir, events);
}

// Space never kicks backward: straight up the pitch (+x), or KICK_ANGLE toward the side while Left or
// Right is held (Up and Down are ignored). Aim help acts only when that line already crosses x = 40
// within the mouth +- AIM_MARGIN: it is then corrected to cross no further than AIM_INSIDE_POST inside
// the nearer post. Everywhere else, lining up with the goal is the player's job. Any ball you have
// (within LOSE_DIST) can be kicked, so a dribbled ball still rolling on after you stop is yours to shoot.
export function kickDir(b, dir) {
  const a = Math.sign(dir.y) * C.KICK_ANGLE;
  let dx = Math.cos(a), dy = Math.sin(a);
  const run = C.PITCH_L - b.x, yc = crossingY(b.x, b.y, a);
  if (run > 0.2 && aimedIn(yc)) {
    dx = run; dy = clamp(yc, C.POST_LEFT + C.AIM_INSIDE_POST, C.POST_RIGHT - C.AIM_INSIDE_POST) - b.y;
  }
  const len = Math.hypot(dx, dy);
  return { x: dx / len, y: dy / len };
}

export function kick(w, dir, events) {
  const b = w.ball;
  if (!w.hasBall) {
    events.push({ type: 'nokick' });
    return false;
  }
  const d = kickDir(b, dir);
  b.vx = d.x * C.KICK_SPEED; b.vy = d.y * C.KICK_SPEED; b.shot = true;
  w.hasBall = false;
  events.push({ type: 'kick', x: b.x, y: b.y });
  return true;
}

// The keeper follows the ball's y (inside the band in front of the goal) at KEEPER_SPEED while the
// player dribbles, and stands still from the moment a shot is kicked until the ball is saved, hits a
// post or stops. He still shouts voy as a shot comes in.
export function moveKeeper(w, dt, events) {
  const k = w.keeper;
  if (!k) return;
  if (w.ball.shot) {
    k.moving = false;
  } else {
    const target = clamp(w.ball.y, C.KEEPER_MIN_Y, C.KEEPER_MAX_Y);
    const move = clamp(target - k.y, -C.KEEPER_SPEED * dt, C.KEEPER_SPEED * dt);
    k.y += move;
    k.moving = Math.abs(move) > 1e-6;
  }
  const dg = dist(w.ball, C.GOAL);
  if (k.voyArmed && dg < C.VOY_DIST) {
    k.voyArmed = false;
    events.push({ type: 'voy', x: k.x, y: k.y });
  } else if (!k.voyArmed && dg > C.VOY_DIST + 2) {
    k.voyArmed = true;
  }
}

// One simulation step. input = { dir, kick }. Returns the events that happened, in order.
export function step(w, dt, input = {}) {
  const dir = input.dir || { x: 0, y: 0 };
  const events = [];
  if (w.scored) return events;
  w.t += dt;
  movePlayer(w, dir, dt, events);
  if (input.kick) kick(w, dir, events);
  updatePossession(w, dir, events);
  moveKeeper(w, dt, events);
  moveBall(w, dt, events);
  return events;
}
