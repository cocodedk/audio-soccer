// The side-call words as short recorded clips (see sounds/README.md). A clip plays at once and a newer
// one cuts it off, so a call is never late. It needs no speech voice and never queues behind a screen
// reader. loadClips() runs once the audio is started; until a clip is loaded, playClip() returns false
// and the caller speaks the word instead.
import { audioCtx, output } from './audio.js';
import { LANGS } from './i18n/index.js';

export const SIDE_WORDS = ['ahead', 'left', 'right', 'behind'];
const CLIP_GAIN = 0.6;
const buffers = {};
let loading = null, playing = null, settled = false;

export function loadClips() {
  const ctx = audioCtx();
  if (!ctx || loading) return loading;
  loading = Promise.all(LANGS.flatMap((lang) => SIDE_WORDS.map(async (word) => {
    try {
      // no-cache: ask the server each time, so a re-recorded clip is never served stale from the browser.
      const res = await fetch(`sounds/side-${word}.${lang}.wav`, { cache: 'no-cache' });
      buffers[`${word}.${lang}`] = await ctx.decodeAudioData(await res.arrayBuffer());
    } catch (e) { /* this word stays spoken */ }
  }))).then(() => { settled = true; });
  return loading;
}

// True once every clip has loaded or failed (or there is no audio at all): a word still missing then is
// missing for good and is spoken. Before that, the caller waits for its clip rather than say it twice.
export const clipsSettled = () => settled || !audioCtx();

// Play the side word in lang. False when there is no clip for it (yet), so the caller can speak it.
export function playClip(word, lang) {
  const ctx = audioCtx(), buf = buffers[`${word}.${lang}`];
  if (!ctx || !buf) return false;
  try { playing?.stop(); } catch (e) { /* already ended */ }
  const s = ctx.createBufferSource();
  s.buffer = buf;
  s.connect(output({ gain: CLIP_GAIN }));
  s.start();
  playing = s;
  return true;
}
