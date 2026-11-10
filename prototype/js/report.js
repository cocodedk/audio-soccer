// The results text: plain lines a tester can paste into an email. A header with the browser, then each
// level with when it ended and how it was heard, and each round, including rounds given up or timed out.
import { LEVELS, ROUNDS } from './levels.js';
import { tr } from './i18n/tables.js';
import { kickText } from './kicks.js';

const s1 = (n) => n.toFixed(1);

// The report is always in English, for whoever reads the email.
function outcome(level, r) {
  if (r.success) return `${LEVELS[level].win} in ${s1(r.time)} s`;
  const how = r.timedOut ? 'time up' : r.paused ? 'paused' : r.left ? 'left the round' : 'gave up';
  return `${tr('en', `levels.${level}.miss`)}, ${how} at ${s1(r.time)} s`;
}

// "Round 2: not found, gave up at 45.0 s. Start 22 m, ball behind. Asked where 3. Bumps 0."
export function roundLine(level, i, r) {
  const parts = [`Round ${i + 1}: ${outcome(level, r)}.`];
  if (level === 1) {
    parts.push(`Start ${Math.round(r.startDist)} m, ball ${r.startBehind ? 'behind' : 'ahead'}.`);
    if (r.efficiency !== null) parts.push(`Path efficiency ${r.efficiency.toFixed(2)}.`);
  } else {
    const list = (r.shots || []).map(kickText).join('; '); // missing in results saved before kicks were recorded
    parts.push(`Kicks ${r.kicks}${list ? `: ${list}` : ''}. Wide ${r.wide}.`);
    if (level === 3) parts.push(`Saved ${r.saves}.`);
  }
  parts.push(`Asked where ${r.where}. Bumps ${r.bumps}.`);
  return parts.join(' ');
}

// meta: { mode: 'voice' | 'sr', rate, hidden, sound, lang } as it was when the level ended. The report
// stays English whatever the language: it is data for the developers. sound and lang may be missing
// (results saved before those settings existed), and old blocks have no time.
const LANG_NAMES = { en: 'English', fa: 'Persian' };

export function settingsLine(meta, at) {
  const parts = at ? [`Ended ${at}.`] : [];
  if (meta) {
    parts.push(`Voice: ${meta.mode === 'sr' ? 'own screen reader' : 'built-in voice'}.`,
      `Speech speed ${Number(meta.rate).toFixed(1)}.`, `Picture ${meta.hidden ? 'hidden' : 'shown'}.`);
    if (meta.sound) parts.push(`Sound: ${meta.sound}.`);
    if (meta.lang) parts.push(`Language: ${LANG_NAMES[meta.lang] || meta.lang}.`);
  }
  return parts.join(' ');
}

export function levelLines({ level, rounds, partial = false, meta, at }) {
  const note = partial ? ` (stopped after ${rounds.length} of ${ROUNDS} rounds)` : '';
  const keeper = level === 3 ? ['Keeper: follows the ball, stops at the kick.'] : [];
  const how = settingsLine(meta, at);
  return [`Level ${level}, ${tr('en', `levels.${level}.name`)}${note}`, ...(how ? [how] : []), ...keeper, ...rounds.map((r, i) => roundLine(level, i, r))];
}

// The browser, from navigator.userAgent, without the parts every browser sends.
export const browserName = (ua = '') =>
  ua.replace(/^Mozilla\/5\.0 /, '').replace(/\(KHTML, like Gecko\) /, '').replace(/\s+/g, ' ').trim().slice(0, 160) || 'unknown';

// A time as ISO to the minute, in UTC: "2026-11-09T14:05Z".
export const isoMinute = (d = new Date()) => `${d.toISOString().slice(0, 16)}Z`;

export function resultsText(blocks, browser) {
  return [`Audio Soccer trial. Browser: ${browserName(browser)}.`, ...blocks.flatMap(levelLines)].join('\n');
}
