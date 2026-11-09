// Spoken cues that come from watching the world: thirds, the ball passing behind, the goal guide, and
// in speakers mode the side calls. All but the side calls are ambient (cue: true), so they are spoken
// by the built-in voice only; a screen reader player gets events and W/H answers, and asks for the rest.
import { say, isSaying } from './speech.js';
import { thirdIndex, thirdName, guideText } from './world.js';
import { speakersOn } from './soundmode.js';
import { sideWord, createSideCaller, BEHIND } from './speakers.js';
import { playClip, clipsSettled } from './clips.js';
import { getLang, t } from './i18n/index.js';

export function createCues() {
  let third = 0, ballWasAhead = true, queued = null;
  const caller = createSideCaller();

  // Speakers mode: one short word when the target's side changes. The target is the ball, or the goal once
  // you have it. The word is a recorded clip (clips.js): it plays at once, with or without a screen
  // reader, and is never late. Only while a clip is missing is the word spoken instead, at low priority
  // with a short wait; a spoken word that expired unsaid is offered again.
  function sideCall(w) {
    if (!speakersOn()) return;
    const word = caller.next(sideWord(w.player, w.ball, w.hasBall), performance.now());
    if (!word) return;
    if (playClip(word, getLang())) { caller.spoke(word, performance.now()); return; }
    if (!clipsSettled() || (word === queued && isSaying('side'))) return;
    queued = word;
    say(t(`call.${word}`), {
      priority: 'low', key: 'side', wait: 800, onSpoken: () => caller.spoke(word, performance.now()),
    });
  }

  return {
    reset(w) {
      third = thirdIndex(w.player.x);
      ballWasAhead = w.ball.x >= w.player.x;
      caller.reset();
      queued = null;
    },
    update(w) {
      const p = w.player, b = w.ball;
      const up = thirdIndex(p.x - 0.5), down = thirdIndex(p.x + 0.5); // 0.5 m hysteresis
      if (up > third || down < third) {
        third = up > third ? up : down;
        say(thirdName(third, getLang()), { cue: true });
      }
      const ahead = b.x - p.x;
      if (ballWasAhead && ahead < -BEHIND && !w.hasBall) {
        // Once it is said, the side call 'behind' would only repeat it.
        say(t('behind'), { key: 'behind', gap: 4000, cue: true, onSpoken: () => caller.spoke('behind', performance.now()) });
      }
      if (ahead > 0) ballWasAhead = true;
      else if (ahead < -BEHIND) ballWasAhead = false;
      if (w.hasBall && thirdIndex(p.x) === 2) say(guideText(p, w.ball, getLang()), { key: 'guide', gap: 6000, cue: true });
      sideCall(w);
    },
  };
}
