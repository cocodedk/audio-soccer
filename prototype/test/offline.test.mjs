import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';

// The trial page must not fetch from any other host: a hung host stalls the page for the tester,
// and every request hands the player's IP to a third party (DESIGN.md: no tracking).
const read = (p) => readFileSync(new URL(p, import.meta.url), 'utf8');
const HTML = read('../index.html');
const CSS = read('../style.css');

test('index.html loads nothing from another host', () => {
  for (const m of HTML.matchAll(/<(link|script|img|audio|source|iframe)\b[^>]*>/gi)) {
    assert.doesNotMatch(m[0], /(src|href)\s*=\s*["']?(https?:)?\/\//i, m[0]);
  }
});

test('style.css loads nothing from another host, and every font file it names exists', () => {
  assert.doesNotMatch(CSS, /@import/);
  const urls = [...CSS.matchAll(/url\(\s*["']?([^"')]+)["']?\s*\)/g)].map((m) => m[1]);
  assert.ok(urls.length >= 5, `only ${urls.length} font urls`);
  for (const u of urls) {
    assert.doesNotMatch(u, /^(https?:)?\/\//, u);
    assert.ok(existsSync(new URL(`../${u}`, import.meta.url)), `${u} missing`);
  }
  for (const face of CSS.match(/@font-face\s*{[^}]*}/g)) assert.match(face, /font-display:\s*swap/, face);
});

test('the Pages workflow copies the fonts next to the page', () => {
  assert.match(read('../../.github/workflows/deploy-pages.yml'), /cp -r [^\n]*prototype\/fonts[^\n]*_site\//);
});
