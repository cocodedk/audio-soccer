// PURE touch gestures for the phone test: no DOM. The page feeds pointer events in as
// down/move/up/cancel(id, x, y, t) with x, y in CSS pixels (y grows down the screen) and t in ms, and
// calls tick(t) every frame so a finger held still can time out.

export const DEAD = 24;       // px: a finger within this of where it landed has not moved (a tap, no steer)
export const TAP_MS = 300;    // a finger lifted within this, without moving, is a tap
export const CHORD_MS = 150;  // fingers landing this close together belong to one tap (two fingers, three)
export const SWIPE = 60;      // px: a one-finger stroke at least this long is a swipe
export const DOUBLE_MS = 350; // a second tap landing within this of the first one's lift is a double-tap

const NONE = { x: 0, y: 0 };
const D = Math.SQRT1_2;
// The 8 directions the keyboard gives, counter-clockwise from straight up the pitch, in pitch
// coordinates (x toward the goal, y to the player's right), with exact zeros so a straight run kicks
// straight (world.kickDir angles a kick by the sign of y).
const DIRS = [[1, 0], [D, D], [0, 1], [-D, D], [-1, 0], [-D, -D], [0, -1], [D, -D]].map(([x, y]) => ({ x, y }));

// A pitch-space vector snapped to the nearest of the 8 directions; none for a zero vector.
export function snap8(x, y) {
  if (!x && !y) return NONE;
  const i = Math.round(Math.atan2(y, x) / (Math.PI / 4));
  return DIRS[((i % 8) + 8) % 8];
}

// A finger's offset from where it landed (screen px) as a steering direction: up the screen runs
// toward the goal, right runs to the player's right. Inside the dead zone it is no steer at all.
export function dragDir(dx, dy, dead = DEAD) {
  return Math.hypot(dx, dy) < dead ? NONE : snap8(-dy, dx);
}

// Play: one finger steers by dragging; taps kick. onTap(n) gets the number of fingers that tapped
// together: 1 kick, 2 where, 3 or more back to the menu. A finger joins the tap of the fingers that
// landed within CHORD_MS before it; the tap is reported once all of them are up, so two fingers
// together are one "where", never two kicks. The steering finger never counts as a tap.
export function createPlayGestures({ onTap = () => {} } = {}) {
  const fingers = new Map(); // id -> { x0, y0, x, y, t0, chord }
  let steerId = null, lastChord = null;

  // A finger is no longer a tap candidate (it moved, was held, lifted late or was cancelled).
  function drop(f, tapped) {
    const c = f.chord;
    if (!c) return;
    f.chord = null;
    c.waiting -= 1;
    if (tapped) c.taps += 1;
    if (c.waiting === 0 && c.taps > 0) onTap(c.taps);
  }

  function expire(t) {
    for (const f of fingers.values()) if (f.chord && t - f.t0 > TAP_MS) drop(f, false);
  }

  return {
    down(id, x, y, t) {
      expire(t);
      const chord = lastChord && lastChord.waiting > 0 && t - lastChord.t0 <= CHORD_MS
        ? lastChord : (lastChord = { t0: t, waiting: 0, taps: 0 });
      chord.waiting += 1;
      fingers.set(id, { x0: x, y0: y, x, y, t0: t, chord });
    },
    move(id, x, y, t) {
      const f = fingers.get(id);
      if (!f) return;
      f.x = x; f.y = y;
      if (Math.hypot(x - f.x0, y - f.y0) >= DEAD) {
        drop(f, false);
        if (steerId === null) steerId = id;
      }
      expire(t);
    },
    up(id, x, y, t) {
      const f = fingers.get(id);
      if (!f) return;
      fingers.delete(id);
      if (steerId === id) steerId = null;
      drop(f, t - f.t0 <= TAP_MS && Math.hypot(x - f.x0, y - f.y0) < DEAD);
      expire(t);
    },
    cancel(id) {
      const f = fingers.get(id);
      if (!f) return;
      fingers.delete(id);
      if (steerId === id) steerId = null;
      drop(f, false);
    },
    tick: expire,
    // The steering direction now: the steering finger's drag from where it landed, or none.
    steer() {
      const f = steerId === null ? null : fingers.get(steerId);
      return f ? dragDir(f.x - f.x0, f.y - f.y0) : NONE;
    },
    reset() { fingers.clear(); steerId = null; lastChord = null; },
  };
}

// Menu and hand-over: TalkBack's own habits. A one-finger stroke of SWIPE px or more is a swipe
// ('left', 'right', 'up', 'down' by its main axis); two quick taps are a double-tap; one tap on its
// own, once the double-tap window has passed, is a tap. A stroke during which another finger touched
// is ignored.
export function createMenuGestures({ onSwipe = () => {}, onDoubleTap = () => {}, onTap = () => {} } = {}) {
  const fingers = new Map(); // id -> { x0, y0, t0, multi }
  let lastTapUp = null; // when the last lone tap was lifted, while a second one may still come

  function tick(t) {
    if (lastTapUp !== null && fingers.size === 0 && t - lastTapUp > DOUBLE_MS) { lastTapUp = null; onTap(); }
  }

  return {
    down(id, x, y, t) {
      tick(t); // a tap whose window has passed is a tap, even if no frame came in between
      const multi = fingers.size > 0;
      for (const f of fingers.values()) f.multi = true;
      fingers.set(id, { x0: x, y0: y, t0: t, multi });
    },
    move() {},
    up(id, x, y, t) {
      const f = fingers.get(id);
      if (!f) return;
      fingers.delete(id);
      if (f.multi) { lastTapUp = null; return; }
      const dx = x - f.x0, dy = y - f.y0, len = Math.hypot(dx, dy);
      if (len >= SWIPE) {
        lastTapUp = null;
        onSwipe(Math.abs(dx) >= Math.abs(dy) ? (dx > 0 ? 'right' : 'left') : (dy < 0 ? 'up' : 'down'));
      } else if (len < DEAD && t - f.t0 <= TAP_MS) {
        if (lastTapUp !== null && f.t0 - lastTapUp <= DOUBLE_MS) { lastTapUp = null; onDoubleTap(); }
        else lastTapUp = t;
      }
    },
    cancel(id) { fingers.delete(id); lastTapUp = null; },
    tick,
    reset() { fingers.clear(); lastTapUp = null; },
  };
}
