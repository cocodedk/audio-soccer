import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  arrowsToDir, toAudio, clockHour, clockText, createWorld, step, lineOutcome, guideText, whereText,
  PLAYER_RADIUS, KICK_SPEED,
} from '../js/world.js';
import { close, run, types } from './helpers.mjs';

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

test('clock text: "2 o\'clock, 3 metres" and "at your feet" under the 0.8 m take distance', () => {
  const a = Math.PI / 3; // 60 degrees right of ahead
  assert.equal(clockText(3 * Math.cos(a), 3 * Math.sin(a)), "2 o'clock, 3 metres");
  assert.equal(clockText(0, 1.2), "3 o'clock, 1 metre");
  assert.equal(clockText(0.5, 0.5), 'at your feet');
  assert.equal(clockText(0, 0.75), 'at your feet');
  assert.equal(clockText(0, 0.85), "3 o'clock, 1 metre", 'a ball you cannot yet take is not at your feet');
});

test('where-am-I and the goal guide read naturally, goal distance and side in words', () => {
  const w = createWorld({ player: { x: 18, y: 2 }, ball: { x: 19.5, y: 4.6 } });
  assert.equal(whereText(w), "Middle third, by the left board. Ball 2 o'clock, 3 metres. Goal, 23 metres, slightly right.");
  const k = createWorld({ player: { x: 30, y: 10 }, ball: { x: 30.3, y: 10 }, keeper: true });
  assert.equal(whereText(k), "Attacking third, centre. Ball at your feet. Goal, 10 metres, straight ahead. Keeper 12 o'clock, 10 metres.");
  assert.equal(guideText({ x: 31, y: 10 }), 'Goal, 9 metres, straight ahead');
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
