// Announcer. Two modes: 'voice' (built-in speechSynthesis) and 'sr' (aria-live regions for the
// player's own screen reader). Priority queue: high > normal > low. A higher line cancels lower ones;
// a line still unsaid after 1.5 s is dropped, never said late.
const PRI = { low: 0, normal: 1, high: 2 };
const MAX_WAIT = 1500;
const now = () => performance.now();

let mode = 'voice', voice = null, queue = [], current = null, regions = null;
const lastSaid = {};

function synth() {
  try { return typeof speechSynthesis !== 'undefined' ? speechSynthesis : null; } catch (e) { return null; }
}

function pickVoice() {
  try {
    const vs = synth()?.getVoices() || [];
    voice = vs.find((v) => /^en[-_]GB/i.test(v.lang)) || vs.find((v) => /^en/i.test(v.lang)) || null;
  } catch (e) { voice = null; }
}

function hush() {
  try { synth()?.cancel(); } catch (e) { /* no speech engine */ }
}

export function initSpeech(politeEl, assertiveEl) {
  regions = { polite: politeEl, assertive: assertiveEl };
  try {
    pickVoice();
    synth()?.addEventListener?.('voiceschanged', pickVoice);
  } catch (e) { /* no speech engine */ }
  setInterval(pump, 200);
}

export function setMode(m) {
  const next = m === 'sr' ? 'sr' : 'voice';
  if (next !== mode) cancel();
  mode = next;
}

const pending = { polite: [], assertive: [] }, flushTimer = {};
const canSpeak = () => !!synth() && typeof SpeechSynthesisUtterance !== 'undefined';

export function cancel() {
  queue = [];
  current = null;
  pending.polite = []; pending.assertive = [];
  hush();
}

// Post to a live region. Lines sent close together are joined into one write, so none overwrites
// another, and the write waits a moment so a focus move made at the same time is announced first.
// Appending or dropping a zero-width space makes repeated text still announce.
function announce(text, p) {
  const key = p >= PRI.high ? 'assertive' : 'polite';
  if (!regions?.[key]) return;
  pending[key].push(/[.!?]$/.test(text) ? text : `${text}.`);
  if (flushTimer[key]) return;
  flushTimer[key] = setTimeout(() => {
    flushTimer[key] = null;
    const el = regions[key], lines = pending[key];
    pending[key] = [];
    if (lines.length) el.textContent = lines.join(' ') + (el.textContent.endsWith('\u200b') ? '' : '\u200b');
  }, 100);
}

function speakNow(item) {
  const s = synth();
  if (!canSpeak()) { current = null; announce(item.text, item.p); return; }
  try {
    const u = new SpeechSynthesisUtterance(item.text);
    if (voice) u.voice = voice;
    u.lang = voice ? voice.lang : 'en-GB';
    u.rate = item.rate;
    u.pitch = item.pitch;
    // pump() only starts a line when it takes the engine to be idle: anything still playing is a
    // leftover and is cut, so the new line is never queued behind it and said late.
    try { if (s.speaking || s.pending) s.cancel(); } catch (e) { /* no speech engine */ }
    // A line is taken as stuck only after its own length (about 90 ms a character) plus room for a
    // slow (network) voice to start, so the long help is never cut off by the stuck-line fallback.
    current = { ...item, started: now(), maxMs: 2500 + 90 * item.text.length, u };
    const done = () => { if (current && current.u === u) { current = null; pump(); } };
    u.onend = done;
    u.onerror = (e) => {
      // A real failure (not our own cancel) falls back to the live regions so the line is not lost.
      if (!/interrupted|canceled/.test(e?.error || '')) announce(item.text, item.p);
      done();
    };
    s.speak(u);
  } catch (e) {
    current = null;
  }
}

function pump() {
  if (mode !== 'voice') return;
  const s = synth();
  const t = now();
  if (current) {
    let busy = false;
    try { busy = !!(s && (s.speaking || s.pending)); } catch (e) { busy = false; }
    // onend is unreliable in some browsers: treat a silent engine or a stuck line as finished.
    if ((!busy && t - current.started > 400) || t - current.started > current.maxMs) current = null;
    else return;
  }
  queue = queue.filter((q) => t - q.at <= MAX_WAIT);
  if (!queue.length) return;
  queue.sort((a, b) => b.p - a.p || a.at - b.at);
  speakNow(queue.shift());
}

// say(text, { priority, key, gap, pitch, rate }): key + gap (ms) rate-limit a kind of line.
export function say(text, { priority = 'normal', key = null, gap = 0, pitch = 1, rate = 1.1 } = {}) {
  const t = now();
  if (key && gap && lastSaid[key] !== undefined && t - lastSaid[key] < gap) return false;
  if (key) lastSaid[key] = t;
  const p = PRI[priority] ?? PRI.normal;
  // Screen reader mode, or no speech engine at all: the live regions carry the line.
  if (mode === 'sr' || !canSpeak()) { announce(text, p); return true; }
  queue = queue.filter((q) => q.p >= p);
  if (current && current.p < p) { current = null; hush(); }
  queue.push({ text, p, at: t, pitch, rate });
  pump();
  return true;
}
