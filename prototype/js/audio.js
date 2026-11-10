// Web Audio engine: one context, limiter -> master -> destination, a game bus that can be cut
// instantly, the listener on the player, and the crowd bed. Headphones mode places each sound with an
// HRTF panner; speakers mode pans nothing and applies the panner's distance curve as a plain gain.
import { toAudio, dist } from './world.js';
import { speakersOn } from './soundmode.js';
import { distanceGain } from './speakers.js';

const DULL_HZ = 2000;  // behind: keeps the tick's 2.5 kHz tone and the rattle's mid grain
const CROWD_HZ = 550;  // the crowd bed sits below the ball's tick and rattle bands

// The master compressor as a limiter, so no sound peaks far above the rest (the tester's ears are sensitive).
// It must leave ordinary cues alone: in speakers mode volume carries distance and the ball must stay the
// clearest sound. At -20 dB it squeezed the still ball's tick (near vs far: 10.5 dB in, 4 dB out) and its
// automatic makeup gain (+11 dB) lifted the quiet chime. The threshold sits just above the loudest cues:
// the near tick's noise makes its peak vary, about -11 to -7.8 dBFS over 30 renders, so -9 still squeezed
// one in six. The browser adds makeup gain we can't turn off, so MASTER, after the limiter, takes it back:
// a full-scale input peaks at about -7.4 dBFS, under the old chain's -6.7.
const LIMITER = { threshold: -7.5, knee: 0, ratio: 20, attack: 0.003, release: 0.25 };
const MASTER = 0.57;

let ctx = null, limiter = null, master = null, bus = null, noiseBuf = null, crowd = null, listener = null;

// Create (first call) and resume the context. Call from a click or key handler.
export function ensureAudio() {
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    try { ctx = new AC(); } catch (e) { return null; }
    limiter = ctx.createDynamicsCompressor();
    for (const k in LIMITER) limiter[k].value = LIMITER[k];
    master = ctx.createGain();
    master.gain.value = MASTER;
    limiter.connect(master);
    master.connect(ctx.destination);
    noiseBuf = ctx.createBuffer(1, ctx.sampleRate * 2, ctx.sampleRate);
    const d = noiseBuf.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
    bus = newBus();
    faceTheGoal();
  }
  if (ctx.state === 'suspended') ctx.resume().catch(() => {});
  return ctx;
}

export const audioCtx = () => ctx;
export const noise = () => noiseBuf;

function newBus() {
  const g = ctx.createGain();
  g.connect(limiter);
  return g;
}

function setParam3(node, names, v, legacy) {
  if (node[names[0]]) {
    node[names[0]].value = v[0]; node[names[1]].value = v[1]; node[names[2]].value = v[2];
  } else {
    legacy(v);
  }
}

// The listener always faces the attacked goal: forward (0, 0, -1), up (0, 1, 0).
function faceTheGoal() {
  const L = ctx.listener;
  if (L.forwardX) {
    L.forwardX.value = 0; L.forwardY.value = 0; L.forwardZ.value = -1;
    L.upX.value = 0; L.upY.value = 1; L.upZ.value = 0;
  } else {
    L.setOrientation(0, 0, -1, 0, 1, 0);
  }
}

// Put the listener on the player. Called every frame.
export function setListener(p) {
  if (!ctx) return;
  listener = { x: p.x, y: p.y };
  const a = toAudio(p), L = ctx.listener;
  setParam3(L, ['positionX', 'positionY', 'positionZ'], [a.x, a.y, a.z], (v) => L.setPosition(...v));
}

function panner(at) {
  const p = ctx.createPanner();
  p.panningModel = 'HRTF';
  p.distanceModel = 'inverse';
  p.refDistance = 2;
  p.rolloffFactor = 1;
  p.maxDistance = 60;
  const a = toAudio(at);
  setParam3(p, ['positionX', 'positionY', 'positionZ'], [a.x, a.y, a.z], (v) => p.setPosition(...v));
  return p;
}

// The input node for one sound. at: game point (spatial). pan: -1..1 stereo (non-spatial).
// dull: lowpass at DULL_HZ, used for ball sounds behind the player. In speakers mode `at` only sets the
// volume (by distance from the listener) and pan is ignored: the sound stays centred.
export function output({ at = null, pan = 0, gain = 1, dull = false } = {}) {
  const speakers = speakersOn();
  const g = ctx.createGain();
  g.gain.value = speakers && at && listener ? gain * distanceGain(dist(listener, at)) : gain;
  let tail = g;
  if (dull) {
    const f = ctx.createBiquadFilter();
    f.type = 'lowpass'; f.frequency.value = DULL_HZ;
    tail.connect(f); tail = f;
  }
  if (!speakers && at) {
    const p = panner(at);
    tail.connect(p); tail = p;
  } else if (!speakers && pan && ctx.createStereoPanner) {
    const sp = ctx.createStereoPanner();
    sp.pan.value = pan;
    tail.connect(sp); tail = sp;
  }
  tail.connect(bus);
  return g;
}

// Crowd bed: two decorrelated noise loops, panned apart (one loop, centred, in speakers mode),
// band-limited below CROWD_HZ (always, so it never reaches the ball's tick and rattle bands). Created
// on play, stopped on pause.
export function crowdStart(level = 0.045) {
  if (!ctx || crowd) return;
  const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 650; bp.Q.value = 0.5;
  const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = CROWD_HZ;
  const g = ctx.createGain(); g.gain.value = 0;
  const sources = (speakersOn() ? [0] : [-0.8, 0.8]).map((pan, i) => {
    const s = ctx.createBufferSource();
    s.buffer = noiseBuf; s.loop = true;
    const sp = !pan || !ctx.createStereoPanner ? ctx.createGain() : ctx.createStereoPanner();
    if (sp.pan) sp.pan.value = pan;
    s.connect(sp); sp.connect(bp);
    s.start(0, i * 0.9);
    return s;
  });
  bp.connect(lp); lp.connect(g); g.connect(bus);
  g.gain.linearRampToValueAtTime(level, ctx.currentTime + 1);
  crowd = { sources, g };
}

export function crowdLevel(level, secs = 1) {
  if (!crowd) return;
  const t = ctx.currentTime, gain = crowd.g.gain;
  gain.cancelScheduledValues(t);
  gain.setValueAtTime(gain.value, t);
  gain.linearRampToValueAtTime(level, t + secs);
}

function crowdStop() {
  if (!crowd) return;
  const t = ctx.currentTime;
  crowd.sources.forEach((s) => { try { s.stop(t + 0.1); } catch (e) { /* already stopped */ } });
  crowd = null;
}

// Silence everything at once: the crowd bed and any sound still ringing. Used for pause and the menu.
export function stopAll() {
  if (!ctx) return;
  crowdStop();
  const old = bus, t = ctx.currentTime;
  old.gain.setValueAtTime(old.gain.value, t);
  old.gain.linearRampToValueAtTime(0, t + 0.06);
  setTimeout(() => { try { old.disconnect(); } catch (e) { /* gone */ } }, 200);
  bus = newBus();
}
