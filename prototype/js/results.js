// The results block: finished levels, plus the rounds of a level the player left, as text to copy.
// Mirrored to localStorage so a reload keeps it; with no storage it simply lives in memory.
import { resultsText } from './report.js';
import { summarySpeech } from './levels.js';
import { loadPref, savePref } from './storage.js';
import { getLang, t } from './i18n/index.js';

const valid = (b) => b && Number.isInteger(b.level) && b.level >= 1 && b.level <= 3 && Array.isArray(b.rounds) && b.rounds.length > 0;

// els: { box, summary, text, copyStatus, heading, copyBtn }. getMeta(): { mode, rate, hidden, sound, lang } now.
export function createResults(els, getMeta) {
  const { box, summary, text, copyStatus, heading, copyBtn } = els;
  let finished = [], shown = null;

  const sayIt = () => { summary.textContent = summarySpeech(shown.level, shown.rounds, !!shown.partial, getLang()); };

  // Draw blocks (finished levels, and an optional unfinished one) with the given voice settings.
  function draw(blocks, meta) {
    shown = blocks[blocks.length - 1];
    sayIt();
    text.value = resultsText(blocks, meta);
    copyStatus.textContent = '';
    box.hidden = false;
  }

  // Only what is final goes to storage. A preview of a level still being played is never saved: after a
  // reload it would be permanent, and finishing that level later would list it twice.
  const persist = () => savePref('results', JSON.stringify({ blocks: finished, meta: getMeta() }));

  try {
    const saved = JSON.parse(loadPref('results', 'null'));
    if (saved && Array.isArray(saved.blocks) && saved.blocks.every(valid) && saved.blocks.length) {
      finished = saved.blocks;
      draw(finished, saved.meta || getMeta());
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

  return {
    // The language changed: say the summary again in it (the report text stays English).
    relabel() { if (shown) sayIt(); copyStatus.textContent = ''; },
    // A level is over, or was left unfinished and is being replaced: keep it for good.
    add(block) {
      if (!valid(block)) return;
      finished.push(block);
      persist();
    },
    // Show everything so far; preview is an unfinished level shown without being kept.
    show(preview = null, focus = false) {
      const blocks = preview && valid(preview) ? [...finished, preview] : finished;
      if (!blocks.length) return;
      draw(blocks, getMeta());
      if (focus) heading.focus();
    },
  };
}
