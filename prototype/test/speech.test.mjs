import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { fakeSpeech } from './helpers.mjs';

const { spoken, finish, finishAll } = fakeSpeech();

const { say, cancel, isSaying, setMode } = await import('../js/speech.js');
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

beforeEach(async () => { setMode('voice'); cancel(); spoken.length = 0; await wait(40); });

const help = (t) => say(t, { key: 'help', many: true, wait: 30000 });

test('help lines share one key and all stay queued; an ordinary cue does not wipe them', () => {
  ['h1', 'h2', 'h3'].forEach(help);
  assert.equal(isSaying('help'), true);
  say('Ball behind you', { key: 'behind', gap: 4000, cue: true });
  say('Middle third', { cue: true });
  finishAll();
  assert.deepEqual(spoken.slice(0, 3), ['h1', 'h2', 'h3']);
  assert.equal(isSaying('help'), false);
});

test('an event (high priority) cuts help off and drops the rest of it', async () => {
  ['h1', 'h2', 'h3'].forEach(help);
  say('Got it.', { priority: 'high' });
  await wait(60); // a speak right after a cancel is held back 30 ms
  finishAll();
  assert.equal(spoken[spoken.length - 1], 'Got it.');
  assert.ok(!spoken.includes('h3'));
});

test('pressing H again while help is going: cancel() and say it from the start', () => {
  ['h1', 'h2'].forEach(help);
  assert.equal(isSaying('help'), true);
  cancel();
  assert.equal(isSaying('help'), false);
});

test('without many, a newer line with the same key replaces one still queued', () => {
  say('first', { key: 'k' }); say('second', { key: 'k' }); say('third', { key: 'k' });
  finishAll();
  assert.deepEqual(spoken, ['first', 'third']);
});

test('a line that expires unsaid does not start its rate limit', () => {
  say('busy');
  assert.equal(say('late', { key: 'late', gap: 4000, wait: -1 }), true); // queued, then dropped unsaid
  finishAll();
  assert.ok(!spoken.includes('late'));
  assert.equal(say('late again', { key: 'late', gap: 4000 }), true, 'not blocked by the line that never played');
  finishAll();
  assert.ok(spoken.includes('late again'));
  assert.equal(say('too soon', { key: 'late', gap: 4000 }), false, 'but once spoken, the gap holds');
});

test('ambient cues are dropped in screen reader mode', () => {
  setMode('sr');
  assert.equal(say('Middle third', { cue: true }), false);
  setMode('voice');
  assert.equal(say('Middle third', { cue: true }), true);
});
