// The language setting: the radios, the page text, the voices of the language, and the fallback for
// Persian on a browser with no Persian voice: the player's own screen reader, which has one, reads the
// game instead. Picking English again puts back the voice mode the player had before.
import { getLang, setLang, onLang, t } from './i18n/index.js';
import { applyLanguage } from './i18n/dom.js';
import { chooseVoices, missingVoice, listVoices } from './voices.js';
import { relabel } from './menu.js';
import { say, cancel } from './speech.js';
import { stopLearn } from './learn.js';
import { loadPref, savePref } from './storage.js';

const $ = (s) => document.querySelector(s);
// The voice mode radio that is checked: 'voice' (the built-in voice) or 'sr' (the screen reader).
export const modeNow = () => $('input[name="voice-mode"]:checked')?.value || 'voice';

// applyMode() sends the voice mode radios to the announcer; onChange() redraws what main.js owns.
export function createLanguage({ applyMode, onChange }) {
  const noticeEl = $('#voice-notice');
  // True while a check of the Persian voices is due: at start with Persian saved, or just after picking it.
  let pending = getLang() === 'fa';

  const pickMode = (m) => {
    const r = $(`input[name="voice-mode"][value="${m}"]`);
    if (r) { r.checked = true; applyMode(); }
  };
  const showRadios = () => document.querySelectorAll('input[name="lang"]').forEach((r) => { r.checked = r.value === getLang(); });

  onLang(() => {
    applyLanguage();
    chooseVoices();
    relabel();
    showRadios();
    onChange();
  });
  applyLanguage();
  showRadios();
  noticeEl.lang = 'fa';
  noticeEl.dir = 'rtl';

  // Persian with no Persian voice on the device (online-only counts as none, see missingVoice):
  // switch to the screen reader and say so, once the browser's voice list is known. Returns true when it did. A player already on their screen reader
  // keeps it; so does one whose browser has a Persian voice.
  function check() {
    if (!pending || getLang() !== 'fa') return false;
    if (!missingVoice()) { if (listVoices().length) pending = false; return false; }
    pending = false;
    // The player chose the built-in voice for Persian themselves on an earlier visit: their online voice
    // works for them, so it is not taken away on every reload. Not when there is no Persian voice at all.
    if (loadPref('mode-picked-fa') === 'voice' && listVoices().length) return false;
    if (modeNow() !== 'voice') return false;
    savePref('restore-mode', 'voice');
    pickMode('sr');
    noticeEl.textContent = t('lang.fallback');
    noticeEl.hidden = false;
    say(t('lang.fallback'), { priority: 'high' });
    return true;
  }

  function choose(l) {
    stopLearn();
    cancel();
    setLang(l);
    pending = l === 'fa';
    noticeEl.hidden = true;
    if (l === 'en') {
      const back = loadPref('restore-mode');
      if (back) { savePref('restore-mode', ''); pickMode(back); }
    }
    if (!check()) say(t('lang.set'), { priority: 'high' });
  }

  document.querySelectorAll('input[name="lang"]').forEach((r) => r.addEventListener('change', () => { if (r.checked) choose(r.value); }));
  // A voice mode the player picks themselves is theirs: it is not undone by going back to English.
  // The fallback notice goes with it.
  // The voice mode isn't saved, so the choice made in Persian is kept for check() on the next visit.
  document.querySelectorAll('input[name="voice-mode"]').forEach((r) => r.addEventListener('change', () => {
    savePref('restore-mode', '');
    if (getLang() === 'fa') savePref('mode-picked-fa', r.value);
    noticeEl.hidden = true;
  }));

  return { check };
}
