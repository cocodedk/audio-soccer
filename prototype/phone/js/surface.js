// The touch surface: the whole screen, once the screen reader is off. Pointer events go to whichever
// gesture recognizer is in use (gestures.js); the browser's own gestures are stopped: no scrolling,
// zooming, pull to refresh, text selection or long-press menu.
const now = () => performance.now();

export function createSurface(el) {
  let target = null;
  const send = (name) => (e) => {
    e.preventDefault();
    target?.[name](e.pointerId, e.clientX, e.clientY, now());
  };
  el.addEventListener('pointerdown', send('down'));
  el.addEventListener('pointermove', send('move'));
  el.addEventListener('pointerup', send('up'));
  el.addEventListener('pointercancel', (e) => target?.cancel(e.pointerId, now()));
  // Non-passive, so preventDefault works: browsers that ignore touch-action still don't scroll or zoom.
  for (const type of ['touchstart', 'touchmove', 'touchend']) el.addEventListener(type, (e) => e.preventDefault(), { passive: false });
  el.addEventListener('contextmenu', (e) => e.preventDefault());

  return {
    // Hand the pointer events to another recognizer, starting it clean.
    use(recognizer) {
      target?.reset();
      target = recognizer;
      target?.reset();
    },
    tick(t) { target?.tick(t); },
  };
}
