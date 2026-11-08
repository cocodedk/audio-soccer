// localStorage that never throws: the page works without it (private window, blocked site data).
const PREFIX = 'audiosoccer.';

export function loadPref(key, fallback = null) {
  try {
    const v = localStorage.getItem(PREFIX + key);
    return v === null ? fallback : v;
  } catch (e) { return fallback; }
}

export function savePref(key, value) {
  try { localStorage.setItem(PREFIX + key, String(value)); } catch (e) { /* storage blocked */ }
}
