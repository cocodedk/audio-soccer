// The start-screen sound tour and the headphone check. Both go through the same path as play
// (play() with an `at` point, the listener placed with setListener), so what you learn is what you hear.
// The tour skips what needs panning in speakers mode and teaches the side calls and the quickening
// beeps instead.
import { setListener } from './audio.js';
import { play } from './sfx.js';
import { ballOpts, tickOpts, chimeOpts } from './soundscape.js';
import { say, cancel } from './speech.js';
import { getRate } from './voices.js';
import { speakersOn } from './soundmode.js';
import { beaconEvery } from './speakers.js';
import { loadClips, playClip, SIDE_WORDS } from './clips.js';
import { getLang, t } from './i18n/index.js';
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
  side(ME.y - 3, t('learn.left'));
  later(() => side(ME.y + 3, t('learn.right')), 1500);
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

// Where the listener stands for the quickening chimes, and where they play (so only the rate changes).
const NEAR_GOAL = { x: 34, y: 10 };

// The goal chime from 30, 18 and 6 m: two seconds apart, then one second, then half a second.
const chimes = () => {
  let at = 0;
  [30, 18, 6, 6, 6].forEach((d) => {
    const every = beaconEvery(d);
    later(() => play('chime', chimeOpts(NEAR_GOAL, every)), at);
    at += every * 1000;
  });
};

// The four side-call clips, as the game plays them: ahead, left, right, behind.
const sideClips = () => loadClips()?.then(() => SIDE_WORDS.forEach((word, i) => later(() => playClip(word, getLang()), i * 800)));

// name: what the voice says first. at: where the listener stands. go: the sound. ms: how long it lasts.
// pan: needs left and right, so only headphones play it.
const steps = () => [
  // The clips say the four words themselves, so the name stays short: the first sound is not held back.
  { name: t('learn.calls'), go: sideClips, ms: SIDE_WORDS.length * 800, speakers: true },
  { name: t('learn.target'), ms: 0, speakers: true }, // spoken after the clips, no sound of its own
  { name: t('learn.ahead'), go: () => ticks(ahead), ms: 2000 },
  { name: t('learn.behind'), go: () => ticks(behind), ms: 2000 },
  { name: t('learn.roll'), go: rolling, ms: 1700, pan: true },
  { name: t('learn.chime'), go: () => play('chime', chimeOpts(GOAL)), ms: 1500, pan: true },
  { name: t('learn.chimeNear'), at: NEAR_GOAL, go: chimes, ms: 4500, speakers: true },
  { name: t(speakersOn() ? 'learn.board' : 'learn.boardL'), at: { x: ME.x, y: 3 }, go: knock(0), ms: 900 },
  { name: t('learn.boardR'), at: { x: ME.x, y: 17 }, go: knock(20), ms: 900, pan: true },
  { name: t('learn.feet'), go: feet, ms: 1500 },
  { name: t('learn.voy'), go: () => say(t('voy'), { priority: 'high', pitch: 1.4 }), ms: 1200 },
].filter((x) => (speakersOn() ? !x.pan : !x.speakers));

// How long to let a step's name be heard before its sound. The built-in voice's speed is known, so it
// gets a wait that scales with its rate. In screen-reader mode the player's own reader speaks the name,
// after a 100 ms live-region delay and at a speed we cannot know, and slower readers are common, so the
// wait assumes a slow reader instead: 1 s plus 100 ms per character, whatever the built-in rate.
export const nameWait = (name, sr, rate) => (sr ? 1000 + name.length * 100 : 500 + name.length * 70 / rate);
// The voice mode radio, as language.js modeNow() reads it. Importing language.js here would loop back
// through menu.js to this file and reach the DOM on load.
const srMode = () => globalThis.document?.querySelector('input[name="voice-mode"]:checked')?.value === 'sr';

export function stopLearn() {
  run += 1;
  timers.forEach(clearTimeout);
  timers.clear();
}

// Each step: the voice names it, then the sound plays. Pressing the button again starts over.
export async function learnSounds() {
  const id = ++run;
  cancel();
  for (const step of steps()) {
    if (id !== run) return;
    setListener(step.at || ME);
    say(step.name, { priority: 'high', wait: 20000 });
    await sleep(nameWait(step.name, srMode(), getRate()));
    if (id !== run) return;
    step.go?.();
    await sleep(step.ms);
  }
  if (id === run) say(t('learn.end'), { priority: 'high', wait: 20000 });
}
