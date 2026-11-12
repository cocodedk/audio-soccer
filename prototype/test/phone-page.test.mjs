// The phone test page (phone/index.html) and its results text: a complete document whose files resolve
// through its base the same way served from prototype/ and on Pages, text that matches the English table,
// and a results report that carries every fact the test is for.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { tr } from '../js/i18n/tables.js';
import { newSession, resultsText } from '../phone/js/report.js';
import { closeStats, newStats } from '../js/levels.js';

const read = (p) => readFileSync(new URL(p, import.meta.url), 'utf8');
const HTML = read('../phone/index.html');

test('phone/index.html is a complete document with a viewport, and every file it names exists', () => {
  assert.match(HTML, /^<!doctype html>\s*<html lang="en" dir="ltr">/i);
  assert.match(HTML.slice(0, 300), /<meta charset="utf-8">/i);
  assert.match(HTML, /<meta name="viewport" content="width=device-width, initial-scale=1/);
  // Relative URLs resolve against <base href="../">: the prototype folder (locally) or the site root.
  assert.match(HTML, /<base href="\.\.\/">/);
  const refs = [...HTML.matchAll(/<(?:script|link)\b[^>]*\b(?:src|href)="([^"]+)"/g)].map((m) => m[1]);
  assert.ok(refs.includes('phone/js/main.js') && refs.includes('style.css') && refs.includes('phone/phone.css'), refs.join(', '));
  for (const r of refs) {
    assert.doesNotMatch(r, /^(https?:)?\/\//, `${r}: another host`);
    assert.ok(existsSync(new URL(`../${r}`, import.meta.url)), `${r} missing`);
  }
});

test('phone/index.html text matches the English table, and every key it uses exists in Persian', () => {
  let seen = 0;
  for (const m of HTML.matchAll(/<(\w+)[^>]*\sdata-i18n="([\w.]+)"[^>]*>([\s\S]*?)<\/\1>/g)) {
    assert.equal(m[3].replace(/\s+/g, ' ').trim(), tr('en', m[2]), m[2]);
    assert.notEqual(tr('fa', m[2]), tr('en', m[2]), `${m[2]} has no Persian`);
    seen++;
  }
  assert.ok(seen >= 6, `only ${seen} marked elements`);
});

test('the Pages workflow copies the phone page next to the trial', () => {
  assert.match(read('../../.github/workflows/deploy-pages.yml'), /cp -r prototype\/phone _site\//);
});

const round = (how, time, extra = {}) => ({ ...closeStats(2, { ...newStats({ player: { x: 8, y: 8 }, ball: { x: 8.5, y: 8 } }), time, ...extra }, how) });

test('results text: the voices, the device facts, the hand-over and each scheme with its rounds', () => {
  const s = newSession('2026-11-12T10:00Z', 'Mozilla/5.0 (Linux; Android 14) Chrome/130.0 Mobile', 'fa');
  Object.assign(s, {
    voiceCount: 40, voices: [{ name: 'English UK', lang: 'en-GB', local: true }, { name: 'Persian', lang: 'fa-IR', local: false }],
    persianOnline: true, english: true, spoke: { name: 'English UK', lang: 'en-GB', local: true }, check: 'ok', checkMs: 1234,
    fullscreen: 'ok', orientation: 'refused', wakeLock: 'unsupported', vibration: true, handover: 14.4, tilt: 'none',
    schemes: [
      { scheme: 'drag', rounds: [round('success', 23.4, { where: 2 }), round('timeout', 120)], partial: true, at: '2026-11-12T10:05Z' },
    ],
  });
  const text = resultsText([s]);
  for (const want of [
    'Session 2026-11-12T10:00Z. Language: Persian. Browser: (Linux; Android 14) Chrome/130.0 Mobile.',
    'Voices: 40 in all. English and Persian: English UK (en-GB, local); Persian (fa-IR, online).',
    'Local Persian voice: no, online only.',
    'Voice check: spoke after 1.2 s, with English UK (en-GB, local).',
    'Spoke English instead of Persian: no local Persian voice.',
    'Fullscreen: ok. Portrait lock: refused. Wake lock: not supported. Vibration: supported. Tilt events: none.',
    'Screen reader hand-over: swipe after 14 s.',
    'Drag to steer (stopped after 2 of 3 rounds), ended 2026-11-12T10:05Z:',
    'Round 1: scored in 23.4 s. Kicks 0. Wide 0. Asked where 2. Bumps 0.',
    'Round 2: not scored, time up at 120.0 s.',
  ]) assert.ok(text.includes(want), `missing: ${want}\n${text}`);
});

test('results text: a failed voice check says so and has no hand-over line', () => {
  const s = Object.assign(newSession('2026-11-12T11:00Z', 'UA', 'fa'), { check: 'novoice', vibration: false });
  const text = resultsText([s]);
  assert.match(text, /Voice check: no voice that works offline\. Stopped before the screen reader hand-over\./);
  assert.match(text, /Local Persian voice: no\./);
  assert.match(text, /Vibration: not supported\. Tilt events: not tried\./);
  assert.doesNotMatch(text, /hand-over: /);
});
