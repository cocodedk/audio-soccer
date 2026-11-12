// PURE: which voices the phone test tries, in order, from the engine's voice list. A voice counts only
// once it has really started speaking (voicecheck.js): the local flag (voices.js local()) decides the
// order, never whether a voice is tried, because Android browsers are known to set it wrongly.
import { pickVoices, local } from '../../js/voices.js';

export const MAX_TRIES = 3;

// [{ lang, voice, english }], best first, at most MAX_TRIES: a local voice of the chosen language, a
// local English one (Persian chosen), an online voice of the chosen language, an online English one.
// Local first keeps the offline preference (online voices may be blocked in Iran). english: the voice is
// English though Persian was chosen. An empty list (not loaded, or a browser that lists nothing) gives
// the browser's default voice (voice: null); a list with neither language gives nothing to try.
export function voiceCandidates(list, lang) {
  if (!list.length) return [{ lang, voice: null, english: false }];
  const own = pickVoices(list, lang), en = lang === 'en' ? [] : pickVoices(list, 'en');
  const first = (voices, isLocal) => voices.find((v) => local(v) === isLocal);
  return [[own, true, lang], [en, true, 'en'], [own, false, lang], [en, false, 'en']]
    .map(([voices, isLocal, l]) => ({ lang: l, voice: first(voices, isLocal), english: l !== lang }))
    .filter((c) => c.voice)
    .slice(0, MAX_TRIES);
}

// What the results record about a voice, and about the voices listed: the count, the English and Persian
// ones, and whether a Persian one is flagged local or online.
export const describeVoice = (v) => ({ name: v.name, lang: v.lang, local: local(v) });

export function voiceFacts(list) {
  const fa = pickVoices(list, 'fa');
  return {
    voiceCount: list.length,
    voices: [...pickVoices(list, 'en'), ...fa].map(describeVoice),
    persianLocal: fa.some(local),
    persianOnline: fa.some((v) => !local(v)),
  };
}
