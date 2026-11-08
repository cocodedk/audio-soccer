// Web Audio engine: one context, master -> compressor -> destination, a game bus that can be cut
// instantly, HRTF panners at game positions, the listener on the player, and the crowd bed.
import { toAudio } from './world.js';

const DULL_HZ = 2000;  // behind: keeps the tick's 2.5 kHz tone and the rattle's mid grain
const CROWD_HZ = 550;  // the crowd bed sits below the ball's tick and rattle bands

let ctx = null, master = null, bus = null, noiseBuf = null, crowd = null;

// Create (first call) and resume the context. Call from a click or key handler.
export function ensureAudio() {
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    try { ctx = new AC(); } catch (e) { return null; }
    const comp = ctx.createDynamicsCompressor();
    master = ctx.createGain();
    master.gain.value = 0.9;
    master.connect(comp);
    comp.connect(ctx.destination);
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
  g.connect(master);
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
// dull: lowpass at DULL_HZ, used for ball sounds behind the player.
export function output({ at = null, pan = 0, gain = 1, dull = false } = {}) {
  const g = ctx.createGain();
  g.gain.value = gain;
  let tail = g;
  if (dull) {
    const f = ctx.createBiquadFilter();
    f.type = 'lowpass'; f.frequency.value = DULL_HZ;
    tail.connect(f); tail = f;
  }
  if (at) {
    const p = panner(at);
    tail.connect(p); tail = p;
  } else if (pan && ctx.createStereoPanner) {
    const sp = ctx.createStereoPanner();
    sp.pan.value = pan;
    tail.connect(sp); tail = sp;
  }
  tail.connect(bus);
  return g;
}

// Crowd bed: two decorrelated noise loops, panned apart, band-limited below CROWD_HZ (always, so it
// never reaches the ball's tick and rattle bands). Created on play, stopped on pause.
export function crowdStart(level = 0.045) {
  if (!ctx || crowd) return;
  const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 650; bp.Q.value = 0.5;
  const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = CROWD_HZ;
  const g = ctx.createGain(); g.gain.value = 0;
  const sources = [-0.8, 0.8].map((pan, i) => {
    const s = ctx.createBufferSource();
    s.buffer = noiseBuf; s.loop = true;
    const sp = ctx.createStereoPanner ? ctx.createStereoPanner() : ctx.createGain();
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
