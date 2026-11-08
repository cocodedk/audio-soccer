// The start-screen sound tour and the headphone check. Both go through the same HRTF path as play
// (play() with an `at` point, the listener placed with setListener), so what you learn is what you hear.
import { setListener } from './audio.js';
import { play } from './sfx.js';
import { ballOpts, tickOpts } from './soundscape.js';
import { say, cancel } from './speech.js';
import { getRate } from './voices.js';
import { GOAL } from './world.js';

const ME = { x: 20, y: 10 };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let run = 0;

// Every sound here is scheduled with later(), so stopLearn() can cancel those still waiting: nothing
// from the tour or the headphone check may sound once a level has started.
const timers = new Set();
const later = (fn, ms) => {
  const id = setTimeout(() => { timers.delete(id); fn(); }, ms);
  timers.add(id);
};

// A sound hard left, then hard right: points 3 m to either side of the listener, each named first.
export function headphoneCheck() {
  setListener(ME);
  const side = (y, word) => {
    [0, 250, 500].forEach((ms) => later(() => play('kick', { at: { x: ME.x, y }, gain: 0.9 }), ms));
    say(word, { priority: 'high' });
  };
  side(ME.y - 3, 'Left');
  later(() => side(ME.y + 3, 'Right'), 1500);
}

const ahead = { x: ME.x + 5, y: ME.y }, behind = { x: ME.x - 5, y: ME.y };
const ticks = (at) => [0, 600, 1200].forEach((ms) => later(() => play('tick', tickOpts(ME.x, at)), ms));
const rolling = () => {
  for (let i = 0; i < 14; i++) {
    const at = { x: ME.x + 4, y: 4 + i * 12 / 13 };
    later(() => play('rattle', ballOpts(ME.x, at, 0.8, 2)), i * 100);
  }
};
const knock = (y) => () => play('board', { at: { x: ME.x, y }, gain: 1 });
const feet = () => [0, 350, 700, 1050].forEach((ms) => later(() => play('step', { gain: 0.22 }), ms));

// name: what the voice says first. at: where the listener stands. go: the sound. ms: how long it lasts.
const STEPS = [
  { name: 'Ball ticking, ahead of you.', go: () => ticks(ahead), ms: 2000 },
  { name: 'The same ball, behind you. It sounds duller.', go: () => ticks(behind), ms: 2000 },
  { name: 'Ball rolling from your left to your right.', go: rolling, ms: 1700 },
  { name: 'The goal chime, ahead.', go: () => play('chime', { at: GOAL, gain: 0.4 }), ms: 1500 },
  { name: 'A knock on the left board.', at: { x: ME.x, y: 3 }, go: knock(0), ms: 900 },
  { name: 'And on the right board.', at: { x: ME.x, y: 17 }, go: knock(20), ms: 900 },
  { name: 'Your footsteps.', go: feet, ms: 1500 },
  { name: 'The keeper shouts voy.', go: () => say('Voy!', { priority: 'high', pitch: 1.4 }), ms: 1200 },
];

export function stopLearn() {
  run += 1;
  timers.forEach(clearTimeout);
  timers.clear();
}

// Each step: the voice names it, then the sound plays. Pressing the button again starts over.
export async function learnSounds() {
  const id = ++run;
  cancel();
  for (const step of STEPS) {
    if (id !== run) return;
    setListener(step.at || ME);
    say(step.name, { priority: 'high', wait: 20000 });
    await sleep(500 + step.name.length * 70 / getRate());
    if (id !== run) return;
    step.go();
    await sleep(step.ms);
  }
  if (id === run) say('That is all the sounds.', { priority: 'high', wait: 20000 });
}
