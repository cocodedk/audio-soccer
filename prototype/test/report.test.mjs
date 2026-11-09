import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resultsText, roundLine } from '../js/report.js';
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
});

test('results text: one header, every round, given-up and timed-out rounds included', () => {
  const l1 = { level: 1, rounds: [round(1, 'success', 8), round(1, 'giveup', 30.04), round(1, 'timeout', 120)] };
  const l2 = { level: 2, rounds: [round(2, 'success', 14.2, { kicks: 2, wide: 1 })] };
  const l3 = { level: 3, partial: true, rounds: [round(3, 'giveup', 9, { saves: 2, kicks: 3 }), round(3, 'left', 4.5)] };
  const text = resultsText([l1, l2, l3], META);
  const lines = text.split('\n');
  assert.equal(lines[0], 'Audio Soccer trial. Voice: built-in voice. Speech speed 1.3. Picture hidden.');
  assert.equal(text.match(/Speech speed/g).length, 1, 'voice settings once');
  assert.equal(lines[1], 'Level 1, Find the ball');
  assert.equal(lines[2], 'Round 1: found in 8.0 s. Start 16 m, ball ahead. Path efficiency 0.50. Asked where 2. Bumps 1.');
  assert.equal(lines[3], 'Round 2: not found, gave up at 30.0 s. Start 16 m, ball ahead. Asked where 2. Bumps 1.');
  assert.equal(lines[4], 'Round 3: not found, time up at 120.0 s. Start 16 m, ball ahead. Asked where 2. Bumps 1.');
  assert.equal(lines[5], 'Level 2, Score');
  assert.equal(lines[6], 'Round 1: scored in 14.2 s. Kicks 2. Wide 1. Asked where 2. Bumps 1.');
  assert.equal(lines[7], 'Level 3, Beat the keeper (stopped after 2 of 3 rounds)');
  assert.equal(lines[8], 'Keeper: follows the ball, stops at the kick.');
  assert.equal(lines[9], 'Round 1: not scored, gave up at 9.0 s. Kicks 3. Wide 0. Saved 2. Asked where 2. Bumps 1.');
  assert.equal(lines[10], 'Round 2: not scored, left the round at 4.5 s. Kicks 0. Wide 0. Saved 0. Asked where 2. Bumps 1.');
  assert.equal(text.match(/Keeper:/g).length, 1, 'the keeper line is level 3 only');
  assert.ok(text.length < 1200, 'short enough for an email');
  assert.match(resultsText([l1], { mode: 'sr', rate: 2, hidden: false }), /Voice: own screen reader\. Speech speed 2\.0\. Picture shown\./);
  assert.match(roundLine(1, 0, { ...l1.rounds[0], startBehind: true }), /ball behind/);
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
