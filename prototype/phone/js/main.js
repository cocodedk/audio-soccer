// The phone controls test, start to finish: the start screen (screen reader on), the voice check, the
// hand-over (screen reader off), the self-spoken menu, the steering schemes (play.js), and the results
// (screen reader back on). Each Start press is one session in the results.
import { initSpeech, say, cancel } from '../../js/speech.js';
import { ensureAudio } from '../../js/audio.js';
import { loadClips } from '../../js/clips.js';
import { setSoundMode } from '../../js/soundmode.js';
import { chooseVoices, getRate } from '../../js/voices.js';
import { getLang, setLang, onLang, t } from '../../js/i18n/index.js';
import { applyLanguage } from '../../js/i18n/dom.js';
import { isoMinute } from '../../js/report.js';
import { newSession } from './report.js';
import { voiceCandidates, voiceFacts, describeVoice } from './voicepick.js';
import { loadVoices, proveVoice, useVoice } from './voicecheck.js';
import { enterFullscreen, lockPortrait, keepAwake, leaveFullscreen, canVibrate } from './device.js';
import { createMenuGestures } from './gestures.js';
import { createMenu } from './menu.js';
import { createSurface } from './surface.js';
import { createRun } from './play.js';
import { createResults } from './results.js';
import { createView } from './view.js';

const $ = (s) => document.querySelector(s);
const page = $('#page'), notice = $('#notice'), surfaceEl = $('#surface');
const HANDOVER_EVERY = 15000; // ms between repeats of the hand-over line until the swipe comes
const END_CAP = 8000;         // ms the voice check line may hold up the next one

const results = createResults({
  box: $('#results'), text: $('#results-text'), copyBtn: $('#copy'), copyStatus: $('#copy-status'), heading: $('#h-results'),
});
const surface = createSurface(surfaceEl), view = createView(surfaceEl);
let phase = 'start', session = null, menu = null, run = null, repeat = null;

function showLanguage() {
  applyLanguage();
  document.title = t('phone.ui.title');
  document.querySelectorAll('input[name="lang"]').forEach((r) => { r.checked = r.value === getLang(); });
}
onLang(() => { chooseVoices(); showLanguage(); });
document.querySelectorAll('input[name="lang"]').forEach((r) => r.addEventListener('change', () => { if (r.checked) setLang(r.value); }));
showLanguage();
initSpeech($('#live-polite'), $('#live-assertive'));
results.show();

// Hands-free: the touch surface covers the screen and the page is hidden; at the end the page comes back.
function handsFree(on) {
  document.documentElement.classList.toggle('hands-free', on);
  surfaceEl.hidden = !on;
  page.hidden = on;
}

// A message on the page, set a moment after it is shown so the screen reader announces it.
function tell(key) {
  notice.hidden = false;
  notice.textContent = '';
  setTimeout(() => { notice.textContent = t(key); }, 50);
}

$('#start').addEventListener('click', async () => {
  if (phase !== 'start') return;
  phase = 'check';
  notice.hidden = true;
  menu = null;
  const fs = enterFullscreen(); // first, while the click still counts as the player's gesture
  ensureAudio();
  loadClips();
  setSoundMode('speakers');
  session = results.add(newSession(isoMinute(), navigator.userAgent || '', getLang()));
  session.vibration = canVibrate();
  session.fullscreen = await fs;
  session.orientation = await lockPortrait();
  session.wakeLock = await keepAwake();
  const list = await loadVoices();
  Object.assign(session, voiceFacts(list));
  const candidates = voiceCandidates(list, getLang());
  if (!candidates.length) { stop('novoice'); return; }
  const found = await proveVoice(candidates, getRate(), (a) => { session.attempts.push(a); results.save(); });
  if (!found.chosen) { stop('silent'); return; }
  useVoice(found.chosen, session.lang, list);
  Object.assign(session, { check: 'ok', checkMs: found.ms, english: found.chosen.english, spoke: found.chosen.voice ? describeVoice(found.chosen.voice) : null });
  results.save();
  await Promise.race([found.end, new Promise((r) => setTimeout(r, END_CAP))]);
  handover();
});

// No voice to speak with: the screen reader stays on, the page says why, and nothing hands-free starts.
function stop(kind) {
  session.check = kind;
  results.save();
  leaveFullscreen();
  phase = 'start';
  tell(kind === 'novoice' ? 'phone.noVoice' : 'phone.silent');
  results.show();
}

// The game asks for the screen reader to be turned off. A one-finger swipe reaching the page proves it
// is: the screen reader would have taken the swipe for itself.
function handover() {
  phase = 'handover';
  handsFree(true);
  view.handover();
  const at = performance.now();
  const sayIt = () => say(t('phone.handover'), { priority: 'high', key: 'handover', wait: 5000 });
  sayIt();
  repeat = setInterval(sayIt, HANDOVER_EVERY);
  surface.use(createMenuGestures({
    onSwipe() {
      clearInterval(repeat);
      session.handover = (performance.now() - at) / 1000;
      results.save();
      // The swipe is a real touch: start the sound again, and fullscreen if a trip to Settings to turn
      // TalkBack off left it (both need a gesture).
      ensureAudio();
      if (session.fullscreen === 'ok' && !document.fullscreenElement) enterFullscreen().then((r) => r === 'ok' && lockPortrait());
      enterMenu('phone.menu.how');
    },
  }));
}

function sayOption(option, cut) {
  view.menu(option);
  say(t(`phone.options.${option}`), { priority: 'high', key: 'menu', cut, wait: 20000 });
}

// line: what is said on entering (how the menu works the first time, "Back to the menu" after that).
function enterMenu(line) {
  phase = 'menu';
  menu = menu || createMenu();
  say(t(line), { priority: 'high', wait: 20000 });
  sayOption(menu.current, false);
  const act = (gesture) => {
    const r = menu.act(gesture);
    if (r.say) sayOption(r.say, true); // a swipe cuts off whatever is being said, as with TalkBack
    if (r.choose) choose(r.choose);
  };
  surface.use(createMenuGestures({ onSwipe: act, onDoubleTap: () => act('double'), onTap: () => act('tap') }));
}

// Runs inside the double-tap's pointer event, so tilt can ask for motion permission (iOS).
function choose(option) {
  if (option === 'finish') { finish(); return; }
  phase = 'play';
  cancel();
  run = createRun(option, {
    onTilt() { session.tilt = 'seen'; results.save(); },
    onEnd(block, why) {
      run = null;
      if (block) session.schemes.push({ ...block, at: isoMinute() });
      if (why === 'notilt' && session.tilt !== 'seen') session.tilt = 'none';
      results.save();
      enterMenu(why === 'notilt' ? 'phone.noTilt' : 'phone.menu.back');
    },
  });
  view.run(option);
  surface.use(run.gestures);
  run.start(performance.now());
}

function finish() {
  phase = 'start'; // Start again begins a new session
  cancel();
  say(t('phone.done'), { priority: 'high' });
  surface.use(null);
  handsFree(false);
  leaveFullscreen();
  results.show(true);
}

let last = performance.now();
function frame(now) {
  const dt = Math.min(0.05, Math.max(0, (now - last) / 1000));
  last = now;
  surface.tick(now);
  run?.update(dt, now);
  view.frame(run?.status(), now);
  requestAnimationFrame(frame);
}
requestAnimationFrame(frame);

// The screen went off or another app came up: the round waits. The wake lock is let go while hidden.
document.addEventListener('visibilitychange', () => {
  if (document.hidden) { run?.pause(); return; }
  run?.resume();
  if (phase !== 'start' && phase !== 'check') keepAwake();
});

window.__phoneTest = { phase: () => phase, menu: () => menu?.current ?? null, run: () => run?.snapshot() ?? null, session: () => session };
