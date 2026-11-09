import { test } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { SIDE_WORDS, playClip, clipsSettled } from '../js/clips.js';
import { LANGS } from '../js/i18n/index.js';
import { sideWord } from '../js/speakers.js';

test('every side word has a clip in English and Persian, under the name clips.js fetches', () => {
  for (const lang of LANGS) {
    for (const word of SIDE_WORDS) {
      assert.ok(existsSync(new URL(`../sounds/side-${word}.${lang}.wav`, import.meta.url)), `${word}.${lang}`);
    }
  }
});

test('the clip words are exactly the words sideWord can return', () => {
  const me = { x: 20, y: 10 };
  const seen = new Set([[10, 0], [0, -5], [0, 5], [-5, 0]].map(([dx, dy]) => sideWord(me, { x: me.x + dx, y: me.y + dy }, false)));
  assert.deepEqual([...seen].sort(), [...SIDE_WORDS].sort());
});

test('with no audio there is no clip and nothing to wait for, so the caller speaks the word', () => {
  assert.equal(clipsSettled(), true);
  assert.equal(playClip('left', 'fa'), false);
});
