// PURE: the self-spoken menu's three options and the cursor over them. No DOM, no speech. Like
// TalkBack, the cursor stops at the ends: a swipe past the last option says that option again.
export const OPTIONS = ['drag', 'tilt', 'finish'];

export function createMenu() {
  let i = 0;
  return {
    get current() { return OPTIONS[i]; },
    // Swipe right (+1) or left (-1). Returns the option now under the cursor.
    move(step) {
      i = Math.max(0, Math.min(OPTIONS.length - 1, i + step));
      return OPTIONS[i];
    },
    // A gesture name from the menu gestures: what the menu does with it.
    act(gesture) {
      if (gesture === 'right' || gesture === 'left') return { say: this.move(gesture === 'right' ? 1 : -1) };
      if (gesture === 'double') return { choose: OPTIONS[i] };
      if (gesture === 'tap') return { say: OPTIONS[i] };
      return {};
    },
  };
}
