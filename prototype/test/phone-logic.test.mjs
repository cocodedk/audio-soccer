// The phone test's pure logic: tilt steering, the vibration cues, the self-spoken menu, the voice plan,
// and the round lines that replace the keyboard wording.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createTilt, THRESHOLD } from '../phone/js/tilt.js';
import { createBuzz, REACH, LINED } from '../phone/js/buzz.js';
import { createMenu, OPTIONS } from '../phone/js/menu.js';
import { planVoice, voiceFacts } from '../phone/js/voicepick.js';
import { roundLines, introLines } from '../phone/js/play.js';
import { createWorld, GOAL } from '../js/world.js';
import { LANGS } from '../js/i18n/index.js';

const UP = { x: 1, y: 0 }, NONE = { x: 0, y: 0 };

test('tilt: level at the round start; past the threshold it steers, snapped; top edge down runs to the goal', () => {
  const tilt = createTilt();
  assert.deepEqual(tilt.dir(-30, 0), NONE, 'no neutral yet: no steer');
  tilt.setNeutral(25, -4); // however the player happens to hold it
  assert.deepEqual(tilt.dir(25 - (THRESHOLD - 2), -4), NONE);
  assert.deepEqual(tilt.dir(25 - 20, -1), UP);
  assert.deepEqual(tilt.dir(25 + 20, -4), { x: -1, y: 0 });
  assert.deepEqual(tilt.dir(25, 16), { x: 0, y: 1 });
  assert.deepEqual(tilt.dir(25, -24), { x: 0, y: -1 });
  assert.deepEqual(tilt.dir(null, null), NONE, 'a sensor sending nulls steers nothing');
  tilt.setNeutral(178, 0); // beta wraps from 180 to -180: a small tilt stays small
  assert.deepEqual(tilt.dir(-178, 0), NONE);
  assert.deepEqual(tilt.dir(160, 0), UP);
});

test('buzz: once when the ball comes into reach, a double buzz on lining up, never while it stays the same', () => {
  const buzz = createBuzz();
  const w = createWorld({ player: { x: 10, y: 4 }, ball: { x: 10.5, y: 4 }, hasBall: true });
  buzz.reset(w);
  assert.equal(buzz.update(w), null, 'the round starts with the ball: no buzz');
  w.hasBall = false;
  assert.equal(buzz.update(w), null);
  w.hasBall = true;
  assert.equal(buzz.update(w), REACH);
  assert.equal(buzz.update(w), null, 'still in reach: quiet');
  w.ball.y = GOAL.y;
  assert.equal(buzz.update(w), LINED);
  for (let i = 0; i < 5; i++) assert.equal(buzz.update(w), null, 'still lined up: quiet');
  w.ball.y = 12.6; // just outside the band, inside the slack: still lined up
  assert.equal(buzz.update(w), null);
  w.ball.y = 12.4;
  assert.equal(buzz.update(w), null, 'wobbling at the edge does not buzz again');
  w.ball.y = 13.5;
  assert.equal(buzz.update(w), null);
  w.ball.y = 12;
  assert.equal(buzz.update(w), LINED, 'off the line and back on is a new change');
  w.hasBall = false;
  assert.equal(buzz.update(w), null);
  w.hasBall = true; // taken back while lined up: the double buzz says both
  assert.equal(buzz.update(w), LINED);
});

test('menu: three options, swipes move and stop at the ends, double-tap chooses, a tap repeats', () => {
  assert.deepEqual(OPTIONS, ['drag', 'tilt', 'finish']);
  const m = createMenu();
  assert.equal(m.current, 'drag');
  assert.deepEqual(m.act('left'), { say: 'drag' }, 'already first: says it again');
  assert.deepEqual(m.act('right'), { say: 'tilt' });
  assert.deepEqual(m.act('right'), { say: 'finish' });
  assert.deepEqual(m.act('right'), { say: 'finish' });
  assert.deepEqual(m.act('tap'), { say: 'finish' });
  assert.deepEqual(m.act('up'), {});
  assert.deepEqual(m.act('left'), { say: 'tilt' });
  assert.deepEqual(m.act('double'), { choose: 'tilt' });
});

test('voice plan: a local voice of the language; English when Persian is missing; online voices never count', () => {
  const v = (name, lang, localService) => ({ name, lang, localService, voiceURI: name });
  const faLocal = v('Dariush', 'fa-IR', true), faNet = v('Net Fa', 'fa-IR', false);
  const enLocal = v('Local En', 'en-US', true), enNet = v('Net En', 'en-GB', false);
  assert.deepEqual(planVoice([enLocal, faNet, faLocal], 'fa'), { lang: 'fa', voice: faLocal, english: false });
  assert.deepEqual(planVoice([enNet, enLocal, faNet], 'fa'), { lang: 'en', voice: enLocal, english: true });
  assert.equal(planVoice([enNet, faNet], 'fa'), null, 'online only: nothing to speak with');
  assert.equal(planVoice([faLocal, enNet], 'en'), null, 'English chosen: a Persian voice cannot speak it');
  assert.deepEqual(planVoice([], 'fa'), { lang: 'fa', voice: null, english: false }, 'an empty list: try the default voice');
  const facts = voiceFacts([enLocal, v('Fr', 'fr-FR', true), faNet]);
  assert.deepEqual([facts.voiceCount, facts.voices.length, facts.persianLocal, facts.persianOnline], [3, 2, false, true]);
});

test('round lines: the phone never says the keyboard wording; the scheme intro says every gesture', () => {
  for (const lang of LANGS) {
    const first = roundLines(2, 1, lang);
    assert.equal(first.length, 2);
    assert.doesNotMatch(first.join(' '), /Space|فاصله/, lang);
    assert.deepEqual(roundLines(2, 2, lang).length, 1);
  }
  const en = introLines('drag', 'en').join(' ');
  for (const word of [/drag/i, /tap to kick/i, /two fingers/i, /three fingers/i]) assert.match(en, word);
  assert.match(introLines('tilt', 'en').join(' '), /tilt/i);
  assert.equal(introLines('tilt', 'fa').length, introLines('tilt', 'en').length);
});
