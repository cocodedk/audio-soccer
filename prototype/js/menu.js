// Start-screen controls: the touch notice, speech rate and voice selects, headphone check, Learn the sounds.
import { ensureAudio } from './audio.js';
import { say } from './speech.js';
import { getRate, setRate, listVoices, currentVoice, setVoice } from './voices.js';
import { headphoneCheck, learnSounds, stopLearn } from './learn.js';

const $ = (s) => document.querySelector(s);
const rateEl = $('#rate'), voiceEl = $('#voice'), noticeEl = $('#touch-notice');

export const showRate = () => { rateEl.value = getRate().toFixed(1); };

// Fill the voice select from the engine's English voices, best (local) first. Called as voices load.
export function fillVoices() {
  const list = listVoices(), cur = currentVoice();
  voiceEl.textContent = '';
  if (!list.length) voiceEl.add(new Option('Default voice', ''));
  for (const v of list) {
    voiceEl.add(new Option(`${v.name}, ${v.lang}${v.localService ? '' : ', online'}`, v.voiceURI, false, v === cur));
  }
  voiceEl.disabled = !list.length;
}

// A touch device with no key pressed so far: say at once that this needs a keyboard.
function touchNotice() {
  let coarse = false;
  try { coarse = !!window.matchMedia?.('(pointer: coarse)').matches; } catch (e) { /* no matchMedia */ }
  if (!coarse) return;
  const text = 'This needs a computer with a keyboard. It does not work on a phone or tablet.';
  noticeEl.hidden = false;
  setTimeout(() => { noticeEl.textContent = text; }, 50); // after it is shown, so a screen reader announces it
  document.addEventListener('keydown', () => { noticeEl.hidden = true; }, { once: true });
}

// applyMode() sends the chosen voice mode (the radios) to the announcer.
export function initMenu({ applyMode }) {
  showRate();
  fillVoices();
  rateEl.addEventListener('change', () => {
    setRate(Number(rateEl.value));
    say(`Speech speed ${rateEl.value}`, { priority: 'high', key: 'rate' });
  });
  voiceEl.addEventListener('change', () => {
    setVoice(voiceEl.value);
    say('This is the voice.', { priority: 'high' });
  });
  // Both buttons need the audio context, which a click is allowed to start.
  const withAudio = (fn) => () => {
    if (!ensureAudio()) { say('This browser cannot play sound.', { priority: 'high' }); return; }
    applyMode();
    stopLearn();
    fn();
  };
  $('#hp-check').addEventListener('click', withAudio(headphoneCheck));
  $('#learn').addEventListener('click', withAudio(learnSounds));
  touchNotice();
}
