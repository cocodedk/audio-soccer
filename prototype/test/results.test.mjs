import { test } from 'node:test';
import assert from 'node:assert/strict';

// A memory localStorage, installed before results.js loads.
const store = new Map();
globalThis.localStorage = { getItem: (k) => (store.has(k) ? store.get(k) : null), setItem: (k, v) => store.set(k, String(v)) };
const { createResults } = await import('../js/results.js');

const el = () => {
  const handlers = {};
  return { textContent: '', value: '', hidden: true, focus() {}, select() {}, addEventListener: (n, f) => { handlers[n] = f; }, click: () => handlers.click() };
};
const els = () => ({ box: el(), summary: el(), text: el(), copyStatus: el(), heading: el(), copyBtn: el(), clearBtn: el() });
const round = { success: true, time: 5, startDist: 12, startBehind: false, efficiency: null, where: 0, bumps: 0, kicks: 1, wide: 0, saves: 0, shots: [] };
const META = { mode: 'voice', rate: 1, hidden: false, sound: 'speakers', lang: 'en' };

test('each level keeps the settings it was played with', () => {
  store.clear();
  let meta = META;
  const e = els(), r = createResults(e, () => meta);
  r.add({ level: 1, rounds: [round] });
  meta = { ...META, sound: 'headphones' };
  r.add({ level: 2, rounds: [round] });
  r.show();
  assert.match(e.text.value, /Level 1, .*\nEnded \S+Z\. .*Sound: speakers\./);
  assert.match(e.text.value, /Level 2, .*\nEnded \S+Z\. .*Sound: headphones\./);
  assert.match(e.text.value, /^Audio Soccer trial\. Browser: /);
  // A reload keeps both.
  const again = els();
  createResults(again, () => META);
  assert.equal(again.text.value.match(/Sound: headphones/g).length, 1);
});

test('results saved in the old format (one meta for all) still load', () => {
  store.clear();
  store.set('audiosoccer.results', JSON.stringify({ blocks: [{ level: 1, rounds: [round] }], meta: { mode: 'sr', rate: 2, hidden: true } }));
  const e = els();
  createResults(e, () => META);
  assert.equal(e.box.hidden, false);
  assert.match(e.text.value, /Level 1, .*\nVoice: own screen reader\. Speech speed 2\.0\. Picture hidden\./);
});

// A clock the clear button reads, and a wait for the status line's next tick.
let clock = 1e6;
Date.now = () => clock;
const tick = () => new Promise((ok) => setTimeout(ok, 60));

test('a level paused and left later keeps the settings and time of the pause', () => {
  store.clear();
  let meta = META;
  const e = els(), r = createResults(e, () => meta);
  const atPause = r.now();
  meta = { ...META, sound: 'headphones' }; // changed in the menu during the pause
  r.add({ level: 1, rounds: [round], partial: true, ...atPause });
  r.show();
  assert.match(e.text.value, /Sound: speakers\./);
  assert.doesNotMatch(e.text.value, /headphones/);
});

test('Clear results asks once through the status line, then empties storage', async () => {
  store.clear();
  const e = els(), r = createResults(e, () => META);
  r.add({ level: 1, rounds: [round] });
  r.show();
  e.clearBtn.click();
  await tick();
  assert.match(e.copyStatus.textContent, /again within 15 seconds/);
  assert.notEqual(e.text.value, '', 'one press clears nothing');
  clock += 10000;
  e.clearBtn.click();
  assert.equal(e.text.value, '');
  assert.match(e.copyStatus.textContent, /cleared/);
  assert.equal(e.box.hidden, true, 'nothing left: the panel goes');
  const again = els();
  createResults(again, () => META);
  assert.equal(again.box.hidden, true, 'nothing comes back after a reload');
  clock += 10000;
  e.clearBtn.click();
  await tick();
  assert.match(e.copyStatus.textContent, /again/, 'the next press asks again');
});

test('Clear results: a held Enter does not delete, and a late press asks again audibly', async () => {
  store.clear();
  const e = els(), r = createResults(e, () => META);
  r.add({ level: 1, rounds: [round] });
  r.show();
  for (let i = 0; i < 40; i++) { e.clearBtn.click(); clock += 30; } // key repeat, 1.2 s
  assert.notEqual(e.text.value, '', 'a held key clears nothing');
  clock += 16000; // too late: this press arms again
  e.clearBtn.click();
  assert.equal(e.copyStatus.textContent, '', 'emptied first, so the same words are announced again');
  await tick();
  assert.match(e.copyStatus.textContent, /again/);
  assert.notEqual(e.text.value, '');
});

test('Clear results while a level is paused keeps that level and says so', () => {
  store.clear();
  let gone = null;
  const e = { ...els(), onEmpty: (msg) => { gone = msg; } }, r = createResults(e, () => META);
  r.add({ level: 1, rounds: [round] });
  r.show({ level: 2, rounds: [round], partial: true });
  clock += 20000;
  e.clearBtn.click();
  clock += 1000;
  e.clearBtn.click();
  assert.match(e.copyStatus.textContent, /level in progress is kept/);
  assert.equal(e.box.hidden, false);
  assert.match(e.text.value, /Level 2/);
  assert.doesNotMatch(e.text.value, /Level 1/);
  assert.equal(gone, null);
  const again = els();
  createResults(again, () => META);
  assert.equal(again.box.hidden, true, 'the level in progress is never saved');
  // With nothing paused, clearing hides the panel and says so through onEmpty.
  const f = { ...els(), onEmpty: (msg) => { gone = msg; } }, done = createResults(f, () => META);
  done.add({ level: 1, rounds: [round] });
  done.show();
  clock += 20000;
  f.clearBtn.click();
  clock += 1000;
  f.clearBtn.click();
  assert.equal(gone, 'Results cleared.');
  assert.equal(f.box.hidden, true);
});

test('Clear results: a left level already saved (partial too) is cleared, not kept as in progress', () => {
  const clearTwice = (e) => { clock += 20000; e.clearBtn.click(); clock += 1000; e.clearBtn.click(); };
  // After a reload with a left level saved.
  store.clear();
  createResults(els(), () => META).add({ level: 1, rounds: [round], partial: true });
  let gone = null;
  const e = { ...els(), onEmpty: (msg) => { gone = msg; } };
  createResults(e, () => META);
  assert.equal(e.box.hidden, false);
  clearTwice(e);
  assert.equal(gone, 'Results cleared.');
  assert.equal(e.box.hidden, true);
  assert.equal(e.text.value, '');
  // In one session: level 1 left, level 2 paused before it has a round (no preview).
  gone = null;
  const f = { ...els(), onEmpty: (msg) => { gone = msg; } }, r = createResults(f, () => META);
  r.add({ level: 1, rounds: [round], partial: true });
  r.show(null);
  clearTwice(f);
  assert.equal(gone, 'Results cleared.');
  assert.equal(f.box.hidden, true);
  assert.doesNotMatch(f.copyStatus.textContent, /kept/);
});
