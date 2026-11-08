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
