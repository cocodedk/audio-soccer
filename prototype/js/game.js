// One play session: level and round flow, the world step, and world events turned into sound,
// speech and stats. main.js owns the DOM; this module owns the game.
import * as W from './world.js';
import { LEVELS, ROUNDS, setupRound, newStats, startLine, summarySpeech } from './levels.js';
import { say, cancel } from './speech.js';
import { crowdStart, stopAll } from './audio.js';
import { createSoundscape } from './soundscape.js';

const SUB = 1 / 120;  // physics sub-step, seconds
const BETWEEN = 3;    // pause between rounds, seconds

export const HELP = 'Arrow up runs toward the goal, arrow down away from it. Left and right arrows go to your left and right. '
  + 'Hold two arrows to run diagonally. Space kicks the ball up the pitch. In the attacking third it shoots at the goal, and holding left or right aims inside that post.'
  + 'W tells you where you are. H repeats this help. Escape pauses, and Space resumes after a pause.';

export function createGame({ onLevelDone }) {
  const sound = createSoundscape();
  let level = 0, round = 0, phase = 'idle', world = null, stats = null, rounds = [], score = 0;
  let wait = 0, resumeTo = 'play', third = 0, ballWasAhead = true, kickQueued = false;

  function nextRound() {
    round += 1;
    world = W.createWorld(setupRound(level));
    stats = newStats();
    phase = 'play';
    third = W.thirdIndex(world.player.x);
    ballWasAhead = world.ball.x >= world.player.x;
    kickQueued = false;
    sound.reset();
    say(startLine(level, round), { priority: 'high' });
  }

  function startLevel(n) {
    stopAll();
    cancel();
    level = n; round = 0; rounds = []; score = 0;
    crowdStart();
    nextRound();
  }

  function endRound(line) {
    rounds.push({ ...stats });
    phase = 'between';
    wait = BETWEEN;
    say(line, { priority: 'high' });
  }

  function finishLevel() {
    phase = 'idle';
    stopAll();
    say(summarySpeech(level, rounds), { priority: 'high' });
    onLevelDone({ level, rounds: rounds.map((r) => ({ ...r })) });
  }

  function onEvent(e) {
    sound.event(e, world);
    switch (e.type) {
      case 'take':
        if (LEVELS[level].endsOn === 'take') endRound(`Got it! ${stats.time.toFixed(1)} seconds.`);
        else say('Got the ball', { key: 'got', gap: 1500 });
        break;
      case 'kick': stats.kicks += 1; break;
      case 'bump': stats.bumps += 1; break;
      case 'goal': score += 1; endRound('Goal!'); break;
      case 'save': say('Saved!', { priority: 'high' }); break;
      case 'post': say('Post!'); break;
      case 'endwall': if (e.wide) say(`Wide ${e.wide}`); break;
      case 'voy': say('Voy!', { pitch: 1.4, rate: 1.3 }); break;
      case 'nokick':
        say('You need the ball to kick', { priority: 'low', key: 'nokick', gap: 2000 });
        break;
      default: break;
    }
  }

  // Spoken cues that come from watching the world: thirds, the ball passing behind, the goal guide.
  function cues() {
    const p = world.player, b = world.ball;
    const up = W.thirdIndex(p.x - 0.5), down = W.thirdIndex(p.x + 0.5); // 0.5 m hysteresis
    if (up > third || down < third) {
      third = up > third ? up : down;
      say(W.THIRD_NAMES[third]);
    }
    const ahead = b.x - p.x;
    if (ballWasAhead && ahead < -1 && !world.hasBall) say('Ball behind you', { key: 'behind', gap: 4000 });
    if (ahead > 0) ballWasAhead = true;
    else if (ahead < -1) ballWasAhead = false;
    if (world.hasBall && W.thirdIndex(p.x) === 2) say(W.guideText(p), { key: 'guide', gap: 4000 });
  }

  function update(dt, dir) {
    if (phase === 'play') {
      stats.time += dt;
      const n = Math.max(1, Math.ceil(dt / SUB)), h = dt / n;
      const kick = kickQueued;
      kickQueued = false;
      outer: for (let i = 0; i < n; i++) {
        for (const e of W.step(world, h, { dir, kick: kick && i === 0 })) {
          onEvent(e);
          if (phase !== 'play') break outer;
        }
      }
      if (phase === 'play') { sound.update(world, dt); cues(); }
    } else if (phase === 'between') {
      wait -= dt;
      if (wait <= 0) { if (round < ROUNDS) nextRound(); else finishLevel(); }
    }
  }

  function action(name) {
    if (name === 'help') { say(HELP); return; }
    if (phase === 'play' && name === 'kick') kickQueued = true;
    if (phase === 'play' && name === 'where') { stats.where += 1; say(W.whereText(world)); }
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
    crowdStart();
    sound.reset();
    say(`Round ${round}. ${W.whereText(world)}`, { priority: 'high' });
    return true;
  }

  function snapshot() {
    const w = world;
    return {
      level, round, phase, playing: phase === 'play',
      player: w ? { x: w.player.x, y: w.player.y } : null,
      ball: w ? { x: w.ball.x, y: w.ball.y, vx: w.ball.vx, vy: w.ball.vy } : null,
      hasBall: w ? w.hasBall : false,
      keeper: w && w.keeper ? { x: w.keeper.x, y: w.keeper.y } : null,
      score,
      stats: { current: stats ? { ...stats } : null, rounds: rounds.map((r) => ({ ...r })) },
    };
  }

  return {
    startLevel, update, action, pause, resume, snapshot,
    get world() { return world; },
    get phase() { return phase; },
    get level() { return level; },
    get round() { return round; },
    get time() { return stats ? stats.time : 0; },
  };
}
