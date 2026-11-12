// The results block of the phone test: every session so far as text to copy, mirrored to localStorage
// so a reload keeps it (the trial's pattern, under a key of its own). With no storage it lives in memory.
import { loadPref, savePref } from '../../js/storage.js';
import { t } from '../../js/i18n/index.js';
import { resultsText } from './report.js';

const KEY = 'phone.sessions';
const valid = (s) => s && typeof s.at === 'string' && Array.isArray(s.schemes) && Array.isArray(s.voices);

// els: { box, text, copyBtn, copyStatus, heading }
export function createResults({ box, text, copyBtn, copyStatus, heading }) {
  let sessions = [];
  try {
    const saved = JSON.parse(loadPref(KEY, '[]'));
    if (Array.isArray(saved)) sessions = saved.filter(valid);
  } catch (e) { /* unreadable: start empty */ }

  copyBtn.addEventListener('click', () => {
    const selectIt = () => { copyStatus.textContent = t('phone.copyBlocked'); text.focus(); text.select(); };
    try {
      navigator.clipboard.writeText(text.value).then(() => { copyStatus.textContent = t('copy.done'); }, selectIt);
    } catch (e) { selectIt(); }
  });

  const api = {
    // Start a session record; it is saved now and again at every save().
    add(session) { sessions.push(session); api.save(); return session; },
    save() { savePref(KEY, JSON.stringify(sessions)); },
    show(focus = false) {
      if (!sessions.length) return;
      text.value = resultsText(sessions);
      copyStatus.textContent = '';
      box.hidden = false;
      if (focus) heading.focus();
    },
  };
  return api;
}
