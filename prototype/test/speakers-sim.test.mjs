// A simulated player who obeys the speakers-mode side calls, stepping the real world: following the words
// alone must always reach a still ball (no spot where the word says 'ahead' but running ahead misses it).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, step } from '../js/world.js';
import { sideWord, createSideCaller } from '../js/speakers.js';
import { goalSide } from '../js/ball.js';
import { PITCH_L } from '../js/constants.js';
import { SHOT_RANGE } from '../js/describe.js';

const RUN = { ahead: { x: 1, y: 0 }, behind: { x: -1, y: 0 }, left: { x: 0, y: -1 }, right: { x: 0, y: 1 } };
const DT = 1 / 120;

// As in the game (cues.js): the words go through createSideCaller, one per SIDE_GAP and never the same
// twice, and each is heard at once (a clip). The player stands until the first word, then keeps running
// the way the last word he heard says. Returns { secs, words } once the ball is taken, or null.
function chase(start, ball, secs = 30) {
  const w = createWorld({ player: start, ball }), caller = createSideCaller();
  let dir, words = 0;
  for (let t = 0; t < secs; t += DT) {
    const word = caller.next(sideWord(w.player, w.ball, false, caller.heard()), t * 1000);
    if (word) { caller.spoke(word, t * 1000); dir = RUN[word]; words++; }
    step(w, DT, { dir });
    if (w.hasBall) return { secs: t, words };
  }
  return null;
}

// The ideal player who hears every change the moment it happens: an extra check on sideWord alone.
function chaseInstant(start, ball, secs = 30) {
  const w = createWorld({ player: start, ball });
  let heard = null;
  for (let t = 0; t < secs; t += DT) {
    heard = sideWord(w.player, w.ball, false, heard);
    step(w, DT, { dir: RUN[heard] });
    if (w.hasBall) return t;
  }
  return null;
}

const start = { x: 10, y: 10 };
const offsets = [];
for (let dx = -8; dx <= 20; dx += 0.7) for (let dy = -8; dy <= 8; dy += 0.3) offsets.push([dx, dy]);

test('obeying the side calls as the caller lets them through takes the ball, without flapping', () => {
  const dead = chase(start, { x: 20, y: 11.2 });
  assert.notEqual(dead, null, 'the reported dead zone');
  assert.ok(dead.words <= 3, `ahead, right, ahead: ${dead.words} words`);
  assert.notEqual(chase(start, { x: 20, y: 8.8 }), null);
  assert.notEqual(chase(start, { x: 9.2, y: 10 }), null, 'just under 1 m behind');
  for (const [dx, dy] of offsets) {
    const r = chase(start, { x: start.x + dx, y: start.y + dy });
    assert.notEqual(r, null, `ball at ${dx.toFixed(1)}, ${dy.toFixed(1)}`);
    assert.ok(r.words <= 3, `ball at ${dx.toFixed(1)}, ${dy.toFixed(1)}: ${r.words} words`);
  }
});

test('with every word heard at once, obeying them takes the ball too', () => {
  assert.notEqual(chaseInstant(start, { x: 20, y: 11.2 }), null, 'the reported dead zone');
  for (const [dx, dy] of offsets) {
    assert.notEqual(chaseInstant(start, { x: start.x + dx, y: start.y + dy }), null, `ball at ${dx.toFixed(1)}, ${dy.toFixed(1)}`);
  }
});

// With the ball, from where level 2 and 3 rounds start (player x 4 to 16, any y, all beyond SHOT_RANGE):
// obeying the calls for the goal must bring the ball into range lined up with the goal. 'ahead' has to come
// out of range too, or a sideways run never stops (right, left, right... for the whole round).
function steer(start, secs = 20) {
  const w = createWorld({ player: start, ball: { x: start.x + 0.5, y: start.y }, hasBall: true });
  const caller = createSideCaller();
  let dir;
  for (let t = 0; t < secs; t += DT) {
    const word = caller.next(sideWord(w.player, w.ball, true, caller.heard()), t * 1000);
    if (word) { caller.spoke(word, t * 1000); dir = RUN[word]; }
    step(w, DT, { dir });
    if (!w.hasBall) return null;
    if (PITCH_L - w.ball.x <= SHOT_RANGE && goalSide(w.ball.y) === 'ahead') return t;
  }
  return null;
}

test('with the ball, obeying the calls from a level 2 or 3 start brings it into range lined up', () => {
  for (let x = 4; x <= 16; x += 2) for (let y = 2; y <= 18; y += 1) {
    assert.notEqual(steer({ x, y }), null, `start ${x}, ${y}`);
  }
});
