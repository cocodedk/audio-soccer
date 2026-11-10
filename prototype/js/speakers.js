// Pure logic for speakers mode, where nothing is panned: distance is carried by volume and by how fast
// the beacon beeps, and a word (a recorded clip, clips.js) names the side the target is on. No DOM, no audio.
import { goalSide } from './ball.js';
import { clamp } from './world.js';
import { PITCH_W, BOARD_NEAR } from './constants.js';

const REF = 2, ROLLOFF = 0.3, FLOOR = 0.3;

// Gain of a sound at distance d (m): 1 within REF m, then a gentle inverse curve that stops at FLOOR (about
// 18 m). Far sounds stay audible on a laptop speaker at a comfortable volume; the tick and the beacon getting
// faster as you close in carry the rest of the distance. (The headphone panner's curve fell to 0.1 at 20 m.)
export const distanceGain = (d) => Math.max(FLOOR, REF / (REF + ROLLOFF * (Math.max(d, REF) - REF)));

// Seconds between goal chimes while you have the ball: 2 s from 30 m, 0.5 s within 6 m, linear between,
// so closing in on the goal speeds the beeps up (like the still ball's tick).
export const beaconEvery = (d) => clamp(0.5 + (d - 6) * 1.5 / 24, 0.5, 2);

const LATERAL = 1.5;      // once 'ahead' is heard, the ball stays ahead within this many metres to either side,
const NEAR_LATERAL = 0.5; // narrowing to this close by: under DRIBBLE_DIST, so obeying the calls takes it
const ON_LINE = 0.3;      // after any other word, 'ahead' only once you are this close to the ball's line
export const BEHIND = 1;  // the ball counts as behind when more than this far back (less when close)

// Half-width of the 'ahead' band for a ball this many metres in front or behind: LATERAL from 6 m out,
// NEAR_LATERAL within 2 m, linear between. Wide far away so the word does not flap while you run; narrow
// close by, since a ball 1.2 m to the side called 'ahead' is never taken (DRIBBLE_DIST is 0.8 m).
const band = (ahead) => clamp(NEAR_LATERAL + (Math.abs(ahead) - 2) / 4, NEAR_LATERAL, LATERAL);

// The word for where the target is: 'ahead', 'left', 'right' or 'behind'. heard is
// the last word the player heard (createSideCaller's heard()).
// The ball (you don't have it): behind when more than BEHIND m back (the band's width when close), else
// by the side it is on. Hysteresis, because the player keeps going the way the last word said: after a
// side word 'ahead' comes only within ON_LINE of the ball's line, and once heard it holds within the band
// (never under NEAR_LATERAL). Running ahead keeps a still ball's side offset, so it stays under ON_LINE,
// the word does not flip back, and the ball is taken (DRIBBLE_DIST is 0.8 m).
// The goal (you have the ball): judged from the ball, where a kick leaves, by goalSide, the same test the
// goal guide uses: 'ahead' when a straight kick lines up with the goal, at any distance. 'Ahead' is the
// word that stops a sideways run, so it must come beyond SHOT_RANGE too (level 2 and 3 rounds start there);
// whether a kick reaches is W's answer ("too far to shoot").
export function sideWord(player, ball, hasBall, heard = null) {
  if (hasBall) return goalSide(ball.y);
  const ahead = ball.x - player.x, right = ball.y - player.y, half = band(ahead);
  if (ahead < -Math.min(BEHIND, half)) return 'behind';
  return Math.abs(right) <= (heard === 'ahead' ? half : ON_LINE) ? 'ahead' : right < 0 ? 'left' : 'right';
}

// The board the player is within BOARD_NEAR of (the brush zone, which a bump is inside): 'left', 'right' or null.
export const nearBoard = (p) => (p.y < BOARD_NEAR ? 'left' : p.y > PITCH_W - BOARD_NEAR ? 'right' : null);

export const SIDE_GAP = 1500; // ms between side calls

// Decides when a side call may be spoken: not within SIDE_GAP of the last one, never the same word
// twice in a row. next(word, t) returns the word to say or null; spoke(word, t) records a call that
// was actually spoken (a call that expired unsaid does not count); heard() is the last word spoken.
// 'ahead' skips the gap: it is the word to stop stepping sideways, and the player keeps going the way
// the last word said, so 1.5 s late he is 6 m past the line.
export function createSideCaller() {
  let last = null, at = -Infinity;
  return {
    reset() { last = null; at = -Infinity; },
    next: (word, t) => (word === last || (word !== 'ahead' && t - at < SIDE_GAP) ? null : word),
    spoke(word, t) { last = word; at = t; },
    heard: () => last,
  };
}
