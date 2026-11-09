// Shared test helpers (node --test also loads this file; it defines no tests).
import assert from 'node:assert/strict';
import { step } from '../js/world.js';

export const close = (a, b, eps = 1e-9) => assert.ok(Math.abs(a - b) < eps, `${a} != ${b}`);
export const types = (events) => events.map((e) => e.type);

// Step a world for secs seconds at 120 Hz with a fixed input; returns every event.
export const run = (w, secs, input = {}, dt = 1 / 120) => {
  const events = [];
  for (let t = 0; t < secs; t += dt) events.push(...step(w, dt, input));
  return events;
};

// A small seeded random generator, so simulated rates are the same on every run.
export const seeded = (a) => () => {
  a = (a + 0x6D2B79F5) | 0;
  let t = Math.imul(a ^ (a >>> 15), 1 | a);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

// A fake speech engine, installed as the browser's: speak() records the line and keeps it "playing"
// until finish() is called. Install it before importing speech.js.
export function fakeSpeech() {
  const spoken = [];
  let playing = null;
  globalThis.SpeechSynthesisUtterance = class { constructor(text) { this.text = text; } };
  globalThis.speechSynthesis = {
    speaking: false, pending: false,
    cancel() { this.speaking = false; playing = null; },
    speak(u) { spoken.push(u.text); playing = u; this.speaking = true; },
    getVoices: () => [], addEventListener() {},
  };
  const finish = () => { const u = playing; speechSynthesis.speaking = false; playing = null; if (u && u.onend) u.onend({}); };
  const finishAll = () => { for (let i = 0; i < 30 && playing; i++) finish(); };
  return { spoken, finish, finishAll };
}
