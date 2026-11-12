// PURE: the phone test's results as plain English lines a tester can paste into an email, one block per
// session (each press of Start). The rounds use the trial's own round lines (../../js/report.js).
import { roundLine, browserName } from '../../js/report.js';
import { ROUNDS } from '../../js/levels.js';

export const LEVEL = 2; // the trial's "take the ball to the goal and score"
const SCHEMES = { drag: 'Drag to steer', tilt: 'Tilt to steer' };
const LANG_NAMES = { en: 'English', fa: 'Persian' };
const OUTCOMES = { ok: 'ok', refused: 'refused', unsupported: 'not supported' };

// A new session record, filled in as the test goes on.
export const newSession = (at, browser, lang) => ({
  at, browser, lang, voiceCount: 0, voices: [], persianLocal: false, persianOnline: false, spoke: null,
  english: false, check: null, checkMs: null, fullscreen: null, orientation: null, wakeLock: null,
  vibration: null, handover: null, tilt: 'not tried', schemes: [],
});

const voiceText = (v) => `${v.name} (${v.lang}, ${v.local ? 'local' : 'online'})`;

function voiceLines(s) {
  const lines = [`Voices: ${s.voiceCount} in all. English and Persian: ${s.voices.map(voiceText).join('; ') || 'none'}.`];
  const fa = s.persianLocal ? 'yes' : s.persianOnline ? 'no, online only' : 'no';
  lines.push(`Local Persian voice: ${fa}.`);
  const who = s.spoke ? voiceText(s.spoke) : "the browser's default voice (its voice list was empty)";
  if (s.check === 'ok') lines.push(`Voice check: spoke after ${(s.checkMs / 1000).toFixed(1)} s, with ${who}.`);
  else if (s.check === 'novoice') lines.push('Voice check: no voice that works offline. Stopped before the screen reader hand-over.');
  else if (s.check === 'silent') lines.push(`Voice check: speech never started, tried ${who}. Stopped before the screen reader hand-over.`);
  else lines.push('Voice check: not finished.');
  if (s.english) lines.push('Spoke English instead of Persian: no local Persian voice.');
  return lines;
}

function deviceLine(s) {
  const o = (v) => OUTCOMES[v] || 'not tried';
  const vib = s.vibration === null ? 'not checked' : s.vibration ? 'supported' : 'not supported';
  return `Fullscreen: ${o(s.fullscreen)}. Portrait lock: ${o(s.orientation)}. Wake lock: ${o(s.wakeLock)}. `
    + `Vibration: ${vib}. Tilt events: ${s.tilt}.`;
}

// One scheme played: { scheme, rounds, partial, at }.
function schemeLines(b) {
  const note = b.partial ? ` (stopped after ${b.rounds.length} of ${ROUNDS} rounds)` : '';
  return [`${SCHEMES[b.scheme] || b.scheme}${note}, ended ${b.at}:`, ...b.rounds.map((r, i) => roundLine(LEVEL, i, r))];
}

export function sessionLines(s) {
  const hand = s.handover === null ? 'not done' : `swipe after ${Math.round(s.handover)} s`;
  return [
    `Session ${s.at}. Language: ${LANG_NAMES[s.lang] || s.lang}. Browser: ${browserName(s.browser)}.`,
    ...voiceLines(s),
    deviceLine(s),
    ...(s.check === 'ok' ? [`Screen reader hand-over: ${hand}.`] : []),
    ...s.schemes.flatMap(schemeLines),
  ];
}

export function resultsText(sessions) {
  return ['Audio Soccer phone controls test.', ...sessions.flatMap((s) => ['', ...sessionLines(s)])].join('\n');
}
