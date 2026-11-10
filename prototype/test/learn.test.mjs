import { test } from 'node:test';
import assert from 'node:assert/strict';

const { nameWait } = await import('../js/learn.js');

test('built-in voice: the wait scales with the name and the voice rate, as before', () => {
  assert.equal(nameWait('abcdefghij', false, 1), 500 + 700);
  assert.equal(nameWait('abcdefghij', false, 2), 500 + 350);
});

test('screen-reader mode: a longer wait that ignores the built-in rate', () => {
  const name = 'A voice calls where the target is: ahead, left, right or behind.';
  assert.equal(nameWait(name, true, 1), nameWait(name, true, 3));
  // Longer than the built-in voice at its slowest rate (voices.js RATES starts at 1.0).
  assert.ok(nameWait(name, true, 1.3) > nameWait(name, false, 1));
  assert.equal(nameWait('abcdefghij', true, 1), 1000 + 1000);
});

test('screen-reader mode: longer names (Persian ones run longer) wait longer', () => {
  assert.ok(nameWait('ضربه به یک دیواره.', true, 1) < nameWait('یک صدا می‌گوید هدف کجاست: جلو، چپ، راست یا پشت سر.', true, 1));
});
