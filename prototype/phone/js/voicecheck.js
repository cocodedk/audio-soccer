// The voice check: speak a line with the browser's speech engine and wait for it to really start, trying
// the candidate voices (voicepick.js) in order until one does. Talks to speechSynthesis directly, not
// through speech.js, because the check needs the start event; the voice that started is then handed to
// speech.js (through voices.js) so every later line uses exactly that voice.
import { setVoice, refreshVoices } from '../../js/voices.js';
import { setLang, LANG_TAG } from '../../js/i18n/index.js';
import { tr } from '../../js/i18n/tables.js';
import { savePref } from '../../js/storage.js';
import { describeVoice } from './voicepick.js';

const synth = () => { try { return typeof speechSynthesis !== 'undefined' ? speechSynthesis : null; } catch (e) { return null; } };

export const START_MS = 5000; // a line that has not started by then never will
const CANCEL_GAP = 50;        // ms: Chrome can swallow a speak() that comes right after a cancel()

// The engine's voices, waiting up to ms for a list that loads late (Chrome loads it after the page).
export async function loadVoices(ms = 1500) {
  const s = synth();
  if (!s) return [];
  const read = () => { try { return s.getVoices() || []; } catch (e) { return []; } };
  for (let waited = 0; !read().length && waited < ms; waited += 100) await new Promise((r) => setTimeout(r, 100));
  return read();
}

// Speak text and resolve { ok, error, ms, end } once it starts (ok: true, ms to start), fails (error:
// true) or times out. end resolves when the line is over (or at once if it never started), so the next
// line does not cut it off.
export function speakCheck(text, { voice = null, lang, rate = 1 } = {}) {
  const s = synth();
  if (!s || typeof SpeechSynthesisUtterance === 'undefined') return Promise.resolve({ ok: false, error: true, ms: 0, end: Promise.resolve() });
  const t0 = performance.now();
  let ended;
  const end = new Promise((r) => { ended = r; });
  return new Promise((resolve) => {
    let settled = false;
    const settle = (ok, error = false) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      if (!ok) { try { s.cancel(); } catch (e) { /* no engine */ } ended(); }
      resolve({ ok, error, ms: performance.now() - t0, end });
    };
    const timer = setTimeout(() => settle(false), START_MS);
    try {
      const u = new SpeechSynthesisUtterance(text);
      if (voice) u.voice = voice;
      u.lang = voice ? voice.lang : lang;
      u.rate = rate;
      u.onstart = () => settle(true);
      u.onend = () => { settle(true); ended(); }; // an engine that skips onstart still ends a spoken line
      u.onerror = () => { settle(false, true); ended(); };
      s.cancel();
      setTimeout(() => { if (!settled) s.speak(u); }, CANCEL_GAP);
    } catch (e) { settle(false, true); }
  });
}

// Try each candidate until one starts speaking. onAttempt(record) gets each try as it ends: { name,
// lang, local, started, error, ms } (name and local are null for the browser's default voice).
// Resolves { chosen, ms, end }: the candidate that spoke (null if none did), its ms to start, and when
// its line is over.
export async function proveVoice(candidates, rate, onAttempt = () => {}) {
  for (const c of candidates) {
    const line = tr(c.lang, c.english ? 'phone.english' : 'phone.check');
    const r = await speakCheck(line, { voice: c.voice, lang: LANG_TAG[c.lang], rate });
    const who = c.voice ? describeVoice(c.voice) : { name: null, lang: LANG_TAG[c.lang], local: null };
    onAttempt({ ...who, started: r.ok, error: r.error, ms: Math.round(r.ms) });
    if (r.ok) return { chosen: c, ms: Math.round(r.ms), end: r.end };
  }
  return { chosen: null, ms: null, end: Promise.resolve() };
}

// From now on speech.js speaks with the proven voice. list: the voices it was found in (voices.js may
// not have that list yet if the engine never announced it). An English voice for a Persian player
// switches the game to English for this session only: savedLang (the player's choice) stays stored.
export function useVoice({ lang, voice, english }, savedLang, list) {
  refreshVoices(list);
  if (english) {
    setLang(lang);
    savePref('lang', savedLang);
  }
  if (voice) setVoice(voice.voiceURI);
}
