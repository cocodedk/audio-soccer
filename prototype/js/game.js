// One play session: level and round flow, the world step, and world events turned into sound,
// speech and stats. main.js owns the DOM; this module owns the game.
import * as W from './world.js';
import { LEVELS, ROUNDS, setupRound, newStats, closeStats, startLines, summarySpeech } from './levels.js';
import { say, cancel, isSaying } from './speech.js';
import { crowdStart, stopAll } from './audio.js';
import { createSoundscape } from './soundscape.js';
import { createCues } from './cues.js';
import { kickRecord, settleKick } from './kicks.js';
import { getLang, t, tl } from './i18n/index.js';

const SUB = 1 / 120;  // physics sub-step, seconds
const BETWEEN = 3;    // pause between rounds, seconds

export function createGame({ onLevelDone = () => {}, rand = Math.random } = {}) {
  const sound = createSoundscape(), cues = createCues();
  let level = 0, round = 0, phase = 'idle', world = null, stats = null, rounds = [], score = 0;
  let wait = 0, resumeTo = 'play', kickQueued = false, clock = false;

  const whereLine = () => t('where.round', { round, total: ROUNDS, text: W.whereText(world, getLang()) });
  // Levels 2 and 3 have the crowd bed; level 1 is quiet so the ball's tick is never masked.
  const crowd = () => { if (level > 1) crowdStart(); };

  function nextRound() {
    round += 1;
    const setup = setupRound(level, rand);
    world = W.createWorld(setup);
    stats = newStats(setup);
    phase = 'play';
    clock = false; // the round clock starts at the first arrow press (or kick)
    kickQueued = false;
    sound.reset();
    cues.reset(world);
    startLines(level, round, getLang()).forEach((line, i) => say(line, i ? { wait: 20000 } : { priority: 'high' }));
  }

  function startLevel(n) {
    stopAll();
    cancel();
    level = n; round = 0; rounds = []; score = 0;
    crowd();
    nextRound();
  }

  // how: 'success', 'giveup' or 'timeout' (see closeStats).
  function endRound(how, line) {
    rounds.push({ ...closeStats(level, stats, how) });
    phase = 'between';
    wait = BETWEEN;
    say(line, { priority: 'high', cut: true });
  }

  function finishLevel() {
    phase = 'idle';
    stopAll();
    say(summarySpeech(level, rounds, false, getLang()), { priority: 'high', wait: 20000 });
    onLevelDone({ level, rounds: rounds.map((r) => ({ ...r })) });
  }

  // A ball hitting the end wall in front of the goal (levels 2 and 3): a shot is counted and called
  // wide; a dribbled one is called too, at a lower rate.
  function endWall(e) {
    if (level < 2 || e.x !== W.PITCH_L) return;
    const side = (k) => t(`dir.${k}`);
    if (e.wide) { stats.wide += 1; say(t('wide', { side: side(e.wide) }), { key: 'wide', gap: 1000 }); }
    else say(t('wideGoal', { side: side(e.y < W.GOAL.y ? 'left' : 'right') }), { key: 'wide', gap: 2500 });
  }

  // dir: the arrows held this step (a kick records whether Left or Right angled it).
  function onEvent(e, dir) {
    sound.event(e, world);
    settleKick(stats.shots, e);
    switch (e.type) {
      case 'take':
        if (LEVELS[level].endsOn === 'take') endRound('success', t('gotIt', { time: stats.time.toFixed(1) }));
        else say(t('gotBall'), { key: 'got', gap: 1500 });
        break;
      case 'kick': stats.kicks += 1; stats.shots.push(kickRecord(e, dir)); break;
      case 'bump': stats.bumps += 1; break;
      case 'goal': score += 1; endRound('success', t('goal')); break;
      case 'save': stats.saves += 1; say(t('saved'), { priority: 'high', key: 'saved', gap: 1500, cut: true }); break;
      case 'post': say(t('post'), { key: 'post', gap: 800 }); break;
      case 'endwall': endWall(e); break;
      case 'voy': say(t('voy'), { pitch: 1.4, cue: true }); break;
      case 'nokick': say(t('noKick'), { priority: 'low', key: 'nokick', gap: 2000 }); break;
      default: break;
    }
  }

  function update(dt, dir) {
    if (phase === 'play') {
      if (!clock && (dir.x || dir.y || kickQueued)) clock = true;
      if (clock) stats.time += dt;
      const n = Math.max(1, Math.ceil(dt / SUB)), h = dt / n;
      const kick = kickQueued;
      kickQueued = false;
      outer: for (let i = 0; i < n; i++) {
        for (const e of W.step(world, h, { dir, kick: kick && i === 0 })) {
          onEvent(e, dir);
          if (phase !== 'play') break outer;
        }
      }
      if (phase === 'play') {
        sound.update(world, dt);
        cues.update(world);
        if (stats.time >= W.ROUND_CAP) endRound('timeout', t('timeUp'));
      }
    } else if (phase === 'between') {
      wait -= dt;
      if (wait <= 0) { if (round < ROUNDS) nextRound(); else finishLevel(); }
    }
  }

  function action(name) {
    if (name === 'help') {
      // Normal priority, so ordinary cues don't wipe it (events still cut in). It waits its turn behind
      // the round's explanation; pressing H again while it is still going starts it over.
      if (isSaying('help')) cancel();
      tl('help').forEach((line) => say(line, { key: 'help', many: true, wait: 30000 }));
      return;
    }
    if (phase !== 'play') return;
    if (name === 'kick') kickQueued = true;
    else if (name === 'where') { stats.where += 1; say(whereLine(), { priority: 'high', key: 'where' }); }
    else if (name === 'giveup') endRound('giveup', t('gaveUp'));
  }

  function pause() {
    if (phase !== 'play' && phase !== 'between') return false;
    resumeTo = phase;
    phase = 'paused';
    stopAll();
    cancel();
    return true;
  }

  function resume() {
    if (phase !== 'paused') return false;
    phase = resumeTo;
    crowd();
    sound.reset();
    say(resumeTo === 'between' ? t('nextRound') : whereLine(), { priority: 'high' });
    return true;
  }

  // The round in play (or paused from play) that the player has started: it is closed as 'left' in a copy,
  // or marked paused for a look at the results during a pause (the round can still go on).
  // A round nobody has touched yet (no key pressed, no W) carries no information and is left out.
  function openRound(paused) {
    const open = phase === 'play' || (phase === 'paused' && resumeTo === 'play');
    return open && stats && (clock || stats.where > 0) ? [{ ...closeStats(level, { ...stats }, 'left'), paused }] : [];
  }

  // The rounds so far of a level that is not over, with the round in progress counted as left (or as
  // paused, when paused is true), or null.
  function partial(paused = false) {
    if (!level || phase === 'idle') return null;
    const all = [...rounds.map((r) => ({ ...r })), ...openRound(paused)];
    return all.length ? { level, rounds: all, partial: true } : null;
  }

  function snapshot() {
    const w = world;
    return {
      level, round, phase, playing: phase === 'play', clockRunning: clock,
      player: w ? { x: w.player.x, y: w.player.y } : null,
      ball: w ? { x: w.ball.x, y: w.ball.y, vx: w.ball.vx, vy: w.ball.vy } : null,
      hasBall: w ? w.hasBall : false,
      keeper: w && w.keeper ? { x: w.keeper.x, y: w.keeper.y } : null,
      score,
      stats: { current: stats ? { ...stats } : null, rounds: rounds.map((r) => ({ ...r })) },
    };
  }

  return {
    startLevel, update, action, pause, resume, partial, snapshot,
    get world() { return world; },
    get phase() { return phase; },
    get level() { return level; },
    get round() { return round; },
    get time() { return stats ? stats.time : 0; },
  };
}
