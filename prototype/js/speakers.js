// Pure logic for speakers mode, where nothing is panned: distance is carried by volume and by how fast
// the beacon beeps, and a word (a recorded clip, clips.js) names the side the target is on. No DOM, no audio.
import { goalSide } from './ball.js';
import { clamp } from './world.js';

const REF = 2, ROLLOFF = 0.3, FLOOR = 0.3;

// Gain of a sound at distance d (m): 1 within REF m, then a gentle inverse curve that stops at FLOOR (about
// 18 m). Far sounds stay audible on a laptop speaker at a comfortable volume; the tick and the beacon getting
// faster as you close in carry the rest of the distance. (The headphone panner's curve fell to 0.1 at 20 m.)
export const distanceGain = (d) => Math.max(FLOOR, REF / (REF + ROLLOFF * (Math.max(d, REF) - REF)));

// Seconds between goal chimes while you have the ball: 2 s from 30 m, 0.5 s within 6 m, linear between,
// so closing in on the goal speeds the beeps up (like the still ball's tick).
export const beaconEvery = (d) => clamp(0.5 + (d - 6) * 1.5 / 24, 0.5, 2);

const LATERAL = 1.5; // the ball counts as ahead within this many metres to either side
export const BEHIND = 1;    // ... and as behind when it is more than this far back

// The word for where the target is: 'ahead', 'left', 'right' or 'behind'.
// The ball (you don't have it): behind when more than BEHIND m back, else by the side it is on beyond
// LATERAL m. The goal (you have the ball): judged from the ball, where a kick leaves, by goalSide, the
// same test the goal guide uses: 'ahead' when a straight kick lines up with the goal.
export function sideWord(player, ball, hasBall) {
  if (hasBall) return goalSide(ball.y);
  const ahead = ball.x - player.x, right = ball.y - player.y;
  if (ahead < -BEHIND) return 'behind';
  return Math.abs(right) <= LATERAL ? 'ahead' : right < 0 ? 'left' : 'right';
}

export const SIDE_GAP = 1500; // ms between side calls

// Decides when a side call may be spoken: not within SIDE_GAP of the last one, never the same word
// twice in a row. next(word, t) returns the word to say or null; spoke(word, t) records a call that
// was actually spoken (a call that expired unsaid does not count).
export function createSideCaller() {
  let last = null, at = -Infinity;
  return {
    reset() { last = null; at = -Infinity; },
    next: (word, t) => (word === last || t - at < SIDE_GAP ? null : word),
    spoke(word, t) { last = word; at = t; },
  };
}
