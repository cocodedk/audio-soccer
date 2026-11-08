import { test } from 'node:test';
import assert from 'node:assert/strict';
import { arrowsToDir, createWorld, step, DRIBBLE_DIST } from '../js/world.js';
import { close, run, types } from './helpers.mjs';

test('dribble: a still ball is taken within 0.8 m, not beyond, and not when fast', () => {
  const near = createWorld({ player: { x: 10, y: 10 }, ball: { x: 10 + DRIBBLE_DIST - 0.01, y: 10 } });
  assert.ok(types(step(near, 1 / 120)).includes('take'));
  assert.equal(near.hasBall, true);
  const far = createWorld({ player: { x: 10, y: 10 }, ball: { x: 10 + DRIBBLE_DIST + 0.01, y: 10 } });
  assert.ok(!types(step(far, 1 / 120)).includes('take'));
  const fast = createWorld({ player: { x: 10, y: 10 }, ball: { x: 10.3, y: 10 } });
  fast.ball.vy = 9;
  assert.ok(!types(step(fast, 1 / 120)).includes('take'));
});

test('dribble: running with the ball taps it about 1 m ahead and keeps it', () => {
  const w = createWorld({ player: { x: 5, y: 10 }, ball: { x: 5.5, y: 10 }, hasBall: true });
  let maxGap = 0, taps = 0;
  for (let t = 0; t < 3; t += 1 / 120) {
    taps += step(w, 1 / 120, { dir: { x: 1, y: 0 } }).filter((e) => e.type === 'tap').length;
    maxGap = Math.max(maxGap, w.ball.x - w.player.x);
  }
  assert.equal(w.hasBall, true);
  assert.ok(taps >= 5 && taps <= 10, `taps ${taps}`);
  assert.ok(maxGap > 0.8 && maxGap < 1.4, `gap ${maxGap}`);
  close(w.player.x, 17, 0.1);
});

test('dribble: turning keeps the ball, and a dribbled ball stops near you', () => {
  const w = createWorld({ player: { x: 5, y: 10 }, ball: { x: 5.5, y: 10 }, hasBall: true });
  run(w, 1, { dir: arrowsToDir(['ArrowUp']) });
  run(w, 1.5, { dir: arrowsToDir(['ArrowRight']) });
  assert.equal(w.hasBall, true);
  assert.ok(w.ball.y - w.player.y > 0.5, 'the ball went right with the player');
  run(w, 3);
  assert.ok(Math.hypot(w.ball.x - w.player.x, w.ball.y - w.player.y) < 3, 'still within reach after stopping');
  assert.equal(w.hasBall, true);
});

test('dribble, stop for 1 s, then Space with no arrows kicks straight up the pitch', () => {
  for (const secs of [0.7, 1, 1.3, 1.9, 2.4, 3.1]) {
    const w = createWorld({ player: { x: 5, y: 10 }, ball: { x: 5.5, y: 10 }, hasBall: true });
    run(w, secs, { dir: arrowsToDir(['ArrowUp']) }); run(w, 1);
    assert.ok(types(step(w, 1 / 120, { kick: true })).includes('kick') && w.ball.vx > 13, `after ${secs} s`);
  }
});

test('dribble: turning right around takes the ball with you', () => {
  const w = createWorld({ player: { x: 20, y: 10 }, ball: { x: 19.5, y: 10 }, hasBall: true });
  run(w, 1, { dir: arrowsToDir(['ArrowDown']) });
  run(w, 1, { dir: arrowsToDir(['ArrowUp']) });
  assert.equal(w.hasBall, true);
  assert.ok(w.ball.x > w.player.x, 'the ball is ahead again');
});
