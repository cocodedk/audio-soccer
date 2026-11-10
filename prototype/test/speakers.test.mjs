import { test } from 'node:test';
import assert from 'node:assert/strict';
import { distanceGain, beaconEvery, sideWord, nearBoard, createSideCaller, SIDE_GAP } from '../js/speakers.js';
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

test('side word for the ball: after a side word, ahead only within 0.3 m of your line', () => {
  const me = { x: 20, y: 10 };
  for (const heard of [null, 'left', 'right', 'behind']) {
    const word = (dx, dy) => sideWord(me, { x: me.x + dx, y: me.y + dy }, false, heard);
    assert.equal(word(10, 0.29), 'ahead');
    assert.equal(word(10, -0.29), 'ahead');
    assert.equal(word(10, 0.4), 'right', 'the wide band holds only once ahead is heard');
    assert.equal(word(1, -0.4), 'left');
    assert.equal(word(-0.5, 0), 'ahead', 'half a metre back is in reach, not behind');
    assert.equal(word(-0.6, 0), 'behind');
  }
});

test('side word for the ball, once ahead is heard: ahead within 1.5 m far off, 0.5 m close by; behind past 1 m back (0.5 m close)', () => {
  const me = { x: 20, y: 10 };
  const word = (dx, dy) => sideWord(me, { x: me.x + dx, y: me.y + dy }, false, 'ahead');
  assert.equal(word(10, 0), 'ahead');
  assert.equal(word(10, 1.5), 'ahead');
  assert.equal(word(10, -1.5), 'ahead');
  assert.equal(word(10, 1.6), 'right');
  assert.equal(word(10, -1.6), 'left');
  assert.equal(word(4, 1), 'ahead', 'the band narrows linearly: 1 m wide at 4 m');
  assert.equal(word(4, 1.1), 'right');
  assert.equal(word(1, 0.5), 'ahead');
  assert.equal(word(1, 0.6), 'right', 'close by, a ball the take distance off your line is to the side');
  assert.equal(word(0.5, -0.6), 'left');
  assert.equal(word(0, -8), 'left');
  assert.equal(word(0, 8), 'right');
  assert.equal(word(-0.5, 0), 'ahead', 'half a metre back is in reach, not behind');
  assert.equal(word(-0.6, 0), 'behind', 'close by, behind starts at 0.5 m');
  assert.equal(word(-10, 6), 'behind');
  assert.equal(word(-1.1, 0), 'behind');
});

test('side word for the goal: ahead exactly when a straight kick lines up, at any distance', () => {
  for (const x of [5, 10, 15, 20, 25, 30, 35]) {
    for (let y = 0.5; y <= 19.5; y += 0.25) {
      const ball = { x, y }, text = guideText({ x: x - 0.5, y }, ball), word = sideWord({ x: x - 0.5, y }, ball, true);
      if (40 - x <= SHOT_RANGE) assert.equal(word === 'ahead', /straight ahead/.test(text), `${x},${y} ${text}`);
      else assert.match(text, /too far to shoot/, 'beyond range W says so, whatever the word');
      assert.ok(['ahead', 'left', 'right'].includes(word), 'the goal is never behind, and always has a word');
      if (word === 'left') assert.ok(y > 10);
      if (word === 'right') assert.ok(y < 10);
    }
  }
  assert.equal(sideWord({ x: 5, y: 10 }, { x: 5.5, y: 10 }, true), 'ahead', 'far off and lined up: ahead stops a sideways run');
  assert.equal(sideWord({ x: 5, y: 3 }, { x: 5.5, y: 3 }, true), 'right', 'far off, the sides still steer');
});

test('near board: within BOARD_NEAR of the left or right board, else none', () => {
  assert.equal(nearBoard({ y: 1.4 }), 'left');
  assert.equal(nearBoard({ y: 0.3 }), 'left');
  assert.equal(nearBoard({ y: 1.5 }), null);
  assert.equal(nearBoard({ y: 10 }), null);
  assert.equal(nearBoard({ y: 18.6 }), 'right');
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
  assert.equal(c.heard(), null);
  c.spoke('left', 5000);
  assert.equal(c.heard(), 'left');
  assert.equal(c.next('ahead', 5001), 'ahead', 'ahead, the word to stop stepping sideways, skips the gap');
  c.spoke('ahead', 5001);
  assert.equal(c.next('ahead', 9999), null, 'but is not repeated either');
});
