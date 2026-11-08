// Levels, round setup, per-round stats and the results text.
export const ROUNDS = 3;

export const LEVELS = {
  1: { name: 'Find the ball', task: 'Find the ball.', keeper: false, endsOn: 'take' },
  2: { name: 'Score', task: 'Score a goal.', keeper: false, endsOn: 'goal' },
  3: { name: 'Beat the keeper', task: 'Beat the keeper.', keeper: true, endsOn: 'goal' },
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

export const newStats = () => ({ time: 0, where: 0, bumps: 0, kicks: 0 });

export const startLine = (level, round) => `Round ${round}. ${LEVELS[level].task}`;

const times = (k) => (k === 1 ? '1 time' : `${k} times`);
const sum = (rounds, key) => rounds.reduce((s, r) => s + r[key], 0);

// "Level 1, Find the ball: 12.4 s, 9.8 s, 15.1 s. Asked where 4 times. Bumped the boards 2 times."
export function resultLine(level, rounds) {
  const L = LEVELS[level];
  let s = `Level ${level}, ${L.name}: ${rounds.map((r) => `${r.time.toFixed(1)} s`).join(', ')}.`;
  s += ` Asked where ${times(sum(rounds, 'where'))}. Bumped the boards ${times(sum(rounds, 'bumps'))}.`;
  if (level > 1) s += ` Kicked ${times(sum(rounds, 'kicks'))}.`;
  return s;
}

// All levels finished this session, as one block to copy.
export function resultsText(done) {
  return ['Audio Soccer trial.', ...done.map((d) => resultLine(d.level, d.rounds))].join(' ');
}

export function summarySpeech(level, rounds) {
  const ts = rounds.map((r) => r.time.toFixed(1));
  const list = ts.length > 1 ? `${ts.slice(0, -1).join(', ')} and ${ts[ts.length - 1]}` : ts[0];
  return `Level ${level} done. Your times: ${list} seconds.`;
}
