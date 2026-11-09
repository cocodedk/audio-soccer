// The Sound setting: 'speakers' (default: no panning, distance by volume, spoken side calls) or
// 'headphones' (HRTF and stereo). Persisted like the speech rate.
import { loadPref, savePref } from './storage.js';

export const SOUND_MODES = ['speakers', 'headphones'];

let mode = (() => { const m = loadPref('sound'); return SOUND_MODES.includes(m) ? m : 'speakers'; })();

export const getSoundMode = () => mode;
export const speakersOn = () => mode === 'speakers';

export function setSoundMode(m) {
  if (SOUND_MODES.includes(m)) { mode = m; savePref('sound', m); }
  return mode;
}
