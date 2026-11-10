import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resultsText, roundLine, settingsLine, browserName, isoMinute } from '../js/report.js';
import { newStats, closeStats, summarySpeech } from '../js/levels.js';
import { tickEvery, ballOpts } from '../js/soundscape.js';
import { pickVoices, stepRate, RATES, DEFAULT_RATE } from '../js/voices.js';
import { loadPref, savePref } from '../js/storage.js';
import { close } from './helpers.mjs';

const setup = { player: { x: 10, y: 10 }, ball: { x: 10, y: 26 } };
const round = (level, how, time, extra = {}) => ({ ...closeStats(level, { ...newStats(setup), time, where: 2, bumps: 1, ...extra }, how) });
const META = { mode: 'voice', rate: 1.3, hidden: true };

test('closeStats: found, gave up, timed out and left; efficiency = start distance / (4 x time), level 1 only', () => {
  const found = round(1, 'success', 8);
  assert.deepEqual([found.success, found.gaveUp, found.timedOut], [true, false, false]);
  close(found.efficiency, 16 / (4 * 8));
  const gave = round(1, 'giveup', 30), late = round(1, 'timeout', 120);
  assert.deepEqual([gave.success, gave.gaveUp, gave.timedOut, gave.efficiency], [false, true, false, null]);
  assert.deepEqual([late.success, late.gaveUp, late.timedOut, late.efficiency], [false, true, true, null]);
  const left = round(1, 'left', 6);
  assert.deepEqual([left.success, left.gaveUp, left.timedOut, left.left, left.efficiency], [false, true, false, true, null]);
  assert.equal(found.left, false);
  assert.equal(round(2, 'success', 8).efficiency, null);
  assert.equal(round(1, 'success', 0).efficiency, null, 'no division by zero');
});

test('newStats records the start distance and whether the ball began behind', () => {
  assert.equal(newStats(setup).startBehind, false);
  close(newStats(setup).startDist, 16);
  assert.equal(newStats({ player: { x: 20, y: 5 }, ball: { x: 12, y: 5 } }).startBehind, true);
  const s = newStats(setup);
  for (const k of ['time', 'where', 'bumps', 'kicks', 'wide', 'saves']) assert.equal(s[k], 0, k);
  assert.deepEqual(s.shots, []);
});

test('results text: a browser header, each level with its own settings and time, every round', () => {
  const l1 = { level: 1, meta: META, at: '2026-11-09T14:05Z', rounds: [round(1, 'success', 8), round(1, 'giveup', 30.04), round(1, 'timeout', 120)] };
  const shots = [{ dist: 18, offset: 2.1, aim: 'straight', outcome: 'wide right' }, { dist: 9.2, offset: -0.3, aim: 'left', outcome: 'goal' }];
  const l2 = { level: 2, meta: { ...META, sound: 'headphones', lang: 'fa' }, at: '2026-11-09T14:12Z', rounds: [round(2, 'success', 14.2, { kicks: 2, wide: 1, shots })] };
  const l3 = { level: 3, partial: true, rounds: [round(3, 'giveup', 9, { saves: 2, kicks: 3 }), round(3, 'left', 4.5)] };
  const ua = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36';
  const text = resultsText([l1, l2, l3], ua);
  const lines = text.split('\n');
  assert.equal(lines[0], 'Audio Soccer trial. Browser: (X11; Linux x86_64) AppleWebKit/537.36 Chrome/130.0.0.0 Safari/537.36.');
  assert.equal(lines[1], 'Level 1, Find the ball');
  assert.equal(lines[2], 'Ended 2026-11-09T14:05Z. Voice: built-in voice. Speech speed 1.3. Picture hidden.');
  assert.equal(lines[3], 'Round 1: found in 8.0 s. Start 16 m, ball ahead. Path efficiency 0.50. Asked where 2. Bumps 1.');
  assert.equal(lines[4], 'Round 2: not found, gave up at 30.0 s. Start 16 m, ball ahead. Asked where 2. Bumps 1.');
  assert.equal(lines[5], 'Round 3: not found, time up at 120.0 s. Start 16 m, ball ahead. Asked where 2. Bumps 1.');
  assert.equal(lines[6], 'Level 2, Score');
  assert.equal(lines[7], 'Ended 2026-11-09T14:12Z. Voice: built-in voice. Speech speed 1.3. Picture hidden. Sound: headphones. Language: Persian.');
  assert.equal(lines[8], 'Round 1: scored in 14.2 s. Kicks 2: 18 m, 2.1 m right, straight, wide right; 9 m, 0.3 m left, left, goal. Wide 1. Asked where 2. Bumps 1.');
  assert.equal(lines[9], 'Level 3, Beat the keeper (stopped after 2 of 3 rounds)', 'an old block with no settings or time adds no line');
  assert.equal(lines[10], 'Keeper: follows the ball, stops at the kick.');
  assert.equal(lines[11], 'Round 1: not scored, gave up at 9.0 s. Kicks 3. Wide 0. Saved 2. Asked where 2. Bumps 1.');
  assert.equal(lines[12], 'Round 2: not scored, left the round at 4.5 s. Kicks 0. Wide 0. Saved 0. Asked where 2. Bumps 1.');
  assert.equal(text.match(/Keeper:/g).length, 1, 'the keeper line is level 3 only');
  assert.ok(text.length < 1500, 'short enough for an email');
  assert.match(settingsLine({ mode: 'sr', rate: 2, hidden: false }), /^Voice: own screen reader\. Speech speed 2\.0\. Picture shown\.$/);
  assert.match(roundLine(1, 0, { ...l1.rounds[0], startBehind: true }), /ball behind/);
  assert.equal(browserName(''), 'unknown');
  assert.ok(browserName('x'.repeat(400)).length <= 160);
  assert.equal(isoMinute(new Date(Date.UTC(2026, 10, 9, 14, 5, 59))), '2026-11-09T14:05Z');
});

test('level summary speech names missed rounds', () => {
  const rounds = [round(1, 'success', 12.44), round(1, 'giveup', 40), round(1, 'success', 10)];
  assert.equal(summarySpeech(1, rounds), 'Level 1 done. Times: 12.4, not found, 10.0.');
  assert.equal(summarySpeech(2, [round(2, 'giveup', 5)], true), 'Level 2 stopped after 1 of 3 rounds. Times: not scored.');
});

test('still-ball tick interval: 0.8 s from 20 m, 0.2 s within 3 m, linear between', () => {
  for (const d of [20, 25, 60]) close(tickEvery(d), 0.8);
  for (const d of [0, 1, 3]) close(tickEvery(d), 0.2);
  close(tickEvery(11.5), 0.5);
  let prev = 0;
  for (let d = 0; d <= 25; d += 0.5) { assert.ok(tickEvery(d) >= prev); prev = tickEvery(d); }
});

test('ball sounds behind the listener are dull, and louder to make up for it', () => {
  const ahead = ballOpts(20, { x: 25, y: 10 }, 1.2, 2), behind = ballOpts(20, { x: 15, y: 10 }, 1.2, 2);
  assert.deepEqual([ahead.dull, ahead.gain, behind.dull, behind.gain], [false, 1.2, true, 2.4]);
  assert.equal(ballOpts(20, { x: 19.5, y: 10 }, 1, 2).dull, false, 'beside you is not behind');
});

test('voices: English only, local first, then British; rate steps stay inside the list', () => {
  const v = (name, lang, localService) => ({ name, lang, localService, voiceURI: name });
  const list = pickVoices([v('Net US', 'en-US', false), v('Fr', 'fr-FR', true), v('Local US', 'en-US', true), v('Net GB', 'en-GB', false), v('Local GB', 'en_GB', true)], 'en');
  assert.deepEqual(list.map((x) => x.name), ['Local GB', 'Local US', 'Net GB', 'Net US']);
  assert.deepEqual(RATES, [1.0, 1.3, 1.6, 2.0, 2.5]);
  assert.equal(DEFAULT_RATE, 1.3);
  assert.equal(stepRate(1.3, 1), 1.6);
  assert.equal(stepRate(1.3, -1), 1.0);
  assert.equal(stepRate(1.0, -1), 1.0);
  assert.equal(stepRate(2.5, 1), 2.5);
  assert.equal(stepRate(1.4, 1), 1.6, 'an unknown rate steps from the default');
});

test('storage never throws when localStorage is missing or blocked', () => {
  assert.equal(loadPref('nothing', 'fallback'), 'fallback');
  assert.doesNotThrow(() => savePref('x', 1));
});
