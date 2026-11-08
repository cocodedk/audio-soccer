import { test } from 'node:test';
import assert from 'node:assert/strict';
import { keyName, wanted } from '../js/input.js';

const key = (k, mods = {}) => { const e = { key: k, ...mods }; return { name: keyName(e), ok: wanted(e, keyName(e)) }; };

test('speed keys: [ and - slower, ] and + and = faster', () => {
  for (const k of ['[', '-']) assert.equal(keyName({ key: k }), 'rateDown');
  for (const k of [']', '+', '=']) assert.equal(keyName({ key: k }), 'rateUp');
});

test('AltGr (Ctrl+Alt on Windows) and Option still reach the speed keys; Ctrl and Meta alone do not', () => {
  for (const k of ['[', ']', '-', '+', '=']) {
    assert.equal(key(k).ok, true, k);
    assert.equal(key(k, { ctrlKey: true, altKey: true }).ok, true, `AltGr ${k}`);
    assert.equal(key(k, { altKey: true }).ok, true, `Option ${k}`);
    assert.equal(key(k, { ctrlKey: true }).ok, false, `Ctrl ${k} is browser zoom`);
    assert.equal(key(k, { metaKey: true }).ok, false, `Meta ${k}`);
    assert.equal(key(k, { ctrlKey: true, altKey: true, metaKey: true }).ok, false);
  }
});

test('other keys still ignore Ctrl, Alt and Meta so shortcuts keep working', () => {
  for (const k of ['g', 'h', 'w', ' ', 'Escape', 'ArrowUp']) {
    assert.equal(key(k).ok, true, k);
    for (const mods of [{ ctrlKey: true }, { altKey: true }, { metaKey: true }, { ctrlKey: true, altKey: true }]) {
      assert.equal(key(k, mods).ok, false, `${k} ${JSON.stringify(mods)}`);
    }
  }
  assert.equal(key('x').ok, false);
});
