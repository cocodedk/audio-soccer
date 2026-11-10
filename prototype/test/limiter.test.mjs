import { test } from 'node:test';
import assert from 'node:assert/strict';

// A fake AudioContext that records nodes and connections, installed before audio.js runs ensureAudio().
const param = (v = 0) => ({ value: v });
class Node {
  constructor(kind) { this.kind = kind; this.to = []; }
  connect(n) { this.to.push(n); return n; }
}
class FakeCtx {
  constructor() {
    this.sampleRate = 8000; this.state = 'running'; this.destination = new Node('destination');
    this.listener = { forwardX: param(), forwardY: param(), forwardZ: param(), upX: param(), upY: param(), upZ: param() };
    this.nodes = [];
  }
  add(n) { this.nodes.push(n); return n; }
  createGain() { return this.add(Object.assign(new Node('gain'), { gain: param(1) })); }
  createDynamicsCompressor() {
    const c = new Node('compressor');
    for (const k of ['threshold', 'knee', 'ratio', 'attack', 'release']) c[k] = param();
    return this.add(c);
  }
  createBuffer(ch, len) { const d = new Float32Array(len); return { getChannelData: () => d }; }
}
globalThis.window = { AudioContext: FakeCtx };
const { ensureAudio } = await import('../js/audio.js');
const ctx = ensureAudio();
const comp = ctx.nodes.find((n) => n.kind === 'compressor');
const master = comp.to[0];

// The Web Audio spec's static curve (knee 0) and its automatic makeup gain, in dB.
const db = (g) => 20 * Math.log10(g);
const curve = (x) => (x < comp.threshold.value ? x : comp.threshold.value + (x - comp.threshold.value) / comp.ratio.value);
const makeup = -0.6 * curve(0);
const out = (x) => curve(x) + makeup + db(master.gain.value);

test('limiter: the game bus goes through the limiter, then the master gain, then out', () => {
  const bus = ctx.nodes.find((n) => n.kind === 'gain' && n.to[0] === comp);
  assert.ok(bus, 'the bus feeds the limiter');
  assert.equal(master.kind, 'gain');
  assert.deepEqual(master.to, [ctx.destination]);
});

test('limiter: ordinary cues pass untouched, so the near/far tick keeps its full volume difference', () => {
  // Worst-case dBFS peak of the near tick (random noise, so it varies: -11 to -7.8 over 30 offline renders);
  // the board knock peaks lower, near -11.5.
  const LOUDEST_CUE = -7.8;
  assert.ok(comp.threshold.value >= LOUDEST_CUE + 0.25, `threshold ${comp.threshold.value} too close to the cues`);
  const near = LOUDEST_CUE, far = near + db(0.3); // distanceGain 1 vs its floor 0.3
  assert.ok(Math.abs(out(near) - out(far) - (near - far)) < 1e-9);
});

// A static estimate from the spec's formula; Chromium's measured render runs about 0.3 dB louder (-7.4 dBFS).
test('limiter: a full-scale input comes out no louder than before (old chain about -6.7 dBFS)', () => {
  assert.ok(out(0) <= -7.5, `full-scale static output ${out(0).toFixed(2)} dBFS`);
});
