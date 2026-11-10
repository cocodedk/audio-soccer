// One record per kick in levels 2 and 3, so the report can answer the trial's question: does the
// player line up with the goal by ear? Pure: game.js feeds it world events and the arrows held.
import { PITCH_L, GOAL } from './constants.js';

// At the kick: metres to the goal line, the ball's sideways offset from the goal centre (left is
// negative, as y grows to the player's right), and whether Left or Right was held (it angles the kick).
export const kickRecord = (e, dir) => ({
  dist: PITCH_L - e.x,
  offset: e.y - GOAL.y,
  aim: dir.y < 0 ? 'left' : dir.y > 0 ? 'right' : 'straight',
  outcome: null,
});

const OUTCOMES = { goal: 'goal', save: 'saved', post: 'post' };

// A world event settles the latest kick still unsettled; call it before recording a new kick. Only the
// first event counts: a rebound off a post or the keeper that later reaches the line is not that kick.
// A new kick before the last one reached the line, or the player taking the ball back first, means
// the last one stopped short: a ball then dribbled into the goal, a post or the keeper is not that kick.
export function settleKick(kicks, e) {
  const k = kicks[kicks.length - 1];
  if (!k || k.outcome) return;
  if (OUTCOMES[e.type]) k.outcome = OUTCOMES[e.type];
  else if (e.type === 'endwall' && e.wide) k.outcome = `wide ${e.wide}`;
  else if (e.type === 'kick' || e.type === 'take') k.outcome = 'short';
}

// At the end of a round, a kick that never reached the line stopped short of it, unless the round was
// left with the ball still rolling. Returns new records; the ones passed in are not changed.
export const closeKicks = (kicks = [], how) =>
  kicks.map((k) => (k.outcome ? { ...k } : { ...k, outcome: how === 'left' ? 'unfinished' : 'short' }));

// "18 m, 2.1 m right, straight, wide right"
export function kickText(k) {
  const off = Math.abs(k.offset) < 0.05 ? 'centre' : `${Math.abs(k.offset).toFixed(1)} m ${k.offset < 0 ? 'left' : 'right'}`;
  return `${Math.round(k.dist)} m, ${off}, ${k.aim}, ${k.outcome || 'unfinished'}`;
}
