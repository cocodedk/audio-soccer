// The trial page's accessibility floor, pinned: skip link, language legend, link to the Android app page,
// the type scale, the 44px option rows, non-text contrast of the light theme, and the pitch labels
// (size and contrast). The thresholds are the ones in the brief; the browser pass is in the review notes.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = (p) => readFileSync(new URL(p, import.meta.url), 'utf8');
const HTML = read('../index.html'), PHONE = read('../phone/index.html'), CSS = read('../style.css');

const lum = (hex) => {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
const ratio = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + 0.05) / (y + 0.05); };
const token = (name) => CSS.match(new RegExp(`--${name}:\\s*(#[0-9A-Fa-f]{6})`))[1]; // the first one: the light theme

test('the skip link is the first thing on the page and lands on the level buttons', () => {
  const body = HTML.slice(HTML.indexOf('<link rel="stylesheet"'));
  assert.match(body.replace(/^[^\n]*\n\s*/, ''), /^<a class="skip" href="#h-levels" data-i18n="ui.skip">/);
  assert.match(HTML, /<h3 id="h-levels" tabindex="-1"/);
  assert.match(CSS, /\.skip:focus \{ transform: none \}/);
});

test('the language legend gives each word its own language, on both pages', () => {
  for (const page of [HTML, PHONE]) {
    assert.match(page, /<legend><span lang="en" dir="ltr">Language<\/span> \/ <span lang="fa" dir="rtl">زبان<\/span><\/legend>/);
  }
});

test('the Android app link follows the phone link, and its path follows the language', async () => {
  assert.match(HTML, /class="phone-link">[^\n]*\n\s*<p class="app-link"><a href="app\/" data-app-link data-i18n="ui.appLink">/);
  const store = {};
  globalThis.localStorage = { getItem: (k) => (k in store ? store[k] : null), setItem: (k, v) => { store[k] = v; } };
  globalThis.document = { documentElement: {}, title: '' };
  const { setLang, APP_PATH } = await import('../js/i18n/index.js');
  const { applyLanguage } = await import('../js/i18n/dom.js');
  assert.deepEqual(APP_PATH, { en: 'app/', fa: 'fa/app/' });
  const link = { attrs: {}, setAttribute(k, v) { this.attrs[k] = v; } };
  const root = { querySelectorAll: (s) => (s === '[data-app-link]' ? [link] : []) };
  applyLanguage(root);
  assert.equal(link.attrs.href, 'app/');
  setLang('fa');
  applyLanguage(root);
  assert.equal(link.attrs.href, 'fa/app/');
});

test('the phone page has a description and a visible link back to the trial (the base is the trial folder)', () => {
  const desc = PHONE.match(/<meta name="description" content="([^"]+)">/)?.[1] ?? '';
  assert.ok(desc.length >= 50 && desc.length <= 160, `description is ${desc.length} characters`);
  assert.match(PHONE, /<base href="\.\.\/">/);
  assert.match(PHONE, /<a href="\.\/" data-i18n="phone\.ui\.back">/);
});

test('the type scale follows the browser default, and the small caps text is at least .875rem', () => {
  assert.match(CSS, /\nbody \{[^}]*font-size: 1\.0625rem/);
  assert.doesNotMatch(CSS, /(^|[\s;{])font-size:\s*\d+(\.\d+)?px/, 'a px font size does not follow the default');
  for (const sel of ['\\.eyebrow', 'kbd']) {
    const size = CSS.match(new RegExp(`${sel} \\{[^}]*font-size: ([\\d.]+)rem`))[1];
    assert.ok(Number(size) >= 0.875, `${sel} is ${size}rem`);
  }
});

test('option rows are 44px tall, and the checked radio and checkbox are 3:1 on the light surface and page', () => {
  assert.match(CSS, /\.check \{[^}]*min-height: 44px/);
  assert.match(CSS, /\.check input \{[^}]*accent-color: var\(--accent\)[;\s]/);
  for (const ground of ['surface', 'bg']) assert.ok(ratio(token('accent'), token(ground)) >= 3, `accent on ${ground}`);
});

// The pitch picture on a fake canvas: every label's font size in canvas pixels, and its colour.
function drawn({ width, dpr, labels }) {
  const seen = [];
  const ctx = new Proxy({ measureText: (t) => ({ width: t.length * 14 * dpr }), fillText(text) { seen.push({ text, font: this.font, fill: this.fillStyle }); } },
    { get: (o, k) => (k in o ? o[k] : () => {}), set: (o, k, v) => { o[k] = v; return true; } });
  globalThis.window = { devicePixelRatio: dpr };
  globalThis.ResizeObserver = class { constructor(fn) { this.fn = fn; } observe() { this.fn(); } };
  const cv = { getContext: () => ctx, width: 0, height: 0, hidden: false, getBoundingClientRect: () => ({ width, height: width * 26 / 47 }) };
  return import('../js/render.js').then(({ createRenderer }) => {
    createRenderer(cv, () => labels).draw({ player: { x: 8, y: 10 }, ball: { x: 9, y: 10 }, keeper: { x: 38, y: 10 } });
    return seen;
  });
}
const L = { left: 'YOUR LEFT · BOARDS', right: 'YOUR RIGHT · BOARDS', attack: 'ATTACKING →', you: 'YOU', keeper: 'GK' };

test('pitch labels are at least 14 CSS px, whatever the pixel ratio and the width', async () => {
  for (const dpr of [1, 2, 3]) for (const width of [240, 700]) {
    const labels = await drawn({ width, dpr, labels: L });
    assert.ok(labels.length >= 5, `${labels.length} labels`);
    for (const l of labels) assert.ok(Number(l.font.match(/([\d.]+)px/)[1]) >= 14 * dpr, `${l.text} at dpr ${dpr}, ${width}px: ${l.font}`);
  }
});

test('pitch labels are at least 4.5:1 on the turf and its stripes, and a narrow picture keeps only the attack arrow', async () => {
  const wide = await drawn({ width: 900, dpr: 1, labels: L });
  for (const l of wide) {
    assert.ok(ratio(l.fill.toUpperCase(), '#1E5A47') >= 4.5, `${l.text}: ${l.fill} on the turf`);
    if (l.text === 'YOU') assert.ok(ratio(l.fill.toUpperCase(), '#22654F') >= 4.5, 'YOU on the stripe');
  }
  assert.ok(wide.some((l) => l.text === L.attack));
  const narrow = await drawn({ width: 240, dpr: 1, labels: L });
  assert.ok(!narrow.some((l) => l.text === L.attack) && narrow.some((l) => l.text === '→'), 'the arrow stands in for the label');
});
