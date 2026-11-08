// Pure text descriptions: clock positions, thirds, where-am-I, the goal guide. No DOM, no audio.
import { GOAL, THIRD } from './constants.js';

// 12 o'clock = toward the goal you attack (+x), 3 = your right (+y), 6 = behind, 9 = your left.
export function clockHour(ahead, right) {
  const h = Math.round(Math.atan2(right, ahead) / (Math.PI / 6));
  const m = ((h % 12) + 12) % 12;
  return m === 0 ? 12 : m;
}

export function metresText(d) {
  const r = Math.round(d);
  return `${r} metre${r === 1 ? '' : 's'}`;
}

// A relative offset (metres ahead, metres to the right) as "2 o'clock, 3 metres", or "at your feet".
export function clockText(ahead, right) {
  const d = Math.hypot(ahead, right);
  if (d < 1) return 'at your feet';
  return `${clockHour(ahead, right)} o'clock, ${metresText(d)}`;
}

export function clockTo(from, to) {
  return clockText(to.x - from.x, to.y - from.y);
}

export function thirdIndex(x) {
  return x < THIRD ? 0 : x < 2 * THIRD ? 1 : 2;
}

export const THIRD_NAMES = ['Defending third', 'Middle third', 'Attacking third'];

export function sideText(y) {
  if (y < 3) return 'near the left board';
  if (y > 17) return 'near the right board';
  return 'in the centre';
}

function placed(name, from, to) {
  const c = clockTo(from, to);
  return c === 'at your feet' ? `${name} at your feet.` : `${name} at ${c}.`;
}

// "Middle third, near the left board. Ball at 2 o'clock, 3 metres. Goal at 1 o'clock, 22 metres."
export function whereText(world) {
  const p = world.player;
  const parts = [`${THIRD_NAMES[thirdIndex(p.x)]}, ${sideText(p.y)}.`, placed('Ball', p, world.ball), placed('Goal', p, GOAL)];
  if (world.keeper) parts.push(placed('Keeper', p, world.keeper));
  return parts.join(' ');
}

// "Goal, 9 metres, slightly left" from the player's position.
export function guideText(p) {
  const ahead = GOAL.x - p.x, right = GOAL.y - p.y;
  const a = Math.atan2(right, ahead) * 180 / Math.PI;
  const side = a < 0 ? 'left' : 'right';
  const abs = Math.abs(a);
  const dir = abs < 8 ? 'straight ahead' : abs < 25 ? `slightly ${side}` : abs < 55 ? side : `hard ${side}`;
  return `Goal, ${metresText(Math.hypot(ahead, right))}, ${dir}`;
}
