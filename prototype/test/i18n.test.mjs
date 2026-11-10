import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { TABLES, tr, trList, entry } from '../js/i18n/tables.js';
import { pickVoices } from '../js/voices.js';
import { startLines, summarySpeech, ROUNDS } from '../js/levels.js';
import { newStats, closeStats } from '../js/levels.js';
import { resultsText } from '../js/report.js';

const { en, fa } = TABLES;
const HTML = readFileSync(new URL('../index.html', import.meta.url), 'utf8');

// Every dotted key of a table, arrays by index, so a missing or extra entry shows up by name.
function keys(obj, prefix = '') {
  return Object.entries(obj).flatMap(([k, v]) => {
    const key = prefix + k;
    return v && typeof v === 'object' ? keys(v, `${key}.`) : [key];
  });
}
// Every string of a table with its key.
function strings(obj, prefix = '') {
  return Object.entries(obj).flatMap(([k, v]) => {
    if (typeof v === 'string') return [[prefix + k, v]];
    return v && typeof v === 'object' ? strings(v, `${prefix}${k}.`) : [];
  });
}

test('both languages have exactly the same entries', () => {
  assert.deepEqual(keys(fa).filter((k) => k !== 'digits').sort(), keys(en).sort()); // digits is Persian's own helper
});

test('Persian text: Persian digits only, no English words but key and product names, ZWNJ where needed', () => {
  const allowed = /Escape|NVDA\+Space|NVDA|JAWS|VoiceOver|Quick Nav|[WGH]\b|<\/?kbd>/g;
  for (const [key, text] of strings(fa)) {
    if (key === 'digits') continue;
    assert.doesNotMatch(text, /[0-9]/, `${key}: ASCII digit`);
    const left = text.replace(allowed, '').replace(/\{\w+\}/g, '');
    assert.doesNotMatch(left, /[A-Za-z]/, `${key}: Latin letters in "${text}"`);
    assert.doesNotMatch(text, /(^|\s)ن?می /u, `${key}: می needs a ZWNJ after it: "${text}"`);
    assert.doesNotMatch(text, /\s(ها|های|تر|ترین)(\s|$|[.،:؛!؟])/u, `${key}: a suffix needs a ZWNJ: "${text}"`);
  }
  for (const word of ['دروازه‌بان', 'می‌شود', 'یک‌سوم']) assert.ok(JSON.stringify(fa).includes(word), word);
});

test('lookup: slots, function entries, Persian digits, the English fallback for a missing key', () => {
  assert.equal(tr('en', 'metres', { n: 1 }), '1 metre');
  assert.equal(tr('en', 'metres', { n: 3 }), '3 metres');
  assert.equal(tr('fa', 'metres', { n: 3 }), '۳ متر');
  assert.equal(tr('en', 'status.play', { level: 2, name: 'Score', round: 1, total: 3, time: '4.5' }), 'Level 2, Score · Round 1 of 3 · 4.5 s');
  assert.equal(tr('fa', 'status.play', { level: 2, name: 'گل زدن', round: 1, total: 3, time: '4.5' }), 'مرحله ۲، گل زدن · دور ۱ از ۳ · ۴٫۵ ثانیه');
  assert.equal(tr('fa', 'call.behind'), 'پشت سر');
  assert.equal(tr('xx', 'call.left'), 'left', 'an unknown language is English');
  assert.equal(tr('en', 'no.such.key'), 'no.such.key');
  assert.equal(trList('fa', 'help').length, trList('en', 'help').length);
  assert.equal(entry('fa', 'canvas').you, 'شما');
});

// A test that index.html and the English table say the same thing, so the page reads right before and
// without the script, and the two cannot drift apart.
test('index.html text matches the English table, and every key exists in Persian', () => {
  const squash = (s) => s.replace(/\s+/g, ' ').trim();
  let seen = 0;
  for (const m of HTML.matchAll(/<(\w+)([^>]*?)\sdata-i18n(-html|-aria)?="ui\.(\w+)"([^>]*)>([\s\S]*?)<\/\1>|<(\w+)([^>]*?)\sdata-i18n-aria="ui\.(\w+)"([^>]*)>/g)) {
    const key = m[4] || m[9], mode = m[3] || '', attrs = (m[2] || '') + (m[5] || '') + (m[8] || '') + (m[10] || '');
    assert.ok(en.ui[key] !== undefined && fa.ui[key] !== undefined, `ui.${key} missing`);
    const want = squash(en.ui[key]);
    if (mode === '-aria') assert.equal(squash(attrs.match(/aria-label="([^"]*)"/)?.[1] ?? ''), want, `aria ui.${key}`);
    else assert.equal(squash(m[6]), want, `ui.${key}`);
    seen++;
  }
  assert.ok(seen > 35, `only ${seen} marked elements found`);
});

test('the page declares its charset first, so Persian is not read as windows-1252', () => {
  assert.match(HTML.slice(0, 200), /^\s*<meta charset="utf-8">/i);
});

test('level texts in Persian: intros, summary with Persian digits', () => {
  assert.deepEqual(startLines(1, 1, 'fa'), ['دور ۱. توپ را پیدا کنید.']);
  assert.equal(startLines(2, 2, 'fa').length, 1);
  assert.equal(startLines(3, 1, 'fa').length, 3);
  const round = (success, time) => ({ ...closeStats(1, { ...newStats({ player: { x: 1, y: 1 }, ball: { x: 5, y: 5 } }), time }, success ? 'success' : 'giveup') });
  const rounds = [round(true, 12.44), round(false, 40), round(true, 10)];
  assert.equal(summarySpeech(1, rounds, false, 'fa'), 'مرحله ۱ تمام شد. زمان‌ها: ۱۲٫۴، پیدا نشد، ۱۰٫۰.');
  assert.equal(summarySpeech(1, rounds.slice(0, 2), true, 'fa'), `مرحله ۱ بعد از ۲ دور از ${'۳'} دور متوقف شد. زمان‌ها: ۱۲٫۴، پیدا نشد.`);
  assert.equal(ROUNDS, 3);
});

test('the copied report stays English whatever the language, and says which sound and language were used', () => {
  const rounds = [{ ...closeStats(1, { ...newStats({ player: { x: 1, y: 1 }, ball: { x: 5, y: 5 } }), time: 8 }, 'success') }];
  const meta = { mode: 'sr', rate: 1.3, hidden: false, sound: 'speakers', lang: 'fa' };
  const how = resultsText([{ level: 1, rounds, meta }], 'UA').split('\n')[2];
  assert.equal(how, 'Voice: own screen reader. Speech speed 1.3. Picture shown. Sound: speakers. Language: Persian.');
  assert.doesNotMatch(resultsText([{ level: 1, rounds, meta: { mode: 'voice', rate: 1, hidden: true } }], 'UA'), /Sound:|Language:/, 'older saved results have neither');
});

test('voices: Persian ones for Persian, local first; English ones keep the British-first rule', () => {
  const v = (name, lang, localService) => ({ name, lang, localService, voiceURI: name });
  const list = [v('Net Fa', 'fa-IR', false), v('Local En', 'en-US', true), v('Local Fa', 'fa_IR', true), v('Fr', 'fr-FR', true), v('Farsi B', 'fa', true)];
  assert.deepEqual(pickVoices(list, 'fa').map((x) => x.name), ['Farsi B', 'Local Fa', 'Net Fa']);
  assert.deepEqual(pickVoices(list, 'en').map((x) => x.name), ['Local En']);
  assert.deepEqual(pickVoices([v('Hey', 'en-US', true)], 'fa'), []);
});
