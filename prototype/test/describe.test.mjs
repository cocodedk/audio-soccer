import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  guideText, whereText, arrowsToDir, createWorld, step, SHOT_RANGE, rollDistance, KICK_SPEED, TAKE_MAX_SPEED, KICK_ANGLE, crossingY, aimedIn,
} from '../js/world.js';
import { close, run, types } from './helpers.mjs';

const kickFrom = (x, y) => {
  const w = createWorld({ player: { x: x - 0.5, y }, ball: { x, y }, hasBall: true });
  step(w, 1 / 120, { kick: true });
  return types(run(w, 6)).includes('goal');
};

test('shot range is derived from the ball physics: about 24 m, less than a full kick', () => {
  close(SHOT_RANGE, rollDistance(KICK_SPEED, TAKE_MAX_SPEED));
  assert.ok(SHOT_RANGE > 23.5 && SHOT_RANGE < 25, `${SHOT_RANGE}`);
  assert.ok(rollDistance(KICK_SPEED) > SHOT_RANGE && rollDistance(KICK_SPEED) < 27);
  assert.equal(kickFrom(40 - SHOT_RANGE + 0.5, 10), true, 'a kick from inside the range reaches the goal');
  assert.equal(kickFrom(40 - 27.5, 10), false, 'a kick from beyond its full range stops short');
});

test('guide: too far to shoot beyond the shot range', () => {
  assert.equal(guideText({ x: 10, y: 10 }), 'Goal, 30 metres, too far to shoot');
  assert.match(guideText({ x: 40 - SHOT_RANGE - 0.1, y: 10 }), /too far to shoot$/);
  assert.match(guideText({ x: 40 - SHOT_RANGE + 0.1, y: 10 }), /straight ahead$/);
});

test('guide: straight ahead exactly when a straight kick scores with aim help (y 7.5 to 12.5)', () => {
  for (const y of [7.5, 8, 10, 12, 12.5]) assert.match(guideText({ x: 31, y }), /straight ahead$/, `y ${y}`);
  for (const y of [7.4, 12.6, 3, 17]) assert.doesNotMatch(guideText({ x: 31, y }), /straight ahead/, `y ${y}`);
});

test('guide: slightly left or right exactly when the matching 15 degree kick scores; otherwise metres to move', () => {
  // 10 m out a 15 degree kick moves the line 2.68 m, so from y 4.82 upward the right kick reaches the window
  assert.match(guideText({ x: 30, y: 5 }), /slightly right$/);
  assert.match(guideText({ x: 30, y: 7.4 }), /slightly right$/);
  assert.match(guideText({ x: 30, y: 4.7 }), /move 3 metres right$/);
  assert.match(guideText({ x: 30, y: 15 }), /slightly left$/);
  assert.match(guideText({ x: 30, y: 15.3 }), /move 3 metres left$/);
  assert.equal(guideText({ x: 34, y: 2 }), 'Goal, 10 metres, move 6 metres right');
  assert.equal(guideText({ x: 34, y: 18.5 }), 'Goal, 10 metres, move 6 metres left');
});

test('guide agrees with the kick: what it promises is what Space does', () => {
  for (const x of [20, 25, 30, 35]) {
    for (let y = 1; y <= 19; y += 0.5) {
      const text = guideText({ x, y }), side = y > 10 ? -1 : 1;
      const straight = aimedIn(crossingY(x, y, 0)), angled = aimedIn(crossingY(x, y, side * KICK_ANGLE));
      if (/straight ahead/.test(text)) assert.ok(straight, `${x},${y}`);
      else if (/slightly/.test(text)) assert.ok(!straight && angled, `${x},${y}`);
      else assert.ok(!straight && !angled, `${x},${y} ${text}`);
    }
  }
  for (const y of [7.5, 8.2, 12.4]) assert.equal(kickFrom(30, y), true, `a straight kick scores from y ${y}`);
});

// W after a sideways dribble: the ball runs about 1 m ahead of the player, to the side, so the advice
// has to be judged from the ball (where the kick leaves), not from the player.
test('W after a sideways dribble: kicking as told does what it promised', () => {
  let differed = 0;
  for (const [y0, key] of [[3, 'ArrowRight'], [17, 'ArrowLeft']]) {
    for (let secs = 0.5; secs <= 2.5; secs += 0.25) {
      const w = createWorld({ player: { x: 30, y: y0 }, ball: { x: 30, y: y0 }, hasBall: true });
      run(w, secs, { dir: arrowsToDir([key]) });
      run(w, 0.2); // stop; the ball rolls on a little and stays yours
      assert.ok(w.hasBall, `${key} ${secs}s still has the ball`);
      const said = whereText(w).match(/Goal, [^.]*/)[0];
      if (said !== guideText(w.player)) differed++;
      const kickWith = (arrows) => {
        const k = structuredClone(w);
        step(k, 1 / 120, { kick: true, dir: arrowsToDir(arrows) });
        return types(run(k, 4)).includes('goal');
      };
      const msg = `${key} ${secs}s, ball y ${w.ball.y.toFixed(2)}: ${said}`;
      if (/straight ahead/.test(said)) assert.ok(kickWith([]), msg);
      else if (/slightly right/.test(said)) assert.ok(kickWith(['ArrowRight']) && !kickWith([]), msg);
      else if (/slightly left/.test(said)) assert.ok(kickWith(['ArrowLeft']) && !kickWith([]), msg);
      else assert.ok(!kickWith([]) && !kickWith(['ArrowRight']) && !kickWith(['ArrowLeft']), msg);
    }
  }
  assert.ok(differed > 0, 'some stops leave the ball where the player-based advice would be wrong');
});
