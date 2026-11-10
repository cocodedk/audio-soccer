// The voice select, on a fake page: a voice that doesn't say whether it is local counts as local.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fakeSpeech } from './helpers.mjs';

globalThis.localStorage = { getItem: () => null, setItem() {} };
fakeSpeech();
const el = () => ({ options: [], hidden: true, dataset: {}, add(o) { this.options.push(o); }, addEventListener() {}, setAttribute() {} });
const voiceEl = el();
globalThis.document = {
  documentElement: {}, title: '', addEventListener() {},
  querySelector: (s) => (s === '#voice' ? voiceEl : el()),
  querySelectorAll: () => [],
};
globalThis.Option = class { constructor(text, value) { this.text = text; this.value = value; } };

const { refreshVoices } = await import('../js/voices.js');
const { fillVoices } = await import('../js/menu.js');

test('menu: only a voice that says it is online is labelled online', () => {
  refreshVoices([
    { name: 'Old', lang: 'en-GB', voiceURI: 'o' },
    { name: 'Net', lang: 'en-GB', localService: false, voiceURI: 'n' },
  ]);
  fillVoices();
  const labels = voiceEl.options.map((o) => o.text);
  assert.ok(labels.includes('Old, en-GB'), labels.join(' | '));
  assert.ok(labels.includes('Net, en-GB, online'), labels.join(' | '));
});
