// Announcer. Two modes: 'voice' (built-in speechSynthesis) and 'sr' (aria-live regions for the
// player's own screen reader). Priority queue: high > normal > low. A higher line cancels lower ones;
// a line still unsaid after its wait (1.5 s unless the caller asks for more) is dropped, never said late.
import { currentVoice, getRate, refreshVoices } from './voices.js';

const PRI = { low: 0, normal: 1, high: 2 };
const MAX_WAIT = 1500;
const CANCEL_GAP = 30; // ms: Chrome can swallow a speak() that comes right after a cancel()
const now = () => performance.now();

let mode = 'voice', queue = [], current = null, regions = null, cancelledAt = -Infinity;
const lastSaid = {};
const pending = { polite: [], assertive: [] }, flushTimer = {};

function synth() {
  try { return typeof speechSynthesis !== 'undefined' ? speechSynthesis : null; } catch (e) { return null; }
}

const canSpeak = () => !!synth() && typeof SpeechSynthesisUtterance !== 'undefined';

function hush() {
  cancelledAt = now();
  try { synth()?.cancel(); } catch (e) { /* no speech engine */ }
}

// onVoices is called whenever the engine's voice list loads or changes.
export function initSpeech(politeEl, assertiveEl, onVoices = () => {}) {
  regions = { polite: politeEl, assertive: assertiveEl };
  const load = () => {
    try { refreshVoices(synth()?.getVoices() || []); } catch (e) { /* no speech engine */ }
    onVoices();
  };
  load();
  try { synth()?.addEventListener?.('voiceschanged', load); } catch (e) { /* no speech engine */ }
  setInterval(pump, 200);
}

export function setMode(m) {
  const next = m === 'sr' ? 'sr' : 'voice';
  if (next !== mode) cancel();
  mode = next;
}

export function cancel() {
  queue = [];
  current = null;
  pending.polite = []; pending.assertive = [];
  hush();
}

// Post to a live region. Lines sent close together are joined into one write, so none overwrites
// another, and the write waits a moment so a focus move made at the same time is announced first.
// Appending or dropping a zero-width space makes repeated text still announce.
function announce(item) {
  const key = item.p >= PRI.high ? 'assertive' : 'polite';
  if (!regions?.[key]) return;
  if (item.key) lastSaid[item.key] = now();
  pending[key].push(/[.!?]$/.test(item.text) ? item.text : `${item.text}.`);
  if (flushTimer[key]) return;
  flushTimer[key] = setTimeout(() => {
    flushTimer[key] = null;
    const el = regions[key], lines = pending[key];
    pending[key] = [];
    if (lines.length) el.textContent = lines.join(' ') + (el.textContent.endsWith('​') ? '' : '​');
  }, 100);
}

function speakNow(item) {
  const s = synth();
  if (!canSpeak()) { current = null; announce(item); return; }
  if (item.key) lastSaid[item.key] = now(); // a line counts as said only once it is actually spoken
  try {
    const u = new SpeechSynthesisUtterance(item.text);
    const v = currentVoice();
    if (v) u.voice = v;
    u.lang = v ? v.lang : 'en-GB';
    u.rate = getRate();
    u.pitch = item.pitch;
    // pump() only starts a line when it takes the engine to be idle: anything still playing is a
    // leftover and is cut, so the new line is never queued behind it and said late.
    try { if (s.speaking || s.pending) hush(); } catch (e) { /* no speech engine */ }
    const delay = Math.max(0, CANCEL_GAP - (now() - cancelledAt));
    // A line is taken as stuck only after its own length (about 90 ms a character) plus room for a
    // slow (network) voice to start.
    current = { ...item, started: now() + delay, maxMs: 2500 + 90 * item.text.length, u };
    const done = () => { if (current && current.u === u) { current = null; pump(); } };
    u.onend = done;
    u.onerror = (e) => {
      // A real failure (not our own cancel) falls back to the live regions so the line is not lost.
      if (!/interrupted|canceled/.test(e?.error || '')) announce(item);
      done();
    };
    const go = () => { if (current && current.u === u) s.speak(u); };
    if (delay) setTimeout(go, delay); else go();
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
  queue = queue.filter((q) => t - q.at <= q.wait);
  if (!queue.length) return;
  queue.sort((a, b) => b.p - a.p || a.at - b.at);
  speakNow(queue.shift());
}

// True while a line with this key is being spoken or waits in the queue (built-in voice only).
export const isSaying = (key) => current?.key === key || queue.some((q) => q.key === key);

// say(text, { priority, key, gap, pitch, wait, cue, many }). key + gap (ms) rate-limit a kind of line, measured
// from when it was last spoken; a newer line with the same key replaces one still queued, unless
// many is set (a sequence of lines sharing one key, such as the help). wait is how
// long (ms) the line may sit unsaid. cue: an ambient cue (thirds, ball behind, goal guide), spoken in
// the built-in voice only, because a live region can neither expire a late line nor cancel it.
export function say(text, { priority = 'normal', key = null, gap = 0, pitch = 1, wait = MAX_WAIT, cue = false, many = false } = {}) {
  if (cue && mode === 'sr') return false;
  const t = now();
  if (key && gap && lastSaid[key] !== undefined && t - lastSaid[key] < gap) return false;
  const p = PRI[priority] ?? PRI.normal;
  const item = { text, p, key, at: t, pitch, wait };
  // Screen reader mode, or no speech engine at all: the live regions carry the line.
  if (mode === 'sr' || !canSpeak()) { announce(item); return true; }
  queue = queue.filter((q) => q.p >= p && !(key && !many && q.key === key));
  if (current && current.p < p) { current = null; hush(); }
  queue.push(item);
  pump();
  return true;
}
