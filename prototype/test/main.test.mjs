// main.js on a fake page: a level left half-played keeps the settings of its pause, and clearing
// with nothing left hides the panel, takes focus out of it and says so.
import { test } from 'node:test';
import assert from 'node:assert/strict';

const store = new Map();
globalThis.localStorage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, String(v)) };
const realInterval = setInterval;
globalThis.setInterval = (...a) => realInterval(...a).unref(); // speech.js pumps forever
globalThis.fetch = () => Promise.reject(new Error('offline'));
globalThis.window = globalThis;
globalThis.addEventListener = () => {};
globalThis.Option = class { constructor(text, value) { this.text = text; this.value = value; } };
globalThis.ResizeObserver = class { observe() {} };
let frame = null;
globalThis.requestAnimationFrame = (f) => { frame = f; };

const noop = new Proxy({}, { get: () => () => noop, set: () => true });
const el = (sel) => {
  const on = {};
  return {
    sel, textContent: '', value: '', hidden: true, checked: false, dataset: {}, options: [], style: {},
    addEventListener: (n, f) => { on[n] = f; }, fire: (n, e = {}) => on[n]?.({ preventDefault() {}, ...e }),
    focus() { document.activeElement = this; }, blur() {}, select() {}, add() {}, setAttribute() {},
    getContext: () => noop, getBoundingClientRect: () => ({ width: 0 }),
    contains: (x) => sel === '#results' && ['#clear-results', '#copy', '#h-results'].includes(x?.sel),
  };
};
const els = new Map();
const $ = (s) => { if (!els.has(s)) els.set(s, el(s)); return els.get(s); };
globalThis.document = {
  documentElement: el('html'), title: '', hidden: false, activeElement: null, fonts: null,
  addEventListener() {}, querySelector: $, querySelectorAll: () => [],
};

let clock = 1e6;
Date.now = () => clock;
const wait = (ms) => new Promise((ok) => setTimeout(ok, ms));
let time = 0;
const play = (secs) => { for (let i = 0; i < secs * 60; i++) frame((time += 1000 / 60)); };
const key = (k) => $('#arena').fire('keydown', { key: k });

await import('../js/main.js');
const { setSoundMode } = await import('../js/soundmode.js');
const saved = () => JSON.parse(store.get('audiosoccer.results')).blocks;

test('main: a level left in the pause is reported with the sound setting it was played with', () => {
  setSoundMode('speakers');
  window.__audioSoccer.startLevel(1);
  key('ArrowUp');
  play(1);
  key('Escape');
  setSoundMode('headphones'); // changed in the menu during the pause
  window.__audioSoccer.startLevel(2);
  const [left] = saved();
  assert.equal(left.level, 1);
  assert.equal(left.partial, true);
  assert.equal(left.meta.sound, 'speakers');
});

test('main: clearing with nothing left hides the panel, moves focus to a level button and says so', async () => {
  key('Escape'); // level 2 paused before it has a round: only the saved level 1 is shown
  assert.equal($('#results').hidden, false);
  $('#resume').hidden = true; // as after a finished level: focus goes to the level buttons
  $('#clear-results').focus();
  clock += 20000;
  $('#clear-results').fire('click');
  clock += 1000;
  $('#clear-results').fire('click');
  assert.equal($('#results').hidden, true);
  assert.equal(document.activeElement, $('[data-level]'));
  assert.deepEqual(saved(), []);
  await wait(150);
  assert.match($('#live-assertive').textContent, /Results cleared\./);
});
