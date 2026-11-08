import { test } from 'node:test';
import assert from 'node:assert/strict';
import { arrowsToDir, createWorld, step, kickDir, KICK_SPEED, KICK_ANGLE, PITCH_L } from '../js/world.js';
import { close, run, types } from './helpers.mjs';

const kickFrom = (x, y, keys = []) => {
  const w = createWorld({ player: { x: x - 0.5, y }, ball: { x, y }, hasBall: true });
  const ev = step(w, 1 / 120, { kick: true, dir: arrowsToDir(keys) });
  return { b: w.ball, ev };
};
// Where the kicked ball's line crosses x = 40.
const crossing = (b) => b.y + (b.vy / b.vx) * (PITCH_L - b.x);

test('Space kicks straight up the pitch everywhere, with no aim at the goal from the attacking third', () => {
  for (const x of [5, 15, 25, 30, 35]) {
    const { b, ev } = kickFrom(x, 4);
    assert.ok(types(ev).includes('kick'));
    close(b.vy, 0); close(b.vx, KICK_SPEED, 0.1); // one step of friction has already acted
  }
});

test('Left or Right held angles the kick 15 degrees toward that side; Up and Down are ignored', () => {
  const slope = (keys) => { const { b } = kickFrom(10, 10, keys); return b.vy / b.vx; };
  close(slope(['ArrowRight']), Math.tan(KICK_ANGLE), 1e-9);
  close(slope(['ArrowLeft']), -Math.tan(KICK_ANGLE), 1e-9);
  close(slope(['ArrowUp', 'ArrowRight']), slope(['ArrowRight']), 1e-9);
  close(slope(['ArrowDown', 'ArrowLeft']), slope(['ArrowLeft']), 1e-9);
  close(slope(['ArrowUp']), 0); close(slope(['ArrowDown']), 0);
  close(slope(['ArrowLeft', 'ArrowRight']), 0);
  const { b } = kickFrom(10, 10, ['ArrowRight']);
  close(Math.hypot(b.vx, b.vy), KICK_SPEED, 0.1);
});

test('a kick never goes backward, whatever is held and wherever the ball is', () => {
  const sets = [[], ['ArrowUp'], ['ArrowDown'], ['ArrowLeft'], ['ArrowRight'], ['ArrowDown', 'ArrowRight'], ['ArrowDown', 'ArrowLeft']];
  for (const keys of sets) {
    for (const [x, y] of [[2, 10], [20, 1], [30, 19], [38, 8], [39.5, 10]]) {
      const { b } = kickFrom(x, y, keys);
      assert.ok(b.vx > 0, `${keys} from ${x},${y}: vx ${b.vx}`);
    }
  }
});

test('aim help: a line that already crosses within the mouth +- 1 m is corrected to just inside the post', () => {
  const at = (y, x = 35) => crossing(kickFrom(x, y).b);
  close(at(10), 10, 1e-6);                          // already in the goal: untouched
  close(at(9.5), 9.5, 1e-6);
  close(at(8.0), 8.9, 1e-6);                        // wide of the post by 0.5 m: pulled in to 0.4 m inside it
  close(at(8.6), 8.9, 1e-6);                        // inside the post by 0.1 m
  close(at(7.6), 8.9, 1e-6);
  close(at(12.4), 11.1, 1e-6);
  close(at(11.3), 11.1, 1e-6);
  close(at(11.0), 11.0, 1e-6);
});

test('aim help leaves a line alone that is more than 1 m outside the mouth', () => {
  for (const y of [7.4, 5, 1, 12.6, 15, 19]) {
    const { b } = kickFrom(35, y);
    close(b.vy, 0); close(crossing(b), y, 1e-6);
  }
});

test('aim help acts on an angled line too, and an angled line far out is not corrected', () => {
  const near = kickFrom(32, 8, ['ArrowRight']).b;              // 8 + 8 tan 15 = 10.14: in the goal already
  close(crossing(near), 8 + 8 * Math.tan(KICK_ANGLE), 1e-6);
  const edge = kickFrom(32, 10, ['ArrowRight']).b;             // 10 + 2.14 = 12.14: pulled in to 11.1
  close(crossing(edge), 11.1, 1e-6);
  const far = kickFrom(20, 10, ['ArrowRight']).b;              // 10 + 5.4 = 15.4: no help, wide
  close(far.vy / far.vx, Math.tan(KICK_ANGLE), 1e-9);
});

test('kickDir is a unit vector pointing up the pitch', () => {
  for (const [y, side] of [[10, 0], [4, 1], [8, -1], [12.4, 1]]) {
    const d = kickDir({ x: 33, y }, { x: 0, y: side });
    close(Math.hypot(d.x, d.y), 1, 1e-9);
    assert.ok(d.x > 0);
  }
});

test('a straight shot from outside the mouth hits the end wall and is called wide', () => {
  const w = createWorld({ player: { x: 29.5, y: 4 }, ball: { x: 30, y: 4 }, hasBall: true });
  step(w, 1 / 120, { kick: true });
  const ev = run(w, 2);
  assert.ok(!types(ev).includes('goal'));
  assert.equal(ev.find((e) => e.type === 'endwall').wide, 'left');
});

test('a straight shot from centre, 14 m out, is a goal; you cannot kick a ball you do not have', () => {
  const w = createWorld({ player: { x: 25.5, y: 10 }, ball: { x: 26, y: 10 }, hasBall: true });
  step(w, 1 / 120, { kick: true });
  assert.ok(types(run(w, 5)).includes('goal'));
  const none = createWorld({ player: { x: 10, y: 10 }, ball: { x: 30, y: 10 } });
  assert.deepEqual(types(step(none, 1 / 120, { kick: true })), ['nokick']);
});
