import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fakeSpeech } from './helpers.mjs';

const { spoken, finish } = fakeSpeech();

const { createCues } = await import('../js/cues.js');
const { createWorld } = await import('../js/world.js');
const { setSoundMode } = await import('../js/soundmode.js');
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const world = (ball, hasBall = false) => createWorld({ player: { x: 10, y: 10 }, ball, hasBall });
const calls = () => spoken.filter((s) => ['ahead', 'left', 'right', 'behind'].includes(s));

test('speakers mode speaks the side of the ball, then waits and never repeats a word', async () => {
  const cues = createCues();
  const w = world({ x: 20, y: 3 });
  cues.reset(w);
  cues.update(w);
  await wait(60);
  assert.deepEqual(calls(), ['left']);
  finish();
  cues.update(w);
  assert.deepEqual(calls(), ['left'], 'the same side is not called again');
  w.ball.y = 17;
  cues.update(w);
  await wait(60);
  assert.deepEqual(calls(), ['left'], 'a change inside 1.5 s waits');
  await wait(1500);
  cues.update(w);
  await wait(60);
  assert.deepEqual(calls(), ['left', 'right']);
  finish();
});

test('with the ball the target is the goal; headphones mode has no side calls', async () => {
  spoken.length = 0;
  const cues = createCues();
  const w = world({ x: 10.5, y: 3 }, true); // a kick from y 3 passes the goal on the left: the goal is to the right
  cues.reset(w);
  cues.update(w);
  await wait(60);
  assert.deepEqual(calls(), ['right']);
  finish();
  setSoundMode('headphones');
  const quiet = createCues(), v = world({ x: 20, y: 3 });
  quiet.reset(v);
  quiet.update(v);
  await wait(60);
  assert.deepEqual(calls(), ['right'], 'nothing new in headphones mode');
  setSoundMode('speakers');
});
