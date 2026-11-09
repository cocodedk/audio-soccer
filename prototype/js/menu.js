// Start-screen controls: the touch notice, the sound setting, speech rate and voice selects, the
// headphone check and Learn the sounds. Language is in language.js.
import { ensureAudio } from './audio.js';
import { loadClips } from './clips.js';
import { say } from './speech.js';
import { getRate, setRate, listVoices, currentVoice, setVoice } from './voices.js';
import { getSoundMode, setSoundMode, speakersOn } from './soundmode.js';
import { headphoneCheck, learnSounds, stopLearn } from './learn.js';
import { t } from './i18n/index.js';

const $ = (s) => document.querySelector(s);
const rateEl = $('#rate'), voiceEl = $('#voice'), noticeEl = $('#touch-notice');

export const showRate = () => { rateEl.value = getRate().toFixed(1); };

// The speech speeds, written in the current language (Persian digits in Persian).
const fillRates = () => { for (const o of rateEl.options) o.textContent = t('num', { n: Number(o.value).toFixed(1) }); };

// Fill the voice select from the engine's voices for the current language, best (local) first. Called
// as voices load and when the language changes.
export function fillVoices() {
  const list = listVoices(), cur = currentVoice();
  voiceEl.textContent = '';
  if (!list.length) voiceEl.add(new Option(t('voiceDefault'), ''));
  for (const v of list) {
    voiceEl.add(new Option(`${v.name}, ${v.lang}${v.localService ? '' : `, ${t('online')}`}`, v.voiceURI, false, v === cur));
  }
  voiceEl.disabled = !list.length;
}

// Show the sound setting: the radios, the note under them, and the headphone check, which only
// headphones need.
function showSound() {
  document.querySelectorAll('input[name="sound"]').forEach((r) => { r.checked = r.value === getSoundMode(); });
  $('#sound-note').textContent = t(speakersOn() ? 'ui.soundNoteSpeakers' : 'ui.soundNoteHeadphones');
  $('#hp-check').hidden = speakersOn();
}

// Everything in the menu that is built in code, again, for the current language.
export function relabel() {
  fillRates();
  fillVoices();
  showSound();
  if (!noticeEl.hidden) noticeEl.textContent = t('touch');
}

// A touch device with no key pressed so far: say at once that this needs a keyboard.
function touchNotice() {
  let coarse = false;
  try { coarse = !!window.matchMedia?.('(pointer: coarse)').matches; } catch (e) { /* no matchMedia */ }
  if (!coarse) return;
  noticeEl.hidden = false;
  setTimeout(() => { noticeEl.textContent = t('touch'); }, 50); // after it is shown, so a screen reader announces it
  document.addEventListener('keydown', () => { noticeEl.hidden = true; }, { once: true });
}

// applyMode() sends the chosen voice mode (the radios) to the announcer.
export function initMenu({ applyMode }) {
  showRate();
  relabel();
  rateEl.addEventListener('change', () => {
    setRate(Number(rateEl.value));
    say(t('speechSpeed', { rate: Number(rateEl.value).toFixed(1) }), { priority: 'high', key: 'rate' });
  });
  voiceEl.addEventListener('change', () => {
    setVoice(voiceEl.value);
    say(t('voiceIs'), { priority: 'high' });
  });
  document.querySelectorAll('input[name="sound"]').forEach((r) => r.addEventListener('change', () => {
    stopLearn();
    setSoundMode(r.value);
    showSound();
    say(t(`ui.${r.value}`), { priority: 'high' });
  }));
  // Both buttons need the audio context, which a click is allowed to start.
  const withAudio = (fn) => () => {
    if (!ensureAudio()) { say(t('noSound'), { priority: 'high' }); return; }
    loadClips();
    applyMode();
    stopLearn();
    fn();
  };
  $('#hp-check').addEventListener('click', withAudio(headphoneCheck));
  $('#learn').addEventListener('click', withAudio(learnSounds));
  touchNotice();
}
