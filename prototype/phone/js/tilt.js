// PURE tilt steering: no DOM. Angles are deviceorientation's beta (front-back, degrees, top edge down
// is negative) and gamma (left-right, right edge down is positive). The attitude the phone has when a
// round starts is neutral; tilting further than THRESHOLD from it steers, snapped to the 8 directions.
import { snap8 } from './gestures.js';

export const THRESHOLD = 12; // degrees

// The difference of two angles in degrees, wrapped to -180..180 (beta jumps from 180 to -180).
const diff = (a, b) => ((((a - b) % 360) + 540) % 360) - 180;

export function createTilt(threshold = THRESHOLD) {
  let neutral = null;
  return {
    setNeutral(beta, gamma) { neutral = { beta, gamma }; },
    // Top edge down (beta falls) runs toward the goal; right edge down (gamma rises) runs right.
    dir(beta, gamma) {
      if (!neutral || beta == null || gamma == null) return { x: 0, y: 0 };
      const fwd = -diff(beta, neutral.beta), right = diff(gamma, neutral.gamma);
      return Math.hypot(fwd, right) < threshold ? { x: 0, y: 0 } : snap8(fwd, right);
    },
  };
}
