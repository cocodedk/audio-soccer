// Wording that has to match what the code does.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { tr } from '../js/i18n/tables.js';

test('strings: the Persian fallback speaks of this device, which may still have an online voice', () => {
  assert.match(tr('en', 'lang.fallback'), /^There is no Persian voice on this device\./);
  assert.match(tr('fa', 'lang.fallback'), /^این دستگاه صدای فارسی آفلاین ندارد؛/);
});

test('strings: the clear window is 15 seconds in both languages', () => {
  assert.match(tr('en', 'clear.confirm'), /15 seconds/);
  assert.match(tr('fa', 'clear.confirm'), /۱۵ ثانیه/);
});
