// The current language: persisted, with the lookups bound to it. Switching sets <html lang dir>
// through the listeners (see dom.js).
import { loadPref, savePref } from '../storage.js';
import { LANGS, tr, trList, entry } from './tables.js';

export { LANGS };
export const LANG_TAG = { en: 'en-GB', fa: 'fa-IR' }; // speech utterance language
export const DIR = { en: 'ltr', fa: 'rtl' };

let lang = (() => { const l = loadPref('lang'); return LANGS.includes(l) ? l : 'en'; })();
const listeners = [];

export const getLang = () => lang;
export const t = (key, params) => tr(lang, key, params);
export const tl = (key) => trList(lang, key);
export const to = (key) => entry(lang, key);
export const onLang = (fn) => { listeners.push(fn); };

export function setLang(l) {
  if (!LANGS.includes(l) || l === lang) return lang;
  lang = l;
  savePref('lang', l);
  listeners.forEach((fn) => fn(l));
  return lang;
}
