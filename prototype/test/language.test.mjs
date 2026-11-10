// The Persian fallback to the screen reader, on a fake page: a voice mode the player picked by hand in
// Persian is remembered across visits (the radios themselves aren't saved).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fakeSpeech } from './helpers.mjs';

const store = {};
globalThis.localStorage = { getItem: (k) => (k in store ? store[k] : null), setItem: (k, v) => { store[k] = v; } };
store['audiosoccer.lang'] = 'fa';
fakeSpeech();

// A fake element: enough for menu.js, i18n/dom.js and language.js.
const el = (props = {}) => ({ options: [], hidden: true, dataset: {}, add() {}, addEventListener(type, fn) { this.on = fn; }, setAttribute() {}, ...props });
// Radios like the browser's: checking one unchecks the other.
let mode = 'voice';
const radio = (value) => Object.defineProperty(el({ value }), 'checked', { get: () => mode === value, set(on) { if (on) mode = value; } });
const modes = [radio('voice'), radio('sr')];
const notice = el();
globalThis.document = {
  documentElement: {}, title: '', addEventListener() {},
  querySelector(s) {
    if (s === '#voice-notice') return notice;
    if (s === 'input[name="voice-mode"]:checked') return modes.find((m) => m.checked) || null;
    const m = s.match(/voice-mode"\]\[value="(\w+)"/);
    return m ? modes.find((r) => r.value === m[1]) : el();
  },
  querySelectorAll: (s) => (s === 'input[name="voice-mode"]' ? modes : []),
};
const pick = (value) => { mode = value; modes.find((m) => m.value === value).on(); };

const { refreshVoices } = await import('../js/voices.js');
const { createLanguage, modeNow } = await import('../js/language.js');
const online = [{ name: 'Net Fa', lang: 'fa-IR', localService: false, voiceURI: 'n' }];

// A fresh "visit": the radios back to the page default, a new language controller.
const visit = (list) => { mode = 'voice'; const l = createLanguage({ applyMode() {}, onChange() {} }); refreshVoices(list); return l.check(); };

test('language: online-only Persian falls back to the screen reader on a first visit', () => {
  assert.equal(visit(online), true);
  assert.equal(modeNow(), 'sr');
});

test('language: the built-in voice picked by hand in Persian is kept on the next visit', () => {
  pick('voice');
  assert.equal(store['audiosoccer.mode-picked-fa'], 'voice');
  assert.equal(visit(online), false);
  assert.equal(modeNow(), 'voice');
});

test('language: with no Persian voice at all the fallback still happens, whatever was picked', () => {
  assert.equal(visit([{ name: 'En', lang: 'en-GB', localService: true, voiceURI: 'e' }]), true);
  assert.equal(modeNow(), 'sr');
});

test('language: the screen reader picked by hand is not remembered as the built-in voice', () => {
  pick('sr');
  assert.equal(visit(online), true);
  assert.equal(modeNow(), 'sr');
});
