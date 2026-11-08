import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  createWorld, step, arrowsToDir, KEEPER_SPEED, KEEPER_REACH, KEEPER_MIN_Y, KEEPER_MAX_Y, KEEPER_X,
} from '../js/world.js';
import { setupRound } from '../js/levels.js';
import { run, types, seeded } from './helpers.mjs';

const DT = 1 / 120;

// One level 3 round played by a scripted player: run straight to (sx, sy) with the ball, optionally
// settle for a second, kick holding `keys`, then watch 3 s. Returns 'goal', 'save', 'post' or 'wide'.
function play(rand, sx, sy, keys = [], settle = false) {
  const w = createWorld(setupRound(3, rand));
  for (let i = 0; i < 4000; i++) {
    const dx = sx - w.player.x, dy = sy - w.player.y, d = Math.hypot(dx, dy);
    if (d < 0.08) break;
    step(w, DT, { dir: { x: dx / d, y: dy / d } });
  }
  if (settle) run(w, 1);
  step(w, DT, { kick: true, dir: arrowsToDir(keys) });
  const t = types(run(w, 3));
  return t.includes('goal') ? 'goal' : t.includes('save') ? 'save' : t.includes('post') ? 'post' : 'wide';
}

// The outcome of n rounds with a random start (setupRound) and a shooting spot picked by spot(rand).
function outcomes(n, seed, spot, keys, settle) {
  const rand = seeded(seed), seen = new Set();
  for (let i = 0; i < n; i++) { const [x, y] = spot(rand); seen.add(play(rand, x, y, keys(y), settle)); }
  return [...seen];
}
const between = (rand, a, b) => a + rand() * (b - a);

test('keeper tuning: slow, short reach, no random error', () => {
  assert.ok(KEEPER_SPEED >= 1.2 && KEEPER_SPEED <= 1.4);
  assert.ok(KEEPER_REACH >= 0.6 && KEEPER_REACH <= 0.7);
  const s = setupRound(3, seeded(1));
  assert.equal(s.keeper, true);
  assert.equal(s.keeperBias, undefined);
  assert.equal(createWorld(s).keeper.bias, undefined);
});

test('head-on: a straight centred shot from 8 to 12 m is always saved', () => {
  for (const settle of [false, true]) {
    assert.deepEqual(outcomes(200, 11, (r) => [between(r, 28, 32), 10], () => [], settle), ['save'], `settled ${settle}`);
  }
});

test('stand slightly off to one side and hold that arrow: always a goal from 10 m', () => {
  // y 7.5 to 8.6, 10 m out, Right: the 15 degree line crosses at 10.2 to 11.3 and aim help pulls it inside the post
  for (const settle of [false, true]) {
    assert.deepEqual(outcomes(200, 12, (r) => [30, between(r, 7.5, 8.6)], () => ['ArrowRight'], settle), ['goal']);
    assert.deepEqual(outcomes(200, 13, (r) => [30, between(r, 11.4, 12.5)], () => ['ArrowLeft'], settle), ['goal']);
  }
});

// A player settled at (x, y) with the ball on y, the keeper already on it; kick holding `keys`.
function settled(x, y, keys) {
  const w = createWorld({ player: { x: x - 0.5, y }, ball: { x, y }, hasBall: true, keeper: true });
  run(w, 3);
  step(w, DT, { kick: true, dir: arrowsToDir(keys) });
  const t = types(run(w, 3));
  return t.includes('goal') ? 'goal' : t.includes('save') ? 'save' : 'wide';
}

test('from the centre, Left scores from 8 to 9 m but goes wide from 9.5 m out to 12 m', () => {
  for (const x of [32, 31.5, 31]) assert.equal(settled(x, 10, ['ArrowLeft']), 'goal', `x ${x}`);
  for (const x of [30.5, 30, 29, 28]) assert.equal(settled(x, 10, ['ArrowLeft']), 'wide', `x ${x}`);
  for (const x of [32, 31]) assert.equal(settled(x, 10, ['ArrowRight']), 'goal', `x ${x}`);
  for (const x of [30, 28]) assert.equal(settled(x, 10, ['ArrowRight']), 'wide', `x ${x}`);
});

test('the keeper follows the ball at KEEPER_SPEED and stays inside the band', () => {
  const w = createWorld({ player: { x: 30, y: 2 }, ball: { x: 30.4, y: 2 }, keeper: true });
  assert.equal(w.keeper.x, KEEPER_X);
  run(w, 1);
  assert.ok(Math.abs(w.keeper.y - (10 - KEEPER_SPEED)) < 0.05, `after 1 s ${w.keeper.y}`);
  run(w, 4);
  assert.equal(w.keeper.y, KEEPER_MIN_Y);
  w.ball.y = 10; w.ball.vx = 0; w.ball.vy = 0;
  run(w, 4);
  assert.ok(Math.abs(w.keeper.y - 10) < 0.01, 'settles on the ball');
  w.ball.y = 19; w.ball.vx = 0; w.ball.vy = 0;
  run(w, 10);
  assert.equal(w.keeper.y, KEEPER_MAX_Y);
});

test('the keeper stops the moment a shot is kicked, shouts voy, and follows again after the save', () => {
  const w = createWorld({ player: { x: 29.5, y: 10 }, ball: { x: 30, y: 10 }, hasBall: true, keeper: true });
  w.keeper.y = 9; // not yet on the ball: he is still walking to it
  step(w, DT, { kick: true });
  const y0 = w.keeper.y;
  assert.equal(w.keeper.moving, false);
  const ev = run(w, 0.4);
  assert.equal(w.keeper.y, y0, 'he does not move while the shot is on its way');
  assert.ok(types(ev).includes('voy'));
  const rest = run(w, 2);
  assert.ok(types(rest).includes('goal'), 'a straight shot 1 m from where he stopped is a goal');
  // a save clears the shot flag, and he tracks the ball again
  const s = createWorld({ player: { x: 29.5, y: 10 }, ball: { x: 30, y: 10 }, hasBall: true, keeper: true });
  step(s, DT, { kick: true });
  assert.ok(types(run(s, 2)).includes('save'));
  assert.equal(s.ball.shot, false);
  s.ball.vx = 0; s.ball.vy = 0; s.ball.y = 8;
  run(s, 1);
  assert.ok(s.keeper.y < 10 - 1, `keeper follows again: ${s.keeper.y}`);
});

test('a save pushes the ball well out, so dribbling into the keeper does not save again and again', () => {
  const w = createWorld({ player: { x: 36, y: 10 }, ball: { x: 36.5, y: 10 }, hasBall: true, keeper: true });
  const ev = run(w, 4, { dir: arrowsToDir(['ArrowUp']) });
  const saves = ev.filter((e) => e.type === 'save');
  assert.ok(saves.length >= 1 && saves.length <= 3, `saves ${saves.length}`);
});
