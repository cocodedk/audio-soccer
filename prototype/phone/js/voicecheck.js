// The voice check: speak one line with the browser's speech engine and wait for it to really start.
// Talks to speechSynthesis directly, not through speech.js, because the check needs the start event.
const synth = () => { try { return typeof speechSynthesis !== 'undefined' ? speechSynthesis : null; } catch (e) { return null; } };

export const START_MS = 5000; // a line that has not started by then never will

// The engine's voices, waiting up to ms for a list that loads late (Chrome loads it after the page).
export async function loadVoices(ms = 1500) {
  const s = synth();
  if (!s) return [];
  const read = () => { try { return s.getVoices() || []; } catch (e) { return []; } };
  for (let waited = 0; !read().length && waited < ms; waited += 100) await new Promise((r) => setTimeout(r, 100));
  return read();
}

// Speak text and resolve { ok, ms, end } once it starts (ok: true, ms to start), fails or times out
// (ok: false). end resolves when the line is over (or at once if it never started), so the next line
// does not cut it off.
export function speakCheck(text, { voice = null, lang, rate = 1 } = {}) {
  const s = synth();
  if (!s || typeof SpeechSynthesisUtterance === 'undefined') return Promise.resolve({ ok: false, ms: 0, end: Promise.resolve() });
  const t0 = performance.now();
  let ended;
  const end = new Promise((r) => { ended = r; });
  return new Promise((resolve) => {
    let settled = false;
    const settle = (ok) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      if (!ok) { try { s.cancel(); } catch (e) { /* no engine */ } ended(); }
      resolve({ ok, ms: performance.now() - t0, end });
    };
    const timer = setTimeout(() => settle(false), START_MS);
    try {
      const u = new SpeechSynthesisUtterance(text);
      if (voice) u.voice = voice;
      u.lang = voice ? voice.lang : lang;
      u.rate = rate;
      u.onstart = () => settle(true);
      u.onend = () => { settle(true); ended(); }; // an engine that skips onstart still ends a spoken line
      u.onerror = () => { settle(false); ended(); };
      s.cancel();
      s.speak(u);
    } catch (e) { settle(false); }
  });
}
