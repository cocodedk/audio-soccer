// Voice and speech-rate choice. The helpers are pure; the small state at the bottom is persisted.
import { loadPref, savePref } from './storage.js';

export const RATES = [1.0, 1.3, 1.6, 2.0, 2.5];
export const DEFAULT_RATE = 1.3;

const isGB = (v) => /^en[-_]GB/i.test(v.lang);

// English voices, best first: local ones (a network voice adds latency and needs internet), then
// British, then by name.
export function englishVoices(list) {
  return list
    .filter((v) => /^en/i.test(v.lang))
    .sort((a, b) => (Number(!!b.localService) - Number(!!a.localService)) || (Number(isGB(b)) - Number(isGB(a))) || a.name.localeCompare(b.name));
}

// One step up (dir 1) or down (-1) the list of rates, staying inside it.
export function stepRate(rate, dir) {
  const i = RATES.indexOf(rate);
  const at = i < 0 ? RATES.indexOf(DEFAULT_RATE) : i;
  return RATES[Math.min(RATES.length - 1, Math.max(0, at + dir))];
}

let voices = [], voice = null;
let rate = (() => { const r = Number(loadPref('rate')); return RATES.includes(r) ? r : DEFAULT_RATE; })();

export const getRate = () => rate;
export const currentVoice = () => voice;
export const listVoices = () => voices;

export function setRate(r) {
  if (RATES.includes(r)) { rate = r; savePref('rate', r); }
  return rate;
}

export function setVoice(uri) {
  const v = voices.find((x) => x.voiceURI === uri);
  if (v) { voice = v; savePref('voice', uri); }
}

// Take the engine's voice list: keep the English ones, and the saved choice if it is still there.
export function refreshVoices(all) {
  voices = englishVoices(all);
  const saved = loadPref('voice');
  voice = voices.find((v) => v.voiceURI === saved) || voices[0] || null;
}
