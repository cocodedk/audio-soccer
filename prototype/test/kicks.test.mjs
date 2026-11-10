import { test } from 'node:test';
import assert from 'node:assert/strict';
import { kickRecord, settleKick, closeKicks, kickText } from '../js/kicks.js';

const kick = (x, y) => ({ type: 'kick', x, y });

test('a kick records distance to the line, signed offset from the goal centre, and the arrow held', () => {
  assert.deepEqual(kickRecord(kick(22, 12.1), { x: 0, y: 0 }), { dist: 18, offset: 12.1 - 10, aim: 'straight', outcome: null });
  assert.equal(kickRecord(kick(30, 7), { x: 0, y: -1 }).aim, 'left');
  assert.equal(kickRecord(kick(30, 7), { x: 0.7, y: 0.7 }).aim, 'right');
  assert.ok(kickRecord(kick(30, 7), { x: 0, y: 0 }).offset < 0, 'left of centre is negative');
});

test('the first event at the line settles the kick; later ones do not change it', () => {
  for (const [e, want] of [[{ type: 'goal' }, 'goal'], [{ type: 'save' }, 'saved'], [{ type: 'post' }, 'post'],
    [{ type: 'endwall', wide: 'left' }, 'wide left'], [{ type: 'endwall', wide: 'right' }, 'wide right']]) {
    const ks = [kickRecord(kick(30, 10), { x: 0, y: 0 })];
    settleKick(ks, { type: 'board' });
    settleKick(ks, { type: 'endwall', wide: null });
    assert.equal(ks[0].outcome, null, 'boards and a dribbled ball settle nothing');
    settleKick(ks, e);
    settleKick(ks, { type: 'goal' });
    assert.equal(ks[0].outcome, want);
  }
  assert.doesNotThrow(() => settleKick([], { type: 'goal' }));
});

test('a new kick before the line, or the end of the round, means short; a left round is unfinished', () => {
  const ks = [kickRecord(kick(5, 10), { x: 0, y: 0 })];
  settleKick(ks, kick(12, 10));
  assert.equal(ks[0].outcome, 'short');
  const taken = [kickRecord(kick(3, 10), { x: 0, y: 0 })];
  for (const type of ['take', 'goal']) settleKick(taken, { type });
  assert.equal(taken[0].outcome, 'short', 'taken back and dribbled in is not a goal from that kick');
  const rebound = [kickRecord(kick(30, 10), { x: 0, y: 0 })];
  for (const type of ['save', 'take']) settleKick(rebound, { type });
  assert.equal(rebound[0].outcome, 'saved', 'a rebound taken after a save stays saved');
  const open = [kickRecord(kick(5, 10), { x: 0, y: 0 })];
  assert.equal(closeKicks(open, 'giveup')[0].outcome, 'short');
  assert.equal(closeKicks(open, 'left')[0].outcome, 'unfinished');
  assert.equal(open[0].outcome, null, 'closeKicks leaves its input alone');
  assert.deepEqual(closeKicks(undefined, 'success'), []);
});

test('kick text is short and English', () => {
  assert.equal(kickText({ dist: 18.2, offset: 2.08, aim: 'straight', outcome: 'wide right' }), '18 m, 2.1 m right, straight, wide right');
  assert.equal(kickText({ dist: 9, offset: -0.3, aim: 'left', outcome: 'goal' }), '9 m, 0.3 m left, left, goal');
  assert.equal(kickText({ dist: 9, offset: 0.01, aim: 'straight', outcome: 'short' }), '9 m, centre, straight, short');
});
