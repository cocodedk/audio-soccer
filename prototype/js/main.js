// Wiring: focus, pausing, results, the game loop and the automation hook. The start-screen controls
// are in menu.js and the results block in results.js.
import { initSpeech, setMode, say } from './speech.js';
import { ensureAudio } from './audio.js';
import { createInput } from './input.js';
import { createGame } from './game.js';
import { createRenderer } from './render.js';
import { createResults } from './results.js';
import { initMenu, fillVoices, showRate } from './menu.js';
import { stopLearn } from './learn.js';
import { getRate, setRate, stepRate } from './voices.js';
import { LEVELS, ROUNDS } from './levels.js';
import { arrowsToDir } from './world.js';

// The page has no <html lang> (Artifact contract), so screen readers would read English in their own voice.
if (!document.documentElement.lang) document.documentElement.lang = 'en';

const $ = (s) => document.querySelector(s);
const arena = $('#arena'), statusEl = $('#status'), resumeBtn = $('#resume'), canvas = $('#pitch');

const modeNow = () => document.querySelector('input[name="voice-mode"]:checked')?.value || 'voice';
const applyMode = () => setMode(modeNow());
initSpeech($('#live-polite'), $('#live-assertive'), fillVoices);
applyMode();
document.querySelectorAll('input[name="voice-mode"]').forEach((r) => r.addEventListener('change', applyMode));
initMenu({ applyMode });

const renderer = createRenderer(canvas);
const results = createResults({
  box: $('#results'), summary: $('#summary'), text: $('#results-text'), copyStatus: $('#copy-status'),
  heading: $('#h-results'), copyBtn: $('#copy'),
}, () => ({ mode: modeNow(), rate: getRate(), hidden: canvas.hidden }));
const game = createGame({ onLevelDone });
const input = createInput(arena, onAction);

// Focus moves before the game speaks, so a screen reader's focus announcement doesn't cut the line off.
function startLevel(n) {
  const left = game.partial(); // a level left half-played still counts in the results
  if (left) results.add(left);
  stopLearn();
  ensureAudio();
  applyMode();
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
    results.show(game.partial(true)); // kept levels, plus this one's rounds so far (the open one as paused)
  }
  if (moveFocus) (resumeBtn.hidden ? $('[data-level]') : resumeBtn).focus();
}

function changeSpeed(dir) {
  const r = setRate(stepRate(getRate(), dir));
  showRate();
  say(`Speed ${r.toFixed(1)}`, { priority: 'high', key: 'rate' });
}

function onAction(name) {
  if (name === 'pause') pauseToMenu(true);
  else if (name === 'kick' && game.phase === 'paused') resumeGame();
  else if (name === 'rateUp' || name === 'rateDown') changeSpeed(name === 'rateUp' ? 1 : -1);
  else game.action(name);
}

function onLevelDone({ level, rounds }) {
  results.add({ level, rounds });
  results.show(null, true);
  resumeBtn.hidden = true;
}

document.querySelectorAll('[data-level]').forEach((b) => b.addEventListener('click', () => startLevel(Number(b.dataset.level))));
resumeBtn.addEventListener('click', resumeGame);
$('#hide-pic').addEventListener('change', (e) => { canvas.hidden = e.target.checked; });

// Leaving the game area, the tab or the window pauses: keys can't reach the game, so nothing plays on.
arena.addEventListener('blur', () => pauseToMenu(false));
document.addEventListener('visibilitychange', () => { if (document.hidden) pauseToMenu(false); });
// Coming back to a paused game (window switch, Tab): say how to go on. Checked a tick later, so the
// focus moves made by startLevel and resumeGame, which leave the pause right after, stay quiet.
arena.addEventListener('focus', () => setTimeout(() => {
  if (game.phase === 'paused' && document.activeElement === arena) {
    say('Paused. Space resumes, Escape for the menu.', { priority: 'high' });
  }
}, 0));

function statusText() {
  const ph = game.phase;
  if (ph === 'idle') return game.level ? `Level ${game.level} done` : 'Choose a level to start';
  if (ph === 'paused') return 'Paused';
  return `Level ${game.level}, ${LEVELS[game.level].name} · Round ${game.round} of ${ROUNDS} · ${game.time.toFixed(1)} s`;
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
