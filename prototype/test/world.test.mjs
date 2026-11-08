import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  arrowsToDir, toAudio, clockHour, clockText, createWorld, step, lineOutcome, guideText, whereText,
  PLAYER_RADIUS, KICK_SPEED, DRIBBLE_DIST,
} from '../js/world.js';

const close = (a, b, eps = 1e-9) => assert.ok(Math.abs(a - b) < eps, `${a} != ${b}`);
const run = (w, secs, input = {}, dt = 1 / 120) => {
  const events = [];
  for (let t = 0; t < secs; t += dt) events.push(...step(w, dt, input));
  return events;
};
const types = (events) => events.map((e) => e.type);

test('arrows map to pitch directions', () => {
  assert.deepEqual(arrowsToDir(['ArrowUp']), { x: 1, y: 0 });
  assert.deepEqual(arrowsToDir(['ArrowDown']), { x: -1, y: 0 });
  assert.deepEqual(arrowsToDir(['ArrowLeft']), { x: 0, y: -1 });
  assert.deepEqual(arrowsToDir(['ArrowRight']), { x: 0, y: 1 });
  assert.deepEqual(arrowsToDir([]), { x: 0, y: 0 });
  const d = arrowsToDir(new Set(['ArrowUp', 'ArrowLeft']));
  close(Math.hypot(d.x, d.y), 1);
  assert.ok(d.x > 0 && d.y < 0);
});

test('ArrowLeft decreases y and ArrowUp increases x at 4 m/s', () => {
  const w = createWorld({ player: { x: 20, y: 10 }, ball: { x: 5, y: 5 } });
  run(w, 0.5, { dir: arrowsToDir(['ArrowLeft']) });
  close(w.player.y, 8, 0.05);
  close(w.player.x, 20);
  run(w, 0.5, { dir: arrowsToDir(['ArrowUp']) });
  close(w.player.x, 22, 0.05);
});

test('audio mapping: a point on the player\'s left has a smaller audio X', () => {
  const player = { x: 20, y: 10 };
  const left = { x: 20, y: 5 }, ahead = { x: 30, y: 10 };
  assert.ok(toAudio(left).x < toAudio(player).x);
  assert.ok(toAudio(ahead).z < toAudio(player).z, 'ahead is further along -Z (the listener\'s forward)');
  assert.deepEqual(toAudio({ x: 3, y: 7 }), { x: 7, y: 0, z: -3 });
});

test('clock positions: ahead 12, right 3, behind 6, left 9', () => {
  assert.equal(clockHour(5, 0), 12);
  assert.equal(clockHour(0, 5), 3);
  assert.equal(clockHour(-5, 0), 6);
  assert.equal(clockHour(0, -5), 9);
  assert.ok([10, 11].includes(clockHour(5, -5)), '45 degrees left is half past 10');
});

test('clock text: "2 o\'clock, 3 metres" and "at your feet" under 1 m', () => {
  const a = Math.PI / 3; // 60 degrees right of ahead
  assert.equal(clockText(3 * Math.cos(a), 3 * Math.sin(a)), "2 o'clock, 3 metres");
  assert.equal(clockText(0, 1.2), "3 o'clock, 1 metre");
  assert.equal(clockText(0.5, 0.5), 'at your feet');
});

test('where-am-I and the goal guide read naturally', () => {
  const w = createWorld({ player: { x: 18, y: 2 }, ball: { x: 19.5, y: 4.6 } });
  assert.equal(whereText(w), "Middle third, near the left board. Ball at 2 o'clock, 3 metres. Goal at 1 o'clock, 23 metres.");
  assert.equal(guideText({ x: 31, y: 10 }), 'Goal, 9 metres, straight ahead');
  assert.equal(guideText({ x: 31, y: 12 }), 'Goal, 9 metres, slightly left');
  assert.equal(guideText({ x: 34, y: 2 }), 'Goal, 10 metres, right');
});

test('side boards clamp the player and bump once per contact', () => {
  const w = createWorld({ player: { x: 20, y: 1 }, ball: { x: 5, y: 15 } });
  const ev = run(w, 1, { dir: arrowsToDir(['ArrowLeft']) });
  close(w.player.y, PLAYER_RADIUS);
  assert.deepEqual(ev.filter((e) => e.type === 'bump').map((e) => [e.side, e.y]), [['left', 0]]);
  const ev2 = run(w, 6, { dir: arrowsToDir(['ArrowRight']) });
  close(w.player.y, 20 - PLAYER_RADIUS);
  assert.deepEqual(ev2.filter((e) => e.type === 'bump').map((e) => e.side), ['right']);
  const ev3 = run(w, 6, { dir: arrowsToDir(['ArrowUp']) });
  close(w.player.x, 40 - PLAYER_RADIUS);
  assert.ok(ev3.some((e) => e.type === 'bump' && e.side === 'end' && e.x === 40));
});

test('the ball knocks off a side board and stays on the pitch', () => {
  const w = createWorld({ player: { x: 2, y: 18 }, ball: { x: 20, y: 2 } });
  w.ball.vy = -8;
  const ev = run(w, 1);
  assert.ok(ev.some((e) => e.type === 'board' && e.side === 'left'));
  assert.ok(w.ball.y >= 0 && w.ball.vy >= 0);
});

test('line outcome: goal between the posts, post within 0.25 m, wall outside', () => {
  assert.equal(lineOutcome(10), 'goal');
  assert.equal(lineOutcome(8.8), 'goal');
  assert.equal(lineOutcome(8.5), 'post');
  assert.equal(lineOutcome(8.3), 'post');
  assert.equal(lineOutcome(11.7), 'post');
  assert.equal(lineOutcome(8.2), 'wall');
  assert.equal(lineOutcome(12), 'wall');
});

const shoot = (y, keeper = false) => {
  const w = createWorld({ player: { x: 35, y }, ball: { x: 35.5, y }, hasBall: true, keeper });
  Object.assign(w.ball, { vx: KICK_SPEED, vy: 0, shot: true }); w.hasBall = false; // straight along x
  const ev = run(w, 2);
  return { w, ev };
};

test('a shot between the posts is a goal', () => {
  const { w, ev } = shoot(10);
  assert.ok(types(ev).includes('goal'));
  assert.equal(w.scored, true);
});

test('a shot at a post clangs and bounces back', () => {
  const { w, ev } = shoot(8.45);
  assert.ok(types(ev).includes('post'));
  assert.ok(!types(ev).includes('goal'));
  assert.ok(w.ball.x < 40);
});

test('a shot outside the posts knocks off the end wall and is called wide', () => {
  const { ev } = shoot(5);
  const wall = ev.find((e) => e.type === 'endwall');
  assert.equal(wall.wide, 'left');
  assert.equal(shoot(16).ev.find((e) => e.type === 'endwall').wide, 'right');
});

test('the keeper saves a shot within reach and shouts voy', () => {
  const { w, ev } = shoot(10, true);
  assert.ok(types(ev).includes('voy'));
  assert.ok(types(ev).includes('save'));
  assert.equal(w.scored, false);
  assert.ok(w.ball.vx <= 0);
});

test('Space shoots at the goal in the attacking third, inside a post with Left or Right, never backward', () => {
  const shot = (keys, x = 30) => { const w = createWorld({ player: { x, y: 4 }, ball: { x: x + 0.5, y: 4 }, hasBall: true });
    step(w, 1 / 120, { kick: true, dir: arrowsToDir(keys) }); return w.ball; };
  const slope = (b) => b.vy / b.vx, mid = shot([]), back = shot(['ArrowDown']), left = shot(['ArrowLeft']), right = shot(['ArrowRight']);
  close(slope(mid), 6 / 9.5, 0.01); close(slope(back), slope(mid), 0.01);
  assert.ok(back.vx > 0 && left.vx > 0 && slope(left) < slope(mid) && slope(right) > slope(mid));
  close(Math.hypot(right.vx, right.vy), KICK_SPEED, 0.1);
  const far = shot(['ArrowRight'], 10); close(far.vy, 0); assert.ok(far.vx > 13, 'further out: straight up the pitch');
  const w3 = createWorld({ player: { x: 10, y: 10 }, ball: { x: 30, y: 10 } });
  assert.deepEqual(types(step(w3, 1 / 120, { kick: true })), ['nokick']);
});

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

test('dribble, stop for 1 s, then Space with no arrows kicks at the goal', () => {
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

test('a kick still carries: straight from 16 m out it reaches the goal', () => {
  const w = createWorld({ player: { x: 23.5, y: 10 }, ball: { x: 24, y: 10 }, hasBall: true });
  step(w, 1 / 120, { kick: true });
  assert.ok(types(run(w, 5)).includes('goal'));
});
