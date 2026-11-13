// One steering scheme: the trial's level 2 (take the ball to the goal and score), three rounds, steered
// by dragging or by tilting, with taps to kick and the two vibration cues. The scheme's intro is spoken
// first; the rounds and their lines, sounds and stats are the trial's own (game.js).
import { createGame } from '../../js/game.js';
import { startLines } from '../../js/levels.js';
import { say, cancel, isSaying } from '../../js/speech.js';
import { tr, trList } from '../../js/i18n/tables.js';
import { getLang } from '../../js/i18n/index.js';
import { createPlayGestures } from './gestures.js';
import { createTilt } from './tilt.js';
import { createBuzz } from './buzz.js';
import { LEVEL } from './report.js';
import { vibrate } from './device.js';

export const TILT_WAIT = 2500; // ms to wait for the first tilt reading before giving up on tilt

// What a round says as it starts: round 1's tip in the phone's words (the trial's names the Space key).
export const roundLines = (level, round, lang) =>
  (round === 1 ? [startLines(level, round, lang)[0], tr(lang, 'phone.tip')] : startLines(level, round, lang));

// How the scheme works, said before its first round.
export const introLines = (scheme, lang) => [...trList(lang, `phone.intro.${scheme}`), ...trList(lang, 'phone.intro.common')];

// scheme: 'drag' or 'tilt'. onEnd(block, why): block is { scheme, rounds, partial } to record, or null
// when no round was played; why is 'done' (three rounds), 'back' (three-finger tap) or 'notilt'.
// onTilt() is called at the first tilt reading.
export function createRun(scheme, { onEnd, onTilt = () => {} }) {
  let phase = 'idle', world = null, waitUntil = Infinity, att = { beta: null, gamma: null };
  const game = createGame({ lines: roundLines, onLevelDone: ({ rounds }) => end({ scheme, rounds, partial: false }, 'done') });
  const buzz = createBuzz(), tilt = createTilt();
  const gestures = createPlayGestures({
    onTap(n) {
      if (n >= 3) back();
      else if (phase === 'play') game.action(n === 2 ? 'where' : 'kick');
    },
  });

  function end(block, why) {
    if (phase === 'over') return;
    phase = 'over';
    window.removeEventListener('deviceorientation', onOrient);
    onEnd(block, why);
  }

  function back() {
    const left = game.partial();
    game.pause(); // stops the sounds
    cancel();
    end(left ? { scheme, rounds: left.rounds, partial: true } : null, 'back');
  }

  function intro() {
    phase = 'intro';
    introLines(scheme, getLang()).forEach((line) => say(line, { priority: 'high', key: 'intro', many: true, wait: 60000 }));
  }

  function onOrient(e) {
    if (e.beta == null || e.gamma == null) return; // a browser with no sensor sends nulls
    att = { beta: e.beta, gamma: e.gamma };
    if (phase === 'tilt') { onTilt(); intro(); }
  }

  // Called from the tap that chose the scheme: iOS asks for motion permission only from a gesture.
  function start(now) {
    if (scheme !== 'tilt') { intro(); return; }
    phase = 'tilt';
    window.addEventListener('deviceorientation', onOrient);
    const ask = window.DeviceOrientationEvent?.requestPermission;
    const timeFrom = () => { waitUntil = performance.now() + TILT_WAIT; };
    if (typeof ask === 'function') ask.call(window.DeviceOrientationEvent).then(timeFrom, timeFrom);
    else waitUntil = now + TILT_WAIT;
  }

  // Each frame, after the surface has ticked the gestures.
  function update(dt, now) {
    if (phase === 'tilt' && now > waitUntil) { end(null, 'notilt'); return; }
    if (phase === 'intro' && !isSaying('intro')) { phase = 'play'; game.startLevel(LEVEL); }
    if (phase !== 'play') return;
    if (game.world !== world) { // a new round: the phone's attitude now is level
      world = game.world;
      buzz.reset(world);
      tilt.setNeutral(att.beta, att.gamma);
    }
    game.update(dt, scheme === 'tilt' ? tilt.dir(att.beta, att.gamma) : gestures.steer());
    if (phase === 'play' && game.phase === 'play') {
      const pattern = buzz.update(game.world);
      if (pattern) vibrate(pattern);
    }
  }

  return {
    gestures, start, update,
    // The page was hidden and shown again (screen off, another app).
    pause() { if (phase === 'play') game.pause(); },
    resume() { if (phase === 'play') game.resume(); },
    snapshot: () => ({ phase, game: game.snapshot() }),
    // What the screen shows each frame (view.js): cheap, no copies.
    status: () => ({ phase, round: game.round, world: game.world }),
  };
}
