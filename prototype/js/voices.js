// Voice and speech-rate choice. The helpers are pure; the small state at the bottom is persisted.
import { loadPref, savePref } from './storage.js';
import { getLang } from './i18n/index.js';

export const RATES = [1.0, 1.3, 1.6, 2.0, 2.5];
export const DEFAULT_RATE = 1.3;

const isGB = (v) => /^en[-_]GB/i.test(v.lang);
// On the device unless the browser says it is online: one that leaves localService out counts as local.
export const local = (v) => v.localService !== false;

// The voices of one language (voice.lang starts with its code: en, fa), best first: local ones (a
// network voice adds latency and needs internet), then British for English, then by name.
export function pickVoices(list, lang) {
  const own = new RegExp(`^${lang}`, 'i');
  return list
    .filter((v) => own.test(v.lang))
    .sort((a, b) => (Number(local(b)) - Number(local(a))) || (Number(isGB(b)) - Number(isGB(a))) || a.name.localeCompare(b.name));
}

// One step up (dir 1) or down (-1) the list of rates, staying inside it.
export function stepRate(rate, dir) {
  const i = RATES.indexOf(rate);
  const at = i < 0 ? RATES.indexOf(DEFAULT_RATE) : i;
  return RATES[Math.min(RATES.length - 1, Math.max(0, at + dir))];
}

let all = [], voices = [], voice = null;
let rate = (() => { const r = Number(loadPref('rate')); return RATES.includes(r) ? r : DEFAULT_RATE; })();

// The saved voice of English stays under the old key, so a choice made before languages still holds.
const voiceKey = (lang) => (lang === 'en' ? 'voice' : `voice-${lang}`);

export const getRate = () => rate;
export const currentVoice = () => voice;
export const listVoices = () => voices;
// The engine has voices at all (its list has loaded), but none on this device for the current
// language. An online-only language counts as missing: online voices may be blocked (the tester is
// in Iran) and then never speak.
export const missingVoice = () => all.length > 0 && !voices.some(local);

export function setRate(r) {
  if (RATES.includes(r)) { rate = r; savePref('rate', r); }
  return rate;
}

export function setVoice(uri) {
  const v = voices.find((x) => x.voiceURI === uri);
  if (v) { voice = v; savePref(voiceKey(getLang()), uri); }
}

// Keep the voices of one language, and the saved choice if it is still there.
export function chooseVoices(lang = getLang()) {
  voices = pickVoices(all, lang);
  const saved = loadPref(voiceKey(lang));
  voice = voices.find((v) => v.voiceURI === saved) || voices[0] || null;
}

// Take the engine's voice list.
export function refreshVoices(list) {
  all = list;
  chooseVoices();
}
