// Turns the world into sound each frame (ball rattle or tick, footsteps, board brush, goal beacon,
// keeper steps, crowd swell) and plays the sound for each world event.
import { setListener, crowdLevel } from './audio.js';
import { play } from './sfx.js';
import { speakersOn } from './soundmode.js';
import { beaconEvery, BEHIND } from './speakers.js';
import { GOAL, PITCH_W, BOARD_NEAR, THIRD, clamp, dist } from './world.js';

const STEP_EVERY = 0.35, BEACON_EVERY = 2, KEEPER_STEP_EVERY = 0.42;

// Seconds between the ticks of a still ball: 0.8 at 20 m or more, 0.2 within 3 m, linear between, so a
// player closing in hears the ticks speed up (at 4 m/s a fixed 0.8 s would be one tick per 3.2 m).
export const tickEvery = (d) => clamp(0.2 + (d - 3) * 0.6 / 17, 0.2, 0.8);

// Options for a ball sound at `at` heard by a player at playerX. Sounds behind the player (relative
// x < -BEHIND) go through the dull lowpass; makeup restores the level the lowpass takes from the high
// rattle and tick, so behind sounds duller, not quieter. Shared with the Learn the sounds tour.
// The goal chime at `at`, with `every` seconds until the next one: shortened when they come quicker.
// Shared with the tour.
export const chimeOpts = (at, every = BEACON_EVERY) => ({ at, gain: 0.4, k: every / BEACON_EVERY });

export function ballOpts(playerX, at, gain, makeup = 1) {
  const dull = at.x - playerX < -BEHIND;
  return { at, dull, gain: dull ? gain * makeup : gain };
}

// A still ball's tick: heard through the dull filter behind you, so it gets the biggest makeup. The
// HRTF (headphones) takes more of the tick away behind you than the lowpass alone does (speakers), where
// the tick's 950 Hz body and most of its 2.5 kHz tone get through, so there it needs far less.
export const tickOpts = (playerX, at) => ballOpts(playerX, at, 1.2, speakersOn() ? 1.3 : 3);

export function createSoundscape() {
  let step = 0, grain = 0, tick = 0, beacon = 0, keeperStep = 0, hot = null;

  const opts = (w, at, gain, makeup) => ballOpts(w.player.x, at, gain, makeup);

  function ball(w, dt) {
    const b = w.ball, speed = Math.hypot(b.vx, b.vy), every = tickEvery(dist(w.player, b));
    if (speed > 0.3) {
      grain -= dt;
      if (grain <= 0) {
        grain = clamp(0.16 - speed * 0.009, 0.035, 0.16);
        play('rattle', opts(w, b, 0.45 + Math.min(speed / 14, 1) * 0.55, 2));
      }
      tick = every;
    } else {
      tick = Math.min(tick - dt, every); // closing in shortens the wait at once
      if (tick <= 0) { tick = every; play('tick', tickOpts(w.player.x, b)); }
    }
  }

  function feet(w, dt) {
    const p = w.player;
    if (!p.moving) { step = 0.05; return; }
    step -= dt;
    if (step > 0) return;
    step = STEP_EVERY;
    play('step', { gain: 0.22 });
    if (p.y < BOARD_NEAR) play('brush', { at: { x: p.x, y: 0 }, gain: 0.7 });
    if (p.y > PITCH_W - BOARD_NEAR) play('brush', { at: { x: p.x, y: PITCH_W }, gain: 0.7 });
  }

  return {
    reset() { step = 0; grain = 0; tick = 0.3; beacon = 0.6; keeperStep = 0; hot = null; },

    update(w, dt) {
      setListener(w.player);
      ball(w, dt);
      feet(w, dt);
      // Only while you have the ball: then the goal is your target. Soft, so the ball stays the clearest sound.
      // In speakers mode the wait shrinks as you close in on the goal (at once, like the still ball's tick),
      // and the chime is shortened so the quick ones do not run into each other.
      const every = speakersOn() ? beaconEvery(dist(w.player, GOAL)) : BEACON_EVERY;
      beacon = w.hasBall ? Math.min(beacon - dt, every) : Math.min(beacon, 0.3);
      if (beacon <= 0) { beacon = every; play('chime', chimeOpts(GOAL, every)); }
      if (w.keeper && w.keeper.moving) {
        keeperStep -= dt;
        if (keeperStep <= 0) { keeperStep = KEEPER_STEP_EVERY; play('step', { at: w.keeper, gain: 0.7 }); }
      }
      const nowHot = w.ball.x > 2 * THIRD;
      // A gentle swell only: the crowd must never cover the ball's tick. (No bed in level 1.)
      if (nowHot !== hot) { hot = nowHot; crowdLevel(hot ? 0.07 : 0.045, hot ? 1.2 : 2); }
    },

    event(e, w) {
      const force = clamp((e.speed || 0) / 8, 0.25, 1);
      switch (e.type) {
        case 'take':
        case 'tap': play('tap', opts(w, e, 0.7)); break;
        case 'kick': play('kick', opts(w, e, 1)); break;
        case 'bump': play(e.side === 'end' ? 'endwall' : 'board', { at: e, gain: 0.8 }); break;
        case 'board': play('board', opts(w, e, force)); break;
        case 'endwall': play('endwall', opts(w, e, force)); break;
        case 'post': play('clang', { at: e, gain: 1 }); break;
        case 'save': play('save', { at: e, gain: 1 }); break;
        case 'goal':
          play('net', { at: e, gain: 1.2 });
          play('cheer', { gain: 0.7 });
          play('whistle', { gain: 0.3 });
          crowdLevel(0.3, 0.3);
          break;
        default: break;
      }
    },
  };
}
