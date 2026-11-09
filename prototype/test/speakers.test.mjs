import { test } from 'node:test';
import assert from 'node:assert/strict';
import { distanceGain, beaconEvery, sideWord, createSideCaller, SIDE_GAP } from '../js/speakers.js';
import { guideText, SHOT_RANGE } from '../js/world.js';
import { getSoundMode, speakersOn, setSoundMode, SOUND_MODES } from '../js/soundmode.js';
import { close } from './helpers.mjs';

test('sound mode: speakers by default; headphones is the other choice; junk is ignored', () => {
  assert.deepEqual(SOUND_MODES, ['speakers', 'headphones']);
  assert.equal(getSoundMode(), 'speakers');
  assert.equal(speakersOn(), true);
  assert.equal(setSoundMode('surround'), 'speakers');
  assert.equal(setSoundMode('headphones'), 'headphones');
  assert.equal(speakersOn(), false);
  setSoundMode('speakers');
});

test('distance gain: 1 within 2 m, a gentle fall, never below 0.3 so a far ball stays audible', () => {
  for (const d of [0, 1, 2]) close(distanceGain(d), 1);
  close(distanceGain(12), 0.4);
  close(distanceGain(20), 0.3);
  close(distanceGain(60), 0.3);
  close(distanceGain(500), 0.3);
  let prev = 2;
  for (let d = 0; d <= 70; d += 0.5) { assert.ok(distanceGain(d) <= prev); prev = distanceGain(d); }
});

test('goal beacon interval: 2 s from 30 m, 0.5 s within 6 m, linear between', () => {
  for (const d of [30, 45, 60]) close(beaconEvery(d), 2);
  for (const d of [0, 3, 6]) close(beaconEvery(d), 0.5);
  close(beaconEvery(18), 1.25);
  let prev = 0;
  for (let d = 0; d <= 40; d += 0.5) { assert.ok(beaconEvery(d) >= prev); prev = beaconEvery(d); }
});

test('side word for the ball: ahead within 1.5 m of your line, left or right beyond, behind past 1 m back', () => {
  const me = { x: 20, y: 10 };
  const word = (dx, dy) => sideWord(me, { x: me.x + dx, y: me.y + dy }, false);
  assert.equal(word(10, 0), 'ahead');
  assert.equal(word(10, 1.5), 'ahead');
  assert.equal(word(10, -1.5), 'ahead');
  assert.equal(word(10, 1.6), 'right');
  assert.equal(word(10, -1.6), 'left');
  assert.equal(word(0, -8), 'left');
  assert.equal(word(0, 8), 'right');
  assert.equal(word(-1, 0), 'ahead', 'exactly 1 m back is not behind yet');
  assert.equal(word(-1.1, 0), 'behind');
  assert.equal(word(-10, 6), 'behind');
});

test('side word for the goal: ahead exactly when guideText says straight ahead (inside the shot range)', () => {
  for (const x of [20, 25, 30, 35]) {
    for (let y = 0.5; y <= 19.5; y += 0.25) {
      const ball = { x, y }, text = guideText({ x: x - 0.5, y }, ball), word = sideWord({ x: x - 0.5, y }, ball, true);
      if (40 - x <= SHOT_RANGE) assert.equal(word === 'ahead', /straight ahead/.test(text), `${x},${y} ${text}`);
      assert.ok(['ahead', 'left', 'right'].includes(word), 'the goal is never behind');
      if (word === 'left') assert.ok(y > 10);
      if (word === 'right') assert.ok(y < 10);
    }
  }
  assert.equal(sideWord({ x: 5, y: 10 }, { x: 5.5, y: 10 }, true), 'ahead', 'far from the goal, lined up is still ahead');
});

test('side caller: one word per 1.5 s, never the same word twice in a row', () => {
  const c = createSideCaller();
  assert.equal(c.next('left', 0), 'left', 'the first call goes at once');
  assert.equal(c.next('left', 100), 'left', 'not yet spoken, so still offered');
  c.spoke('left', 100);
  assert.equal(c.next('right', 100 + SIDE_GAP - 1), null, 'too soon');
  assert.equal(c.next('right', 100 + SIDE_GAP), 'right');
  assert.equal(c.next('left', 100 + SIDE_GAP), null, 'the same word again');
  assert.equal(c.next('left', 99999), null, 'never repeated, however long ago');
  c.spoke('right', 2000);
  assert.equal(c.next('left', 4000), 'left', 'the side changed back');
  c.reset();
  assert.equal(c.next('left', 4001), 'left', 'a new round starts afresh');
});
