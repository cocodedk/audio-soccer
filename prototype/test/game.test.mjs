import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createGame } from '../js/game.js';
import { startLines, ROUNDS } from '../js/levels.js';
import { levelLines } from '../js/report.js';
import { ROUND_CAP } from '../js/world.js';
import { seeded, close } from './helpers.mjs';

const NONE = { x: 0, y: 0 };
const UP = { x: 1, y: 0 };
// game.js runs fine under node: with no AudioContext or speech engine, sound and speech do nothing.
const newGame = (seed = 1) => {
  const done = [];
  return { done, game: createGame({ onLevelDone: (d) => done.push(d), rand: seeded(seed) }) };
};
const tick = (game, secs, dir = NONE, dt = 0.05) => { for (let t = 0; t < secs; t += dt) game.update(dt, dir); };
// Walk straight at the ball until the round ends (level 1).
function walkToBall(game) {
  for (let i = 0; i < 4000 && game.phase === 'play'; i++) {
    const { player, ball } = game.world, dx = ball.x - player.x, dy = ball.y - player.y, d = Math.hypot(dx, dy) || 1;
    game.update(1 / 60, { x: dx / d, y: dy / d });
  }
}

test('the round clock starts at the first arrow press, not at the round start', () => {
  const { game } = newGame();
  game.startLevel(1);
  tick(game, 5);
  assert.equal(game.time, 0);
  tick(game, 1, UP);
  close(game.time, 1, 0.06);
  tick(game, 1);
  close(game.time, 2, 0.11);
});

test('G gives up the round: not found, elapsed time kept, and the next round starts after the pause', () => {
  const { game, done } = newGame();
  game.startLevel(1);
  tick(game, 2, { x: 0, y: -1 });
  game.action('giveup');
  assert.equal(game.phase, 'between');
  const r = game.snapshot().stats.rounds[0];
  assert.deepEqual([r.gaveUp, r.timedOut, r.success, r.efficiency], [true, false, false, null]);
  close(r.time, 2, 0.06);
  game.action('giveup'); // ignored between rounds
  assert.equal(game.snapshot().stats.rounds.length, 1);
  tick(game, 3.2);
  assert.deepEqual([game.phase, game.round], ['play', 2]);
  game.action('giveup'); tick(game, 3.2); game.action('giveup'); tick(game, 3.2);
  assert.equal(game.phase, 'idle');
  assert.equal(done.length, 1);
  assert.deepEqual(done[0].rounds.map((x) => x.gaveUp), [true, true, true]);
  assert.equal(done[0].rounds.length, ROUNDS);
});

test('a round auto-ends at 120 s as not found, the same way as giving up', () => {
  let seed = 1, game;
  do { ({ game } = newGame(seed++)); game.startLevel(1); } while (game.world.ball.x < 8); // a ball well away from x = 0
  tick(game, ROUND_CAP - 1, { x: -1, y: 0 });
  assert.equal(game.phase, 'play');
  tick(game, 2, { x: -1, y: 0 });
  assert.equal(game.phase, 'between');
  const r = game.snapshot().stats.rounds[0];
  assert.deepEqual([r.gaveUp, r.timedOut, r.success], [true, true, false]);
  close(r.time, ROUND_CAP, 0.06);
});

test('a found ball records start distance, start side, time, efficiency and the counters', () => {
  const { game } = newGame(5);
  game.startLevel(1);
  const s0 = game.snapshot(), { player, ball } = s0;
  walkToBall(game);
  const r = game.snapshot().stats.rounds[0];
  assert.equal(r.success, true);
  assert.equal(r.gaveUp, false);
  assert.ok(r.startDist >= 10);
  close(r.startDist, Math.hypot(ball.x - player.x, ball.y - player.y), 1e-9);
  assert.equal(r.startBehind, ball.x < player.x);
  assert.ok(r.efficiency > 0.6 && r.efficiency <= 1.1, `efficiency ${r.efficiency}`);
  close(r.efficiency, r.startDist / (4 * r.time), 1e-9);
  for (const k of ['where', 'bumps', 'kicks', 'wide', 'saves']) assert.equal(typeof r[k], 'number', k);
});

test('level 2: a wide shot is counted; level 3: a save is counted; efficiency only in level 1', () => {
  const { game } = newGame(2);
  game.startLevel(2);
  Object.assign(game.world.player, { x: 29.5, y: 4 }); Object.assign(game.world.ball, { x: 30, y: 4 });
  game.update(0.05, UP); // the first press starts the clock
  game.action('kick'); game.update(0.05, NONE);
  tick(game, 2);
  const w = game.snapshot().stats.current;
  assert.deepEqual([w.kicks, w.wide, w.saves], [1, 1, 0]);

  const k = newGame(3).game;
  k.startLevel(3);
  Object.assign(k.world.player, { x: 29.5, y: 10 }); Object.assign(k.world.ball, { x: 30, y: 10 });
  k.action('kick'); k.update(0.05, NONE);
  tick(k, 2);
  const s = k.snapshot().stats.current;
  assert.deepEqual([s.kicks, s.saves], [1, 1]);
  k.action('giveup');
  assert.equal(k.snapshot().stats.rounds[0].efficiency, null);
});

test('W counts as asking where; a level left half-played reports its rounds so far', () => {
  const { game } = newGame();
  assert.equal(game.partial(), null);
  game.startLevel(1);
  assert.equal(game.partial(), null, 'a round nobody has touched carries nothing');
  game.action('where'); game.action('where');
  assert.equal(game.snapshot().stats.current.where, 2);
  game.action('giveup');
  const p = game.partial();
  assert.deepEqual([p.level, p.rounds.length, p.partial], [1, 1, true]);
  assert.equal(p.rounds[0].where, 2);
  assert.ok(game.pause());
  assert.equal(game.partial().rounds.length, 1, 'paused between rounds: nothing extra');
  game.startLevel(2);
  assert.equal(game.partial(), null, 'a new level starts empty');
});

test('leaving mid-round closes that round as left, with its time and counters, without ending it', () => {
  const { game } = newGame(4);
  game.startLevel(1);
  tick(game, 1, { x: 0, y: -1 });
  game.action('where');
  const p = game.partial();
  assert.equal(p.rounds.length, 1);
  const r = p.rounds[0];
  assert.deepEqual([r.left, r.gaveUp, r.timedOut, r.success, r.efficiency, r.where], [true, true, false, false, null, 1]);
  close(r.time, 1, 0.06);
  assert.equal(game.snapshot().stats.current.left, false, 'the round in play is untouched');
  assert.equal(game.phase, 'play');
  assert.ok(game.pause());
  assert.equal(game.partial().rounds[0].left, true, 'paused from play: still counted');
  assert.equal(game.partial().rounds[0].paused, false, 'leaving for another level: left, not paused');
  const preview = levelLines(game.partial(true));
  assert.ok(preview.some((l) => /not found, paused at 1\.0 s/.test(l)), preview.join('\n'));
  assert.ok(game.resume());
  assert.equal(game.phase, 'play');
  tick(game, 0.5, { x: 0, y: -1 });
  close(game.partial().rounds[0].time, 1.5, 0.11);
  game.action('giveup'); // now a normal given-up round, not a left one
  const q = game.partial();
  assert.deepEqual([q.rounds.length, q.rounds[0].left, q.rounds[0].gaveUp], [1, false, true]);
});

test('level 3 start lines explain the keeper; level 2 says about twenty metres; round 1 only', () => {
  const l3 = startLines(3, 1);
  assert.ok(l3.includes("The keeper sets himself on the ball and can't move once you kick. Get the goal slightly to your left or right, hold that arrow, and kick."));
  assert.deepEqual(startLines(3, 2), ['Round 2. Beat the keeper.']);
  assert.equal(startLines(1, 1).length, 1);
  assert.ok(startLines(2, 1).some((l) => /straight up the pitch/.test(l) && /twenty metres/.test(l)));
});
