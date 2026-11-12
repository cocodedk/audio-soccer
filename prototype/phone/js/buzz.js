// PURE vibration cues: no DOM. Watches the world each frame and returns a navigator.vibrate pattern
// when something changes, never while it stays the same: one short buzz when the ball comes into reach
// (the player takes it), a double buzz when the player with the ball becomes lined up with the goal
// (a straight kick would go in: the same test as the spoken side call 'ahead').
import { aimedIn, AIM_LOW, AIM_HIGH } from '../../js/ball.js';

export const REACH = [40];
export const LINED = [40, 80, 40];
const SLACK = 0.3; // m: once lined up, the ball must drift this far outside the band to count as off it

export function createBuzz() {
  let had = true, lined = true; // a round starts with the ball: no buzz until something changes
  return {
    reset(w) { had = w.hasBall; lined = had && aimedIn(w.ball.y); },
    update(w) {
      const y = w.ball.y;
      const nowLined = w.hasBall && (lined ? y >= AIM_LOW - SLACK && y <= AIM_HIGH + SLACK : aimedIn(y));
      const took = w.hasBall && !had;
      const pattern = nowLined && !lined ? LINED : took ? REACH : null;
      had = w.hasBall;
      lined = nowLined;
      return pattern;
    },
  };
}
