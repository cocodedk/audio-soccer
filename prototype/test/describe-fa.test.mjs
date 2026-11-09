import { test } from 'node:test';
import assert from 'node:assert/strict';
import { clockText, clockTo, guideText, whereText, sideText, thirdName, metresText, createWorld, SHOT_RANGE } from '../js/world.js';

test('Persian clock positions and metres use Persian digits', () => {
  assert.equal(clockText(0, 3, 'fa'), 'ساعت ۳، ۳ متر');
  assert.equal(clockText(5, 0, 'fa'), 'ساعت ۱۲، ۵ متر');
  assert.equal(clockText(-4, 0, 'fa'), 'ساعت ۶، ۴ متر');
  assert.equal(clockText(0, -7, 'fa'), 'ساعت ۹، ۷ متر');
  assert.equal(clockText(2.6, 1.5, 'fa'), 'ساعت ۱، ۳ متر');
  assert.equal(clockText(0.5, 0, 'fa'), 'جلوی پایتان');
  assert.equal(clockTo({ x: 10, y: 10 }, { x: 10, y: 12 }, 'fa'), 'ساعت ۳، ۲ متر');
  assert.equal(metresText(12.4, 'fa'), '۱۲ متر');
  assert.equal(metresText(12.4), '12 metres', 'English by default');
});

test('Persian thirds and sides', () => {
  assert.deepEqual([0, 1, 2].map((i) => thirdName(i, 'fa')), ['یک‌سوم دفاعی', 'یک‌سوم میانی', 'یک‌سوم حمله']);
  assert.equal(sideText(1, 'fa'), 'کنار دیواره چپ');
  assert.equal(sideText(19, 'fa'), 'کنار دیواره راست');
  assert.equal(sideText(10, 'fa'), 'وسط');
  assert.equal(thirdName(2), 'Attacking third');
});

test('Persian guide: far, straight ahead, slightly, and metres to move', () => {
  assert.equal(guideText({ x: 10, y: 10 }, undefined, 'fa'), 'دروازه، ۳۰ متر، برای شوت زدن خیلی دور است');
  assert.equal(guideText({ x: 31, y: 10 }, undefined, 'fa'), 'دروازه، ۹ متر، درست جلو');
  assert.equal(guideText({ x: 30, y: 5 }, undefined, 'fa'), 'دروازه، ۱۱ متر، کمی به راست');
  assert.equal(guideText({ x: 30, y: 15 }, undefined, 'fa'), 'دروازه، ۱۱ متر، کمی به چپ');
  assert.equal(guideText({ x: 34, y: 2 }, undefined, 'fa'), 'دروازه، ۱۰ متر، ۶ متر به راست بروید');
  assert.equal(guideText({ x: 34, y: 18.5 }, undefined, 'fa'), 'دروازه، ۱۰ متر، ۶ متر به چپ بروید');
  assert.match(guideText({ x: 40 - SHOT_RANGE - 0.1, y: 10 }, undefined, 'fa'), /خیلی دور است$/);
});

test('the Persian guide says what the English one does: same branch for every spot', () => {
  const branch = (text, words) => words.findIndex((w) => w.test(text));
  const en = [/too far/, /straight ahead/, /slightly/, /move/], fa = [/خیلی دور/, /درست جلو/, /کمی/, /بروید/];
  for (const x of [10, 20, 25, 30, 35]) {
    for (let y = 1; y <= 19; y += 0.5) {
      assert.equal(branch(guideText({ x, y }, undefined, 'fa'), fa), branch(guideText({ x, y }), en), `${x},${y}`);
    }
  }
});

test('Persian where: place, ball, goal and keeper in one go', () => {
  const w = createWorld({ player: { x: 20, y: 1 }, ball: { x: 20, y: 4 }, hasBall: false, keeper: true });
  assert.equal(
    whereText(w, 'fa'),
    'یک‌سوم میانی، کنار دیواره چپ. توپ ساعت ۳، ۳ متر. دروازه، ۲۲ متر، ۷ متر به راست بروید. دروازه‌بان ساعت ۱، ۲۲ متر.',
  );
  assert.match(whereText(w), /^Middle third, by the left board\. Ball 3 o'clock, 3 metres\. Goal, 22 metres, move 7 metres right\. Keeper 1 o'clock, 22 metres\.$/);
  const mine = createWorld({ player: { x: 34, y: 10 }, ball: { x: 34.5, y: 10 }, hasBall: true });
  assert.equal(whereText(mine, 'fa'), 'یک‌سوم حمله، وسط. توپ جلوی پایتان. دروازه، ۶ متر، درست جلو.');
});
