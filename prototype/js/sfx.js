// Synthesized sound effects. Each one builds fresh nodes into an output() and lets them be collected
// after they stop. play(name, opts) never throws: a failed sound must not break the game.
import { audioCtx, noise, output } from './audio.js';

function burst(dest, { dur = 0.04, type = 'bandpass', freq = 3000, q = 1, gain = 1, at = 0, attack = 0.002 } = {}) {
  const ctx = audioCtx(), t = ctx.currentTime + at;
  const s = ctx.createBufferSource(), f = ctx.createBiquadFilter(), g = ctx.createGain();
  s.buffer = noise();
  s.loop = true; // the 2 s buffer would run out under a long burst (the cheer) and cut it off with a click
  f.type = type; f.frequency.value = freq; f.Q.value = q;
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(gain, t + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  s.connect(f); f.connect(g); g.connect(dest);
  s.start(t, Math.random() * 1.5);
  s.stop(t + dur + 0.05);
}

function tone(dest, { f0 = 140, f1 = 50, dur = 0.14, gain = 1, type = 'sine', at = 0, attack = 0.003 } = {}) {
  const ctx = audioCtx(), t = ctx.currentTime + at;
  const o = ctx.createOscillator(), g = ctx.createGain();
  o.type = type;
  o.frequency.setValueAtTime(f0, t);
  o.frequency.exponentialRampToValueAtTime(f1, t + dur);
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(gain, t + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  o.connect(g); g.connect(dest);
  o.start(t);
  o.stop(t + dur + 0.05);
}

function whistleBlast(dest, at, len) {
  const ctx = audioCtx(), t = ctx.currentTime + at;
  [2900, 3060].forEach((fr) => {
    const o = ctx.createOscillator(), lfo = ctx.createOscillator(), lg = ctx.createGain(), g = ctx.createGain();
    o.frequency.value = fr; lfo.frequency.value = 27; lg.gain.value = 110;
    lfo.connect(lg); lg.connect(o.frequency);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(0.055, t + 0.02);
    g.gain.setValueAtTime(0.055, t + len);
    g.gain.exponentialRampToValueAtTime(0.0001, t + len + 0.06);
    o.connect(g); g.connect(dest);
    o.start(t); lfo.start(t);
    o.stop(t + len + 0.1); lfo.stop(t + len + 0.1);
  });
}

const SFX = {
  // Metallic grains of the ball's rattle, plus one mid-range grain that still gets through the
  // 1200 Hz lowpass a ball behind you goes through, so it sounds duller there but not lost.
  rattle(d) {
    const n = 2 + Math.round(Math.random());
    for (let i = 0; i < n; i++) burst(d, { dur: 0.016, freq: 4200 + Math.random() * 1600, q: 9, gain: 0.85, at: Math.random() * 0.03 });
    burst(d, { dur: 0.02, freq: 1200 + Math.random() * 300, q: 3.5, gain: 0.5, at: Math.random() * 0.03 });
  },
  // A still ball's soft tick, so a stopped ball is never silent. The 950 Hz body survives the lowpass.
  tick(d) {
    burst(d, { dur: 0.02, freq: 4800, q: 10, gain: 0.7 });
    tone(d, { f0: 2500, f1: 2300, dur: 0.06, gain: 0.12 });
    tone(d, { f0: 950, f1: 900, dur: 0.05, gain: 0.15 });
  },
  tap(d) {
    tone(d, { f0: 180, f1: 80, dur: 0.07, gain: 0.3 });
    burst(d, { dur: 0.018, freq: 4600, q: 8, gain: 0.28 });
  },
  kick(d) {
    tone(d, { f0: 150, f1: 45, dur: 0.13, gain: 0.1 });
    burst(d, { dur: 0.03, type: 'lowpass', freq: 1500, gain: 0.06 });
  },
  // Side boards: a wooden knock.
  board(d) {
    tone(d, { f0: 210, f1: 150, dur: 0.1, gain: 0.3, type: 'triangle' });
    burst(d, { dur: 0.09, freq: 750, q: 3, gain: 0.38 });
  },
  // End walls: lower and heavier than the side boards.
  endwall(d) {
    tone(d, { f0: 105, f1: 62, dur: 0.18, gain: 0.14, type: 'triangle' });
    burst(d, { dur: 0.12, freq: 330, q: 2.5, gain: 0.15 });
  },
  // A footstep near a side board: a soft brush on that side.
  brush(d) {
    burst(d, { dur: 0.13, freq: 1900, q: 0.8, gain: 0.4, attack: 0.04 });
  },
  step(d) {
    burst(d, { dur: 0.05, type: 'lowpass', freq: 420, gain: 0.9 });
  },
  // Goal beacon: a soft two-note chime. o.k (0..1) shortens it for the quick beeps of speakers mode.
  chime(d, o) {
    const k = Math.max(0.35, Math.min(1, o.k ?? 1));
    tone(d, { f0: 784, f1: 780, dur: 0.6 * k, gain: 0.21, attack: 0.01 });
    tone(d, { f0: 1175, f1: 1170, dur: 0.75 * k, gain: 0.18, attack: 0.01, at: 0.17 * k });
  },
  net(d) {
    burst(d, { dur: 0.45, type: 'highpass', freq: 2600, gain: 0.045, attack: 0.05 });
    burst(d, { dur: 0.3, freq: 900, q: 0.7, gain: 0.04, attack: 0.02 });
  },
  cheer(d) {
    burst(d, { dur: 2.8, freq: 1100, q: 0.5, gain: 0.11, attack: 0.25 });
    burst(d, { dur: 2.2, freq: 2600, q: 0.8, gain: 0.05, attack: 0.3 });
  },
  whistle(d) {
    whistleBlast(d, 0, 0.3);
  },
  // Post: a ringing metallic clang.
  clang(d) {
    [523, 1287, 2093, 3011].forEach((f, i) => tone(d, { f0: f, f1: f * 0.995, dur: 1 - i * 0.18, gain: 0.034 / (i + 1), attack: 0.001 }));
    burst(d, { dur: 0.02, freq: 3000, q: 2, gain: 0.042 });
  },
  // Keeper save: a glove slap and a thud.
  save(d) {
    tone(d, { f0: 200, f1: 90, dur: 0.12, gain: 0.1 });
    burst(d, { dur: 0.06, type: 'lowpass', freq: 900, gain: 0.1 });
    burst(d, { dur: 0.03, freq: 2200, q: 1.5, gain: 0.08, at: 0.01 });
  },
};

export function play(name, opts = {}) {
  const ctx = audioCtx();
  if (!ctx || ctx.state === 'closed' || !SFX[name]) return;
  try { SFX[name](output(opts), opts); } catch (e) { /* a failed sound never breaks the game */ }
}
