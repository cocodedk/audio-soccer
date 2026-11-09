// Top-down canvas picture of the pitch for sighted helpers and low-vision players. It shows only
// what the sound already says: the pitch, the player, the ball and the keeper. labels() gives the words
// drawn on it ({ left, right, attack, you, keeper }) in the current language.
const TURF = '#1E5A47', TURF2 = '#22654F', CHALK = '#F1F5EF', BOARD = '#A9C2CF', AMBER = '#F5B436';
const HOME = '#F4F7F2', NAVY = '#16233A';
const FRAME = { x0: -3.5, x1: 43.5, y0: -3, y1: 23 };

export function createRenderer(cv, labels) {
  const c = cv.getContext('2d');
  let s = 0, last = null;
  const X = (x) => (x - FRAME.x0) * s;
  const Y = (y) => (y - FRAME.y0) * s;
  const font = (w, size) => `${w} ${Math.max(10, size * s)}px "Atkinson Hyperlegible", "Segoe UI", Tahoma, system-ui, sans-serif`;
  const circle = (x, y, r) => { c.beginPath(); c.arc(X(x), Y(y), r * s, 0, Math.PI * 2); };

  function fit() {
    const r = cv.getBoundingClientRect();
    if (!r.width) return;
    const dpr = window.devicePixelRatio || 1;
    cv.width = Math.round(r.width * dpr);
    cv.height = Math.round(r.height * dpr);
    s = cv.width / (FRAME.x1 - FRAME.x0);
    draw(last);
  }

  function pitch() {
    c.direction = 'ltr'; // not the page's (resizing resets it): a Persian label is one right-to-left run either way
    c.fillStyle = TURF; c.fillRect(0, 0, cv.width, cv.height);
    c.fillStyle = TURF2;
    for (let i = 1; i < 10; i += 2) c.fillRect(X(i * 4), Y(0), 4 * s, 20 * s);
    c.strokeStyle = CHALK; c.lineWidth = Math.max(1.5, 0.12 * s);
    c.strokeRect(X(0), Y(0), 40 * s, 20 * s);
    c.beginPath(); c.moveTo(X(20), Y(0)); c.lineTo(X(20), Y(20)); c.stroke();
    circle(20, 10, 3); c.stroke();
    c.beginPath(); c.arc(X(40), Y(10), 5 * s, Math.PI / 2, Math.PI * 1.5); c.stroke();
    c.setLineDash([0.4 * s, 0.4 * s]);
    [40 / 3, 80 / 3].forEach((x) => { c.beginPath(); c.moveTo(X(x), Y(0)); c.lineTo(X(x), Y(20)); c.stroke(); });
    c.setLineDash([]);
    c.strokeRect(X(40), Y(8.5), 1.2 * s, 3 * s);
    c.fillStyle = BOARD;
    c.fillRect(X(0), Y(-0.55), 40 * s, 0.55 * s);
    c.fillRect(X(0), Y(20), 40 * s, 0.55 * s);
    c.fillStyle = CHALK; c.font = font(700, 0.95); c.textBaseline = 'middle';
    c.textAlign = 'left';
    c.fillText(labels().left, X(0.2), Y(-1.75));
    c.fillText(labels().right, X(0.2), Y(21.75));
    c.textAlign = 'right';
    c.fillText(labels().attack, X(39.8), Y(-1.75));
  }

  function person(x, y, fill, stroke, label) {
    circle(x, y, 0.75); c.fillStyle = fill; c.fill();
    c.lineWidth = Math.max(1.5, 0.14 * s); c.strokeStyle = stroke; c.stroke();
    if (label) {
      c.fillStyle = stroke; c.font = font(700, 0.6); c.textAlign = 'center';
      c.fillText(label, X(x), Y(y) + 0.03 * s);
    }
  }

  function draw(w) {
    last = w;
    if (!s || cv.hidden) return;
    pitch();
    if (!w) return;
    if (w.keeper) person(w.keeper.x, w.keeper.y, NAVY, HOME, labels().keeper);
    const p = w.player;
    person(p.x, p.y, HOME, NAVY, null);
    circle(p.x, p.y, 1.25); c.strokeStyle = AMBER; c.lineWidth = Math.max(2, 0.24 * s); c.stroke();
    c.fillStyle = AMBER; c.font = font(700, 0.9); c.textAlign = 'center';
    c.fillText(labels().you, X(p.x), Y(p.y < 3 ? p.y + 2.2 : p.y - 2));
    circle(w.ball.x, w.ball.y, 0.42); c.fillStyle = AMBER; c.fill();
    c.lineWidth = Math.max(1, 0.1 * s); c.strokeStyle = '#3A2600'; c.stroke();
  }

  new ResizeObserver(fit).observe(cv);
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(() => draw(last));
  return { draw, fit };
}
