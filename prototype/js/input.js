// Keyboard state for the game area. Arrows are held; Space, W, H and Escape are one-shot actions.
// preventDefault only on those keys, and only without Ctrl, Alt or Meta, so browser and screen
// reader shortcuts still work.
const ARROWS = new Set(['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight']);

function keyName(e) {
  if (ARROWS.has(e.key)) return e.key;
  if (e.key === ' ' || e.code === 'Space') return 'kick';
  const k = (e.key || '').toLowerCase();
  if (k === 'w') return 'where';
  if (k === 'h') return 'help';
  if (k === 'escape' || k === 'esc') return 'pause';
  return null;
}

export function createInput(el, onAction) {
  const held = new Set();

  el.addEventListener('keydown', (e) => {
    if (e.ctrlKey || e.altKey || e.metaKey) return;
    const k = keyName(e);
    if (!k) return;
    e.preventDefault();
    if (ARROWS.has(k)) held.add(k);
    else if (!e.repeat) onAction(k);
  });

  window.addEventListener('keyup', (e) => {
    const k = keyName(e);
    if (k && ARROWS.has(k)) held.delete(k);
  });

  const releaseAll = () => held.clear();
  window.addEventListener('blur', releaseAll);
  el.addEventListener('blur', releaseAll);
  document.addEventListener('visibilitychange', releaseAll);

  return { held, releaseAll };
}
