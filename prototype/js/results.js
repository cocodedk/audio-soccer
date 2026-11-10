// The results block: finished levels, plus the rounds of a level the player left, as text to copy.
// Mirrored to localStorage so a reload keeps it; with no storage it simply lives in memory.
import { resultsText, isoMinute } from './report.js';
import { summarySpeech } from './levels.js';
import { loadPref, savePref } from './storage.js';
import { getLang, t } from './i18n/index.js';

const CLEAR_WINDOW = 15000, CLEAR_HOLD = 700; // ms: the confirm window; presses this close are a held key
const valid = (b) => b && Number.isInteger(b.level) && b.level >= 1 && b.level <= 3 && Array.isArray(b.rounds) && b.rounds.length > 0;

// els: { box, summary, text, copyStatus, heading, copyBtn, clearBtn, onEmpty(msg) }. getMeta(): { mode, rate, hidden,
// sound, lang } now. Each block keeps the settings and time of when it ended (a block that already has
// them, from when its level was paused, keeps those), so a tester who switches from speakers to
// headphones between levels is reported right.
export function createResults(els, getMeta) {
  const { box, summary, text, copyStatus, heading, copyBtn, clearBtn, onEmpty } = els;
  let finished = [], shown = null, previewing = false, clearArmed = 0, lastPress = 0;
  const now = () => ({ meta: getMeta(), at: isoMinute() });
  const stamp = (block) => ({ ...now(), ...block });
  const browser = () => (typeof navigator !== 'undefined' && navigator.userAgent) || '';

  const sayIt = () => { summary.textContent = summarySpeech(shown.level, shown.rounds, !!shown.partial, getLang()); };

  // Draw blocks (finished levels, and, when previewing, the level in progress last).
  function draw(blocks, preview = false) {
    shown = blocks[blocks.length - 1];
    previewing = preview;
    sayIt();
    text.value = resultsText(blocks, browser());
    copyStatus.textContent = '';
    clearArmed = 0;
    box.hidden = false;
  }

  // Only what is final goes to storage. A preview of a level still being played is never saved: after a
  // reload it would be permanent, and finishing that level later would list it twice.
  const persist = () => savePref('results', JSON.stringify({ blocks: finished }));

  try {
    const saved = JSON.parse(loadPref('results', 'null'));
    if (saved && Array.isArray(saved.blocks) && saved.blocks.every(valid) && saved.blocks.length) {
      // The old format kept one meta for all blocks: give it to each block that has none.
      finished = saved.blocks.map((b) => (b.meta || !saved.meta ? b : { ...b, meta: saved.meta }));
      draw(finished);
    }
  } catch (e) { /* unreadable or no storage: start empty */ }

  copyBtn.addEventListener('click', () => {
    const selectIt = () => {
      const keys = /Mac|iPhone|iPad/.test(navigator.platform || '') ? 'Command C' : 'Control C';
      copyStatus.textContent = t('copy.blocked', { keys });
      text.focus();
      text.select();
    };
    try {
      navigator.clipboard.writeText(text.value).then(() => { copyStatus.textContent = t('copy.done'); }, selectIt);
    } catch (e) {
      selectIt();
    }
  });

  // Two presses within CLEAR_WINDOW ms empty the stored results; the first press says so through the
  // status line, which a screen reader announces (no dialog to get lost in). Presses closer than
  // CLEAR_HOLD ms to the one before are a held Enter and do nothing.
  clearBtn.addEventListener('click', () => {
    const at = Date.now(), held = at - lastPress < CLEAR_HOLD;
    lastPress = at;
    if (held) return;
    if (at - clearArmed > CLEAR_WINDOW) {
      const armed = clearArmed = at;
      // Emptied first and set a tick later, so a second arming (same words) is announced again.
      copyStatus.textContent = '';
      setTimeout(() => { if (clearArmed === armed) copyStatus.textContent = t('clear.confirm'); }, 50);
      return;
    }
    // The level in progress (shown again at each pause). Not a left level already saved, which is partial too.
    const kept = previewing ? shown : null;
    finished = [];
    persist();
    if (kept) {
      draw([kept], true);
      copyStatus.textContent = t('clear.kept');
      return;
    }
    clearArmed = 0;
    shown = null;
    previewing = false;
    text.value = '';
    summary.textContent = '';
    copyStatus.textContent = t('clear.done');
    if (onEmpty) onEmpty(t('clear.done')); // moves focus out first: the panel and its status line go
    box.hidden = true;
  });

  return {
    // The language changed: say the summary again in it (the report text stays English).
    relabel() { if (shown) sayIt(); copyStatus.textContent = ''; clearArmed = 0; },
    // Settings and time now, to stamp a level paused now and left later.
    now,
    // A level is over, or was left unfinished and is being replaced: keep it for good.
    add(block) {
      if (!valid(block)) return;
      finished.push(stamp(block));
      persist();
    },
    // Show everything so far; preview is an unfinished level shown without being kept.
    show(preview = null, focus = false) {
      const live = !!preview && valid(preview);
      const blocks = live ? [...finished, stamp(preview)] : finished;
      if (!blocks.length) return;
      draw(blocks, live);
      if (focus) heading.focus();
    },
  };
}
