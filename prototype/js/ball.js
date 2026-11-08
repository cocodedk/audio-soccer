// Pure ball physics: friction, side boards, end walls, goal, posts and keeper saves. No DOM, no audio.
import {
  PITCH_L, PITCH_W, GOAL, POST_LEFT, POST_RIGHT, POST_TOLERANCE, FRICTION, SLOW_DRAG, SLOW_DRAG_UNTIL, STOP_SPEED,
  BALL_BOUNCE, POST_BOUNCE, SAVE_BOUNCE, KEEPER_X, KEEPER_REACH,
} from './constants.js';

// What happens to a ball crossing the line x = 40 at height y: 'post', 'goal' or 'wall'.
export function lineOutcome(y) {
  if (Math.abs(y - POST_LEFT) <= POST_TOLERANCE || Math.abs(y - POST_RIGHT) <= POST_TOLERANCE) return 'post';
  if (y > POST_LEFT && y < POST_RIGHT) return 'goal';
  return 'wall';
}

function sideBoards(b, speed, events) {
  if (b.y < 0) {
    b.y = -b.y; b.vy = -b.vy * BALL_BOUNCE;
    events.push({ type: 'board', side: 'left', x: b.x, y: 0, speed });
  } else if (b.y > PITCH_W) {
    b.y = 2 * PITCH_W - b.y; b.vy = -b.vy * BALL_BOUNCE;
    events.push({ type: 'board', side: 'right', x: b.x, y: PITCH_W, speed });
  }
}

function goalLine(w, px, py, speed, events) {
  const b = w.ball;
  const u = (PITCH_L - px) / (b.x - px);
  const y = py + (b.y - py) * u;
  const outcome = lineOutcome(y);
  const shot = b.shot;
  if (outcome === 'post') {
    b.x = 2 * PITCH_L - b.x; b.vx = -b.vx * POST_BOUNCE; b.shot = false;
    events.push({ type: 'post', x: PITCH_L, y: y < GOAL.y ? POST_LEFT : POST_RIGHT, speed });
  } else if (outcome === 'goal' && w.keeper && Math.abs(w.keeper.y - y) <= KEEPER_REACH) {
    b.x = KEEPER_X - 0.3; b.y = y;
    b.vx = -Math.abs(b.vx) * SAVE_BOUNCE; b.vy = b.vy * 0.3 + (y - w.keeper.y) * 2; b.shot = false;
    events.push({ type: 'save', x: PITCH_L, y, speed });
  } else if (outcome === 'goal') {
    b.x = PITCH_L + 0.6; b.y = y; b.vx = 0; b.vy = 0; b.shot = false;
    w.scored = true;
    events.push({ type: 'goal', x: PITCH_L, y, speed });
  } else {
    b.x = 2 * PITCH_L - b.x; b.vx = -b.vx * BALL_BOUNCE; b.shot = false;
    const wide = shot ? (y < GOAL.y ? 'left' : 'right') : null;
    events.push({ type: 'endwall', x: PITCH_L, y, speed, wide });
  }
}

export function moveBall(w, dt, events) {
  const b = w.ball;
  const speed = Math.hypot(b.vx, b.vy);
  if (speed < STOP_SPEED) { b.vx = 0; b.vy = 0; b.shot = false; return; }
  const decel = FRICTION + SLOW_DRAG * Math.max(0, 1 - speed / SLOW_DRAG_UNTIL);
  const f = Math.max(0, speed - decel * dt) / speed;
  b.vx *= f; b.vy *= f;
  const px = b.x, py = b.y;
  b.x += b.vx * dt; b.y += b.vy * dt;
  sideBoards(b, speed, events);
  if (b.x < 0) {
    b.x = -b.x; b.vx = -b.vx * BALL_BOUNCE;
    events.push({ type: 'endwall', x: 0, y: b.y, speed, wide: null });
  } else if (b.x > PITCH_L && px <= PITCH_L) {
    goalLine(w, px, py, speed, events);
  }
}
