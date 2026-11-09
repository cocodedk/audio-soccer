// The string tables and the lookup that fills them. Pure: no DOM, no storage, so describe.js and
// levels.js can use it. The current language lives in index.js; here a language is always passed in.
import en from './en.js';
import fa from './fa.js';

export const TABLES = { en, fa };
export const LANGS = ['en', 'fa'];

const get = (obj, key) => key.split('.').reduce((o, k) => (o == null ? o : o[k]), obj);
const table = (lang) => TABLES[lang] || en;
const localise = (S, text) => (S.digits ? S.digits(text) : text);

// tr(lang, 'guide.head', { m: '3 metres' }): the entry at a dotted key. A string has its {slots}
// filled; a function entry gets the params. A key missing from the language falls back to English.
export function tr(lang, key, params = {}) {
  const S = table(lang);
  const v = get(S, key) ?? get(en, key);
  if (v === undefined) return key;
  const text = typeof v === 'function' ? v(params) : v.replace(/\{(\w+)\}/g, (m, k) => (k in params ? params[k] : m));
  return localise(S, text);
}

// trList(lang, 'help'): an entry that is an array of strings, each localised.
export function trList(lang, key) {
  const S = table(lang);
  return (get(S, key) ?? get(en, key) ?? []).map((x) => localise(S, x));
}

// entry(lang, 'canvas'): an entry that is an object of strings, as it is (no localising of digits).
export const entry = (lang, key) => get(table(lang), key) ?? get(en, key) ?? {};
