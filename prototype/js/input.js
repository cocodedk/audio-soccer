// Keyboard state for the game area. Arrows are held; Space, W, H, G, the speed keys and Escape are
// one-shot actions. preventDefault only on those keys, and only without Ctrl, Alt or Meta, so browser
// and screen reader shortcuts still work. The speed keys also work with Alt held: [ and ] are AltGr+8
// and AltGr+9 on Danish and German layouts (Windows reports AltGr as Ctrl+Alt) and Option+key on a Mac,
// and e.key already names the character the layout produced. Ctrl alone never passes (Ctrl+- zooms).
const ARROWS = new Set(['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight']);
// A key that types a non-Latin letter (W gives ص on a Persian layout) is named by its place instead.
const BY_PLACE = { KeyW: 'w', KeyH: 'h', KeyG: 'g', BracketLeft: '[', BracketRight: ']', Minus: '-', Equal: '=' };

export function keyName(e) {
  if (ARROWS.has(e.key)) return e.key;
  if (e.key === ' ' || e.code === 'Space') return 'kick';
  const key = e.key || '';
  const k = (/[^\x00-\x7f]/.test(key) ? BY_PLACE[e.code] || key : key).toLowerCase();
  if (k === 'w') return 'where';
  if (k === 'h') return 'help';
  if (k === 'g') return 'giveup';
  if (k === '[' || k === '-') return 'rateDown';
  if (k === ']' || k === '+' || k === '=') return 'rateUp';
  if (k === 'escape' || k === 'esc') return 'pause';
  return null;
}

const isRate = (k) => k === 'rateUp' || k === 'rateDown';

// Whether the key press k (from keyName) is ours, given the modifiers held.
export function wanted(e, k) {
  if (!k || e.metaKey) return false;
  if (e.ctrlKey || e.altKey) return isRate(k) && !!e.altKey;
  return true;
}

export function createInput(el, onAction) {
  const held = new Set();

  el.addEventListener('keydown', (e) => {
    const k = keyName(e);
    if (!wanted(e, k)) return;
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
