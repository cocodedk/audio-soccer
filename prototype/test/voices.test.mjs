import { test } from 'node:test';
import assert from 'node:assert/strict';
import { refreshVoices, chooseVoices, missingVoice, listVoices, pickVoices } from '../js/voices.js';

const v = (name, lang, localService) => ({ name, lang, localService, voiceURI: name });
// Load a fake engine list, then look at one language.
const missing = (list, lang) => { refreshVoices(list); chooseVoices(lang); return missingVoice(); };

test('missingVoice: an empty list (not loaded yet) is not a missing voice', () => {
  assert.equal(missing([], 'fa'), false);
});

test('missingVoice: no Persian voice at all is missing', () => {
  assert.equal(missing([v('En', 'en-GB', true)], 'fa'), true);
});

test('missingVoice: online-only Persian counts as missing, but stays in the voice list', () => {
  assert.equal(missing([v('En', 'en-GB', true), v('Net Fa', 'fa-IR', false)], 'fa'), true);
  assert.deepEqual(listVoices().map((x) => x.name), ['Net Fa'], 'the player can still pick it');
});

test('missingVoice: a local Persian voice is not missing', () => {
  assert.equal(missing([v('Local Fa', 'fa-IR', true)], 'fa'), false);
  assert.equal(missing([v('Old Fa', 'fa-IR', undefined)], 'fa'), false, 'a browser that does not say counts as local');
});

test('missingVoice: mixed online and local Persian is not missing', () => {
  assert.equal(missing([v('Net Fa', 'fa-IR', false), v('Local Fa', 'fa', true)], 'fa'), false);
});

test('missingVoice: English with a local voice is unchanged', () => {
  assert.equal(missing([v('En', 'en-GB', true), v('Net Fa', 'fa-IR', false)], 'en'), false);
});

test('pickVoices: a voice with no localService sorts with the local ones, as missingVoice counts it', () => {
  const list = [v('A Net', 'fa-IR', false), v('B Old', 'fa-IR', undefined)];
  assert.deepEqual(pickVoices(list, 'fa').map((x) => x.name), ['B Old', 'A Net']);
});
