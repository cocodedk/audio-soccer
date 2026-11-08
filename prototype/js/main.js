// Wiring: menu, focus, headphone check, results, the game loop and the automation hook.
import { initSpeech, setMode, say } from './speech.js';
import { ensureAudio } from './audio.js';
import { play } from './sfx.js';
import { createInput } from './input.js';
import { createGame } from './game.js';
import { createRenderer } from './render.js';
import { LEVELS, resultsText, summarySpeech } from './levels.js';
import { arrowsToDir } from './world.js';

// The page has no <html lang> (Artifact contract), so screen readers would read English in their own voice.
if (!document.documentElement.lang) document.documentElement.lang = 'en';

const $ = (s) => document.querySelector(s);
const arena = $('#arena'), statusEl = $('#status'), resumeBtn = $('#resume');
const resultsBox = $('#results'), summaryEl = $('#summary'), resultsText_ = $('#results-text');
const copyStatus = $('#copy-status'), canvas = $('#pitch');
const finished = []; // levels finished this session, for the results block

initSpeech($('#live-polite'), $('#live-assertive'));
const modeNow = () => document.querySelector('input[name="voice-mode"]:checked')?.value || 'voice';
setMode(modeNow());
document.querySelectorAll('input[name="voice-mode"]').forEach((r) => r.addEventListener('change', () => setMode(modeNow())));

const renderer = createRenderer(canvas);
const game = createGame({ onLevelDone });
const input = createInput(arena, onAction);

// Focus moves before the game speaks, so a screen reader's focus announcement doesn't cut the line off.
function startLevel(n) {
  ensureAudio();
  setMode(modeNow());
  resumeBtn.hidden = true;
  arena.focus();
  game.startLevel(n);
}

function resumeGame() {
  if (game.phase !== 'paused') return;
  ensureAudio();
  resumeBtn.hidden = true;
  arena.focus();
  game.resume();
}

// Escape always reaches the menu: it pauses a running game, and from a paused or finished one it
// still moves focus there (blur and a hidden tab pause without moving focus).
function pauseToMenu(moveFocus) {
  if (game.pause()) {
    input.releaseAll();
    resumeBtn.textContent = `Resume level ${game.level}, round ${game.round}`;
    resumeBtn.hidden = false;
    say('Paused. Space resumes.', { priority: 'high' });
  }
  if (moveFocus) (resumeBtn.hidden ? $('[data-level]') : resumeBtn).focus();
}

function onAction(name) {
  if (name === 'pause') pauseToMenu(true);
  else if (name === 'kick' && game.phase === 'paused') resumeGame();
  else game.action(name);
}

function onLevelDone({ level, rounds }) {
  finished.push({ level, rounds });
  summaryEl.textContent = summarySpeech(level, rounds);
  resultsText_.value = resultsText(finished);
  copyStatus.textContent = '';
  resultsBox.hidden = false;
  resumeBtn.hidden = true;
  $('#h-results').focus();
}

document.querySelectorAll('[data-level]').forEach((b) => b.addEventListener('click', () => startLevel(Number(b.dataset.level))));

resumeBtn.addEventListener('click', resumeGame);

// A sound hard left, then hard right, each with its word spoken.
$('#hp-check').addEventListener('click', () => {
  if (!ensureAudio()) { say('This browser cannot play sound.', { priority: 'high' }); return; }
  setMode(modeNow());
  const side = (pan, word) => {
    [0, 250, 500].forEach((ms) => setTimeout(() => play('kick', { pan, gain: 0.9 }), ms));
    say(word, { priority: 'high' });
  };
  side(-1, 'Left');
  setTimeout(() => side(1, 'Right'), 1500);
});

$('#hide-pic').addEventListener('change', (e) => { canvas.hidden = e.target.checked; });

$('#copy').addEventListener('click', () => {
  const text = resultsText_.value;
  const selectIt = () => {
    const keys = /Mac|iPhone|iPad/.test(navigator.platform || '') ? 'Command C' : 'Control C';
    copyStatus.textContent = `Copying was blocked. The text is selected: press ${keys} to copy it.`;
    resultsText_.focus();
    resultsText_.select();
  };
  try {
    navigator.clipboard.writeText(text).then(() => { copyStatus.textContent = 'Copied.'; }, selectIt);
  } catch (e) {
    selectIt();
  }
});

// Leaving the game area, the tab or the window pauses: keys can't reach the game, so nothing plays on.
arena.addEventListener('blur', () => pauseToMenu(false));
document.addEventListener('visibilitychange', () => { if (document.hidden) pauseToMenu(false); });
// Coming back to a paused game (window switch, Tab): say how to go on. Checked a tick later, so the
// focus moves made by startLevel and resumeGame, which leave the pause right after, stay quiet.
arena.addEventListener('focus', () => setTimeout(() => {
  if (game.phase === 'paused' && document.activeElement === arena) {
    say('Paused. Press Space to resume, or Escape for the menu.', { priority: 'high' });
  }
}, 0));

function statusText() {
  const ph = game.phase;
  if (ph === 'idle') return game.level ? `Level ${game.level} done` : 'Choose a level to start';
  if (ph === 'paused') return 'Paused';
  return `Level ${game.level}, ${LEVELS[game.level].name} · Round ${game.round} of 3 · ${game.time.toFixed(1)} s`;
}

let last = performance.now(), shown = '';
function frame(now) {
  const dt = Math.min(0.05, Math.max(0, (now - last) / 1000));
  last = now;
  game.update(dt, arrowsToDir(input.held));
  renderer.draw(game.world);
  const text = statusText();
  if (text !== shown) { statusEl.textContent = text; shown = text; }
  requestAnimationFrame(frame);
}
requestAnimationFrame(frame);

window.__audioSoccer = {
  state: () => game.snapshot(),
  startLevel: (n) => startLevel(Number(n)),
};
