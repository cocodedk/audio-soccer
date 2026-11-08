// Spoken cues that come from watching the world: thirds, the ball passing behind, the goal guide.
// All of them are ambient (cue: true), so they are spoken by the built-in voice only; a screen
// reader player gets events and W/H answers, and asks for the rest.
import { say } from './speech.js';
import { thirdIndex, THIRD_NAMES, guideText } from './world.js';

export function createCues() {
  let third = 0, ballWasAhead = true;
  return {
    reset(w) {
      third = thirdIndex(w.player.x);
      ballWasAhead = w.ball.x >= w.player.x;
    },
    update(w) {
      const p = w.player, b = w.ball;
      const up = thirdIndex(p.x - 0.5), down = thirdIndex(p.x + 0.5); // 0.5 m hysteresis
      if (up > third || down < third) {
        third = up > third ? up : down;
        say(THIRD_NAMES[third], { cue: true });
      }
      const ahead = b.x - p.x;
      if (ballWasAhead && ahead < -1 && !w.hasBall) say('Ball behind you', { key: 'behind', gap: 4000, cue: true });
      if (ahead > 0) ballWasAhead = true;
      else if (ahead < -1) ballWasAhead = false;
      if (w.hasBall && thirdIndex(p.x) === 2) say(guideText(p, w.ball), { key: 'guide', gap: 6000, cue: true });
    },
  };
}
