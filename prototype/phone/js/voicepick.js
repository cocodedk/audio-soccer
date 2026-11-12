// PURE: which voice the phone test may speak with, from the engine's voice list. Only a voice on the
// device counts (voices.js local()): online voices may be blocked in Iran and then never speak.
import { pickVoices, local } from '../../js/voices.js';

// { lang, voice, english } to speak with, or null when nothing on the device can speak.
// - a local voice of the chosen language: that one;
// - Persian chosen, no local Persian voice, a local English one: English (english: true);
// - an empty list: the browser's default voice (voice: null), whether it speaks is checked by ear;
// - otherwise (only online voices, or none in either language): null.
export function planVoice(list, lang) {
  const own = pickVoices(list, lang).filter(local);
  if (own.length) return { lang, voice: own[0], english: false };
  const en = lang === 'en' ? [] : pickVoices(list, 'en').filter(local);
  if (en.length) return { lang: 'en', voice: en[0], english: true };
  if (!list.length) return { lang, voice: null, english: false };
  return null;
}

// What the results record about the voices found: the count, the English and Persian ones, and whether
// a Persian one is on the device or only online.
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
