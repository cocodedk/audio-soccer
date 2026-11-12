// Touch gestures of the phone test (phone/js/gestures.js), fed sequences of pointer events.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createPlayGestures, createMenuGestures, snap8, dragDir, DEAD, TAP_MS, DOUBLE_MS } from '../phone/js/gestures.js';

const UP = { x: 1, y: 0 }, NONE = { x: 0, y: 0 };
const play = () => {
  const taps = [];
  return { taps, g: createPlayGestures({ onTap: (n) => taps.push(n) }) };
};
const menu = () => {
  const got = [];
  const g = createMenuGestures({ onSwipe: (d) => got.push(d), onDoubleTap: () => got.push('double'), onTap: () => got.push('tap') });
  return { got, g };
};
// A short tap of one finger at (x, y) from t to t + ms.
const tap = (g, id, t, x = 200, y = 400, ms = 80) => { g.down(id, x, y, t); g.up(id, x + 2, y + 1, t + ms); };

test('snapping: the 8 keyboard directions, with exact zeros so a straight run kicks straight', () => {
  assert.deepEqual(snap8(1, 0.3), UP); // 17 degrees off still runs straight up the pitch
  assert.equal(Object.is(snap8(1, 0.3).y, 0), true);
  assert.deepEqual(snap8(0, -2), { x: 0, y: -1 });
  const d = snap8(1, 1);
  assert.ok(Math.abs(d.x - Math.SQRT1_2) < 1e-12 && Math.abs(d.y - Math.SQRT1_2) < 1e-12);
  assert.deepEqual(snap8(-1, 0.1), { x: -1, y: 0 });
  assert.deepEqual(snap8(0, 0), NONE);
  // Screen to pitch: up the screen (y falls) is toward the goal, right on the screen is the player's right.
  assert.deepEqual(dragDir(0, -50), UP);
  assert.deepEqual(dragDir(50, 0), { x: 0, y: 1 });
  assert.deepEqual(dragDir(-50, 0), { x: 0, y: -1 });
});

test('a drag inside the dead zone is no steer, beyond it steers from where the finger landed, lifting stops', () => {
  const { g, taps } = play();
  g.down(1, 200, 400, 0);
  g.move(1, 200, 400 - (DEAD - 4), 30);
  assert.deepEqual(g.steer(), NONE);
  g.move(1, 210, 340, 60);
  assert.deepEqual(g.steer(), UP);
  g.move(1, 260, 400, 400); // the direction is measured from the landing point, not the last move
  assert.deepEqual(g.steer(), { x: 0, y: 1 });
  g.move(1, 205, 402, 450); // back inside the dead zone: stands still but still the steering finger
  assert.deepEqual(g.steer(), NONE);
  g.move(1, 200, 300, 500);
  assert.deepEqual(g.steer(), UP);
  g.up(1, 200, 300, 600);
  assert.deepEqual(g.steer(), NONE);
  assert.deepEqual(taps, [], 'a drag is never a kick');
});

test('a tap while another finger steers is a kick, and the steering goes on', () => {
  const { g, taps } = play();
  g.down(1, 200, 500, 0);
  g.move(1, 200, 420, 50);
  tap(g, 2, 500, 100, 200);
  assert.deepEqual(taps, [1]);
  assert.deepEqual(g.steer(), UP);
  tap(g, 3, 900, 300, 200); // and again
  assert.deepEqual(taps, [1, 1]);
  assert.deepEqual(g.steer(), UP);
});

test('two fingers tapping together are one "where", not two kicks; three fingers are "back"', () => {
  const { g, taps } = play();
  g.down(1, 100, 400, 0); g.down(2, 200, 400, 40);
  g.up(1, 100, 400, 110);
  assert.deepEqual(taps, [], 'nothing until both fingers are up');
  g.up(2, 200, 400, 130);
  assert.deepEqual(taps, [2]);
  g.down(3, 100, 400, 1000); g.down(4, 200, 400, 1020); g.down(5, 300, 400, 1050);
  g.up(4, 200, 400, 1120); g.up(3, 100, 400, 1130); g.up(5, 300, 400, 1140);
  assert.deepEqual(taps, [2, 3]);
});

test('a two-finger tap while a third finger steers is "where", and the steering goes on', () => {
  const { g, taps } = play();
  g.down(1, 200, 500, 0); g.move(1, 260, 500, 40);
  g.down(2, 100, 200, 300); g.down(3, 150, 200, 330);
  g.up(2, 100, 200, 400); g.up(3, 150, 200, 410);
  assert.deepEqual(taps, [2]);
  assert.deepEqual(g.steer(), { x: 0, y: 1 });
});

test('quick taps one after the other are separate kicks, not a two-finger tap', () => {
  const { g, taps } = play();
  tap(g, 1, 0); tap(g, 2, 200); tap(g, 1, 400);
  assert.deepEqual(taps, [1, 1, 1]);
});

test('a finger held still, or lifted after moving, is not a tap; a cancelled finger stops steering', () => {
  const { g, taps } = play();
  g.down(1, 200, 400, 0);
  g.tick(TAP_MS + 50);
  g.up(1, 200, 400, TAP_MS + 100);
  g.down(2, 200, 400, 1000); g.move(2, 200, 360, 1050); g.up(2, 200, 360, 1100);
  assert.deepEqual(taps, []);
  g.down(3, 200, 400, 2000); g.move(3, 200, 300, 2050);
  assert.deepEqual(g.steer(), UP);
  g.cancel(3);
  assert.deepEqual(g.steer(), NONE);
  assert.deepEqual(taps, []);
});

test('a finger that rests first can still become the steering finger', () => {
  const { g } = play();
  g.down(1, 200, 400, 0);
  g.tick(TAP_MS * 2);
  g.move(1, 200, 340, TAP_MS * 2 + 20);
  assert.deepEqual(g.steer(), UP);
});

test('menu: swipes by their main axis; a slow short press is nothing', () => {
  const { g, got } = menu();
  g.down(1, 100, 400, 0); g.up(1, 220, 430, 150);
  g.down(1, 300, 400, 1000); g.up(1, 180, 380, 1150);
  g.down(1, 200, 500, 2000); g.up(1, 210, 380, 2150);
  g.down(1, 200, 400, 3000); g.up(1, 200, 400, 3800);
  g.tick(5000);
  assert.deepEqual(got, ['right', 'left', 'up']);
});

test('menu: two quick taps are a double-tap; one tap alone is a tap once the window has passed', () => {
  const { g, got } = menu();
  tap(g, 1, 0);
  g.tick(100);
  assert.deepEqual(got, [], 'a single tap waits: it may be the first of two');
  tap(g, 1, 80 + DOUBLE_MS - 50);
  assert.deepEqual(got, ['double']);
  g.tick(2000);
  assert.deepEqual(got, ['double'], 'the double-tap used both taps');
  tap(g, 1, 3000);
  g.tick(3080 + DOUBLE_MS + 10);
  assert.deepEqual(got, ['double', 'tap']);
  tap(g, 1, 5000); tap(g, 1, 5080 + DOUBLE_MS + 100); // too far apart: two taps
  g.tick(7000);
  assert.deepEqual(got, ['double', 'tap', 'tap', 'tap']);
});

test('menu: a swipe after a tap is a swipe, and a stroke with a second finger on the screen is ignored', () => {
  const { g, got } = menu();
  tap(g, 1, 0);
  g.down(1, 100, 400, 150); g.up(1, 250, 400, 250);
  g.tick(1000);
  assert.deepEqual(got, ['right']);
  g.down(1, 100, 400, 2000); g.down(2, 100, 500, 2010);
  g.up(1, 250, 400, 2100); g.up(2, 250, 500, 2110);
  g.tick(3000);
  assert.deepEqual(got, ['right']);
});
