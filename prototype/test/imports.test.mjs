// CI only runs the pure modules, so a broken import in a DOM module (main.js, menu.js, render.js ...)
// would ship a dead page. This walks every module's static import/export-from lines and checks that each
// relative file exists and exports every name asked of it, plus the files index.html and clips.js load.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, dirname, resolve, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { SIDE_WORDS } from '../js/clips.js';
import { LANGS } from '../js/i18n/index.js';

const PROTO = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const JS = join(PROTO, 'js');

// `a`, `a as b` -> { from: 'a', as: 'b' }
const names = (list) => list.split(',').map((s) => s.trim()).filter(Boolean)
  .map((s) => { const [from, as = from] = s.split(/\s+as\s+/); return { from: from.trim(), as: as.trim() }; });

// What one module asks of others and what it declares itself. Static statements only, at line start.
function parse(src) {
  const deps = []; // { spec, wanted: [names], line }
  const own = new Set(), stars = [];
  const lineOf = (i) => src.slice(0, i).split('\n').length;
  for (const m of src.matchAll(/^\s*import\s+([\w$*{][^'"]*?)\s+from\s+['"]([^'"]+)['"]/gm)) {
    const wanted = [];
    let clause = m[1].trim();
    const braces = clause.match(/\{([^}]*)\}/);
    if (braces) { wanted.push(...names(braces[1]).map((n) => n.from)); clause = clause.replace(braces[0], ''); }
    clause = clause.replace(/\*\s+as\s+[\w$]+/, '');
    if (/^[\w$]+/.test(clause.trim())) wanted.push('default');
    deps.push({ spec: m[2], wanted, line: lineOf(m.index) });
  }
  for (const m of src.matchAll(/^\s*import\s+['"]([^'"]+)['"]/gm)) deps.push({ spec: m[1], wanted: [], line: lineOf(m.index) });
  for (const m of src.matchAll(/^\s*export\s+(\*(?:\s+as\s+([\w$]+))?|\{([^}]*)\})\s+from\s+['"]([^'"]+)['"]/gm)) {
    const spec = m[4], line = lineOf(m.index);
    if (m[3] !== undefined) {
      const list = names(m[3]);
      deps.push({ spec, wanted: list.map((n) => n.from), line });
      list.forEach((n) => own.add(n.as));
    } else if (m[2]) { deps.push({ spec, wanted: [], line }); own.add(m[2]); }
    else { deps.push({ spec, wanted: [], line }); stars.push(spec); }
  }
  for (const m of src.matchAll(/^\s*export\s*\{([^}]*)\}(?!\s*from)/gm)) names(m[1]).forEach((n) => own.add(n.as));
  for (const m of src.matchAll(/^\s*export\s+(?:async\s+)?(?:function\s*\*?|class|const|let|var)\s*([\w$]+)/gm)) own.add(m[1]);
  // `export const A = 1, B = 2;` on one line: drop strings and brackets, then every top-level `NAME =`.
  for (const m of src.matchAll(/^\s*export\s+(?:const|let|var)\s+([^\n]*)/gm)) {
    let rest = m[1].replace(/(['"`])(?:\\.|(?!\1).)*\1/g, '""');
    while (/\([^()]*\)|\[[^[\]]*\]|\{[^{}]*\}/.test(rest)) rest = rest.replace(/\([^()]*\)|\[[^[\]]*\]|\{[^{}]*\}/g, '0');
    for (const d of rest.split(',')) { const n = d.match(/^\s*([\w$]+)\s*=(?!=)/); if (n) own.add(n[1]); }
  }
  if (/^\s*export\s+default\b/m.test(src)) own.add('default');
  return { deps, own, stars };
}

// read(path) -> source text, or null when there is no such file. Returns a list of problems.
function checkModules(paths, read, base = '') {
  const cache = new Map();
  const mod = (p) => { if (!cache.has(p)) { const s = read(p); cache.set(p, s === null ? null : parse(s)); } return cache.get(p); };
  const exportsOf = (p, seen = new Set()) => {
    const m = mod(p);
    if (!m || seen.has(p)) return new Set();
    seen.add(p);
    const all = new Set(m.own);
    for (const spec of m.stars) for (const n of exportsOf(resolve(dirname(p), spec), seen)) if (n !== 'default') all.add(n);
    return all;
  };
  const problems = [];
  for (const p of paths) {
    const m = mod(p);
    const at = (d) => `${relative(base, p) || p}:${d.line}`;
    for (const d of m.deps) {
      if (!d.spec.startsWith('.')) { problems.push(`${at(d)} non-relative import '${d.spec}' (the page has no bundler)`); continue; }
      const target = resolve(dirname(p), d.spec);
      if (!mod(target)) { problems.push(`${at(d)} '${d.spec}' does not exist`); continue; }
      const has = exportsOf(target);
      for (const n of d.wanted) if (!has.has(n)) problems.push(`${at(d)} '${d.spec}' does not export '${n}'`);
    }
  }
  return problems;
}

const walk = (d) => readdirSync(d).flatMap((f) => {
  const q = join(d, f);
  return statSync(q).isDirectory() ? walk(q) : q.endsWith('.js') ? [q] : [];
});
const readOrNull = (p) => { try { return readFileSync(p, 'utf8'); } catch { return null; } };

test('every module under js/ and phone/js/ imports only files that exist and names they export', () => {
  const files = [...walk(JS), ...walk(join(PROTO, 'phone', 'js'))];
  assert.ok(files.length > 20, `found only ${files.length} modules`);
  assert.deepEqual(checkModules(files, readOrNull, PROTO), []);
});

test('index.html loads a script and stylesheet that exist', () => {
  const html = readFileSync(join(PROTO, 'index.html'), 'utf8');
  const refs = [...html.matchAll(/<script\b[^>]*\bsrc="([^"]+)"/g), ...html.matchAll(/<link\b[^>]*rel="stylesheet"[^>]*href="([^"]+)"/g)]
    .map((m) => m[1]).filter((u) => !/^https?:/.test(u));
  assert.ok(refs.includes('js/main.js') && refs.includes('style.css'), refs.join(', '));
  for (const r of refs) assert.ok(existsSync(join(PROTO, r)), `index.html: ${r} missing`);
});

test('every sounds/ file clips.js fetches exists', () => {
  const src = readFileSync(join(JS, 'clips.js'), 'utf8');
  const values = { word: SIDE_WORDS, lang: LANGS };
  const urls = [...src.matchAll(/[`'"](sounds\/[^`'"]+)[`'"]/g)].map((m) => m[1]);
  assert.ok(urls.length > 0, 'clips.js names no sounds/ file');
  // Expand each ${name} over its list; a placeholder we cannot expand fails rather than pass unchecked.
  const expand = (u) => {
    const m = u.match(/\$\{(\w+)\}/);
    if (!m) return [u];
    assert.ok(values[m[1]], `clips.js: no values for \${${m[1]}} in ${u}`);
    return values[m[1]].flatMap((v) => expand(u.replace(m[0], v)));
  };
  for (const f of urls.flatMap(expand)) assert.ok(existsSync(join(PROTO, f)), `${f} missing`);
});

test('the checker reports a missing file, a missing name and a bare specifier, and passes the valid forms', () => {
  const fixture = {
    '/f/a.js': [
      "import { x, v, y, z as zz, w } from './b.js';",
      "import def, * as ns from './d.js';",
      "import { gone } from './nope.js';",
      "import { q } from 'some-package';",
      "export { r as renamed } from './c.js';",
    ].join('\n'),
    '/f/b.js': "export const x = f(1, 2), v = [3, 4];\nexport * from './c.js';\nconst z0 = 2;\nexport { z0 as z };",
    '/f/c.js': 'export function y() {}\nexport async function r() {}\nexport default 3;',
    '/f/d.js': 'export default class D {}',
  };
  const read = (p) => fixture[p] ?? null;
  assert.deepEqual(checkModules(['/f/a.js'], read, '/f'), [
    "a.js:1 './b.js' does not export 'w'",
    "a.js:3 './nope.js' does not exist",
    "a.js:4 non-relative import 'some-package' (the page has no bundler)",
  ]);
  // `export *` does not pass a default on, so asking b.js for one is caught too.
  assert.deepEqual(checkModules(['/f/e.js'], (p) => (p === '/f/e.js' ? "import B from './b.js';" : read(p)), '/f'),
    ["e.js:1 './b.js' does not export 'default'"]);
  // The multi-line import form ball.js uses.
  assert.deepEqual(checkModules(['/f/g.js'], (p) => (p === '/f/g.js' ? "import {\n  x,\n  y,\n} from './b.js';" : read(p)), '/f'), []);
});
