// Pure text descriptions: clock positions, thirds, where-am-I, the goal guide. No DOM, no audio. The
// words come from the i18n tables: every text function takes a language code, English by default.
import { tr } from './i18n/tables.js';
import { GOAL, THIRD, DRIBBLE_DIST, PITCH_L, KICK_SPEED, TAKE_MAX_SPEED, KICK_ANGLE } from './constants.js';
import { crossingY, aimedIn, goalSide, rollDistance, AIM_LOW, AIM_HIGH } from './ball.js';

// How far a kick reaches the goal line with the speed of a real shot (it slows to a pass below
// TAKE_MAX_SPEED): about 24 m. Further out the goal is too far to shoot at.
export const SHOT_RANGE = rollDistance(KICK_SPEED, TAKE_MAX_SPEED);

// 12 o'clock = toward the goal you attack (+x), 3 = your right (+y), 6 = behind, 9 = your left.
export function clockHour(ahead, right) {
  const h = Math.round(Math.atan2(right, ahead) / (Math.PI / 6));
  const m = ((h % 12) + 12) % 12;
  return m === 0 ? 12 : m;
}

export const metresText = (d, lang = 'en') => tr(lang, 'metres', { n: Math.round(d) });

// A relative offset (metres ahead, metres to the right) as "2 o'clock, 3 metres", or "at your feet"
// (within DRIBBLE_DIST, the distance at which a free ball is taken).
export function clockText(ahead, right, lang = 'en') {
  const d = Math.hypot(ahead, right);
  if (d < DRIBBLE_DIST) return tr(lang, 'clock.feet');
  return tr(lang, 'clock.at', { hour: clockHour(ahead, right), metres: metresText(d, lang) });
}

export function clockTo(from, to, lang = 'en') {
  return clockText(to.x - from.x, to.y - from.y, lang);
}

export function thirdIndex(x) {
  return x < THIRD ? 0 : x < 2 * THIRD ? 1 : 2;
}

export const thirdName = (i, lang = 'en') => tr(lang, `thirds.${i}`);

export function sideText(y, lang = 'en') {
  return tr(lang, `side.${y < 3 ? 'left' : y > 17 ? 'right' : 'centre'}`);
}

// "Middle third, by the left board. Ball 2 o'clock, 3 metres. Goal, 23 metres, slightly right."
// The goal part is guideText: it names the direction in words, which is what lining up needs.
export function whereText(world, lang = 'en') {
  const p = world.player;
  const from = world.hasBall ? world.ball : p; // a kick leaves from the ball, which can sit 2 m to one side
  const parts = [
    tr(lang, 'where.place', { third: thirdName(thirdIndex(p.x), lang), side: sideText(p.y, lang) }),
    tr(lang, 'where.ball', { pos: clockTo(p, world.ball, lang) }),
    tr(lang, 'where.guide', { text: guideText(p, from, lang) }),
  ];
  if (world.keeper) parts.push(tr(lang, 'where.keeper', { pos: clockTo(p, world.keeper, lang) }));
  return parts.join(' ');
}

// What the goal part of W says, judged by the kick itself: "straight ahead" only when a straight kick
// would score (with aim help), "slightly left/right" only when the 15 degree kick toward the goal
// would, otherwise the side and how many metres to move. Beyond SHOT_RANGE: too far to shoot.
//   "Goal, 9 metres, straight ahead" / "Goal, 23 metres, slightly right" /
//   "Goal, 10 metres, move 6 metres right" / "Goal, 30 metres, too far to shoot"
// p is where the player stands (the distance they hear); from is where a kick would leave (the ball).
export function guideText(p, from = p, lang = 'en') {
  const m = metresText(Math.hypot(PITCH_L - p.x, GOAL.y - p.y), lang), head = tr(lang, 'guide.head', { m });
  if (PITCH_L - from.x > SHOT_RANGE) return tr(lang, 'guide.far', { head });
  const way = goalSide(from.y);
  if (way === 'ahead') return tr(lang, 'guide.ahead', { head });
  const left = way === 'left', side = tr(lang, `dir.${way}`), angle = left ? -KICK_ANGLE : KICK_ANGLE;
  if (aimedIn(crossingY(from.x, from.y, angle))) return tr(lang, 'guide.slightly', { head, side });
  const move = from.y > AIM_HIGH ? from.y - AIM_HIGH : AIM_LOW - from.y; // to the nearest line a straight kick would score from
  return tr(lang, 'guide.move', { head, m: metresText(Math.max(1, move), lang), side });
}
