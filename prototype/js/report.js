// The results text: plain lines a tester can paste into an email. One header with how the game was
// heard, then each level and each round, including rounds given up or timed out.
import { LEVELS, ROUNDS } from './levels.js';

const s1 = (n) => n.toFixed(1);

function outcome(L, r) {
  if (r.success) return `${L.win} in ${s1(r.time)} s`;
  const how = r.timedOut ? 'time up' : r.paused ? 'paused' : r.left ? 'left the round' : 'gave up';
  return `${L.miss}, ${how} at ${s1(r.time)} s`;
}

// "Round 2: not found, gave up at 45.0 s. Start 22 m, ball behind. Asked where 3. Bumps 0."
export function roundLine(level, i, r) {
  const L = LEVELS[level], parts = [`Round ${i + 1}: ${outcome(L, r)}.`];
  if (level === 1) {
    parts.push(`Start ${Math.round(r.startDist)} m, ball ${r.startBehind ? 'behind' : 'ahead'}.`);
    if (r.efficiency !== null) parts.push(`Path efficiency ${r.efficiency.toFixed(2)}.`);
  } else {
    parts.push(`Kicks ${r.kicks}. Wide ${r.wide}.`);
    if (level === 3) parts.push(`Saved ${r.saves}.`);
  }
  parts.push(`Asked where ${r.where}. Bumps ${r.bumps}.`);
  return parts.join(' ');
}

export function levelLines({ level, rounds, partial = false }) {
  const note = partial ? ` (stopped after ${rounds.length} of ${ROUNDS} rounds)` : '';
  const keeper = level === 3 ? ['Keeper: follows the ball, stops at the kick.'] : [];
  return [`Level ${level}, ${LEVELS[level].name}${note}`, ...keeper, ...rounds.map((r, i) => roundLine(level, i, r))];
}

// meta: { mode: 'voice' | 'sr', rate, hidden } as it was when the block was made.
export function resultsText(blocks, meta) {
  const how = meta.mode === 'sr' ? 'own screen reader' : 'built-in voice';
  const head = `Audio Soccer trial. Voice: ${how}. Speech speed ${Number(meta.rate).toFixed(1)}. Picture ${meta.hidden ? 'hidden' : 'shown'}.`;
  return [head, ...blocks.flatMap(levelLines)].join('\n');
}
