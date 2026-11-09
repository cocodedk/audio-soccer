// Levels, round setup, per-round stats and the lines spoken at the start and end of a level. The win
// word is for the copied report; what is spoken and shown comes from the i18n tables (a language code
// can be passed, English by default).
import { PLAYER_SPEED } from './constants.js';
import { tr, trList } from './i18n/tables.js';

export const ROUNDS = 3;

export const LEVELS = {
  1: { keeper: false, endsOn: 'take', win: 'found' },
  2: { keeper: false, endsOn: 'goal', win: 'scored' },
  3: { keeper: true, endsOn: 'goal', win: 'scored' },
};

const between = (rand, a, b) => a + rand() * (b - a);

// Starting positions for one round. Level 1: player and a still ball at least 10 m apart.
// Levels 2 and 3: the player starts with the ball somewhere in their own half.
export function setupRound(level, rand = Math.random) {
  if (level === 1) {
    const player = { x: between(rand, 3, 37), y: between(rand, 3, 17) };
    let ball;
    do {
      ball = { x: between(rand, 2, 38), y: between(rand, 2, 18) };
    } while (Math.hypot(ball.x - player.x, ball.y - player.y) < 10);
    return { player, ball, hasBall: false, keeper: false };
  }
  const player = { x: between(rand, 4, 16), y: between(rand, 4, 16) };
  return { player, ball: { x: player.x + 0.5, y: player.y }, hasBall: true, keeper: LEVELS[level].keeper };
}

// One round's record. startDist and startBehind describe where the ball was at the start (they matter
// in level 1; in levels 2 and 3 the player starts with the ball).
export const newStats = (setup) => ({
  time: 0, where: 0, bumps: 0, kicks: 0, wide: 0, saves: 0,
  startDist: Math.hypot(setup.ball.x - setup.player.x, setup.ball.y - setup.player.y),
  startBehind: setup.ball.x < setup.player.x,
  success: false, gaveUp: false, timedOut: false, left: false, efficiency: null,
});

// Close a round: how is 'success', 'giveup' (G), 'timeout' (the cap) or 'left' (the player went to
// another level mid-round). All but success count as not found / not scored and keep the elapsed time. Level 1 efficiency = startDist / (PLAYER_SPEED x time):
// 1 means a straight run at full speed.
export function closeStats(level, s, how) {
  s.success = how === 'success';
  s.gaveUp = how !== 'success';
  s.timedOut = how === 'timeout';
  s.left = how === 'left';
  s.efficiency = level === 1 && s.success && s.time > 0 ? s.startDist / (PLAYER_SPEED * s.time) : null;
  return s;
}

// Spoken at the start of a round. The explanations come in round 1 only.
export const startLines = (level, round, lang = 'en') => [
  tr(lang, 'roundIntro', { round, task: tr(lang, `levels.${level}.task`) }),
  ...(round === 1 ? trList(lang, `extra.${level}`) : []),
];

// "Level 1 done. Times: 12.4, not found, 10.2."
export function summarySpeech(level, rounds, partial = false, lang = 'en') {
  const miss = tr(lang, `levels.${level}.miss`);
  const list = rounds.map((r) => (r.success ? r.time.toFixed(1) : miss)).join(tr(lang, 'summary.sep'));
  const head = partial ? tr(lang, 'summary.stopped', { level, n: rounds.length, total: ROUNDS }) : tr(lang, 'summary.done', { level });
  return tr(lang, 'summary.times', { head, list });
}
