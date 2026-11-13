// What the touch surface shows a sighted helper watching the phone test: where the test is and the
// gestures that work now, the menu, the pitch during a round, and the lines as they are spoken. Display
// only: none of it takes a touch (phone.css), and speech.js and clips.js tell it what was said once it is
// said, so it can never hold up or change a line or a sound.
import { onLine } from '../../js/speech.js';
import { onClip } from '../../js/clips.js';
import { createRenderer } from '../../js/render.js';
import { t } from '../../js/i18n/index.js';
import { ROUNDS } from '../../js/levels.js';
import { OPTIONS } from './menu.js';

const KEEP = 4;       // lines on screen: the one being spoken and the ones just before it
const CALL_MS = 1500; // how long a side-call word stays up
const ARROW = { ahead: '▲', left: '◀', right: '▶', behind: '▼' };
const GESTURES = {
  handover: ['swipeUp'], menu: ['move', 'choose', 'repeat'], intro: ['back'],
  drag: ['drag', 'kick', 'where', 'back'], tilt: ['tilt', 'kick', 'where', 'back'],
};
// The canvas is turned so that up the screen is toward the goal, which would turn its words on their
// side: it draws none, and the page puts them upright around it.
const NO_LABELS = { left: '', right: '', attack: '', you: '', keeper: '' };

export function createView(root) {
  const $ = (s) => root.querySelector(s);
  const title = $('.v-title'), gestures = $('.v-gestures'), note = $('.v-note'), menuEl = $('.v-menu');
  const pitch = $('.v-pitch'), cv = $('.v-pitch canvas'), call = $('.v-call'), linesEl = $('.v-lines');
  // The size before the turn: getBoundingClientRect would give the turned box.
  const renderer = createRenderer(cv, () => NO_LABELS, () => ({ width: cv.offsetWidth, height: cv.offsetHeight }));
  let scheme = null, shown = '', said = [], callUntil = 0;

  const fill = (el, texts, cls = () => '') => el.replaceChildren(...texts.map((text, i) => {
    const li = document.createElement('li');
    li.textContent = text;
    li.className = cls(i);
    return li;
  }));

  // part: which of the menu, the pitch and the intro note is shown under the gestures (none: null).
  function stage(heading, set, part) {
    title.textContent = heading;
    fill(gestures, GESTURES[set].map((g) => t(`phone.view.g.${g}`)));
    menuEl.hidden = part !== 'menu';
    pitch.hidden = part !== 'play';
    note.hidden = part !== 'intro';
  }

  // The newest line is the one being spoken; a line said again (the hand-over's repeat) is not doubled.
  onLine((text) => {
    if (said[said.length - 1] === text) return;
    said = [...said, text].slice(-KEEP);
    fill(linesEl, said, (i) => (i === said.length - 1 ? 'now' : ''));
  });
  onClip((word) => {
    call.textContent = `${ARROW[word] || ''} ${t(`call.${word}`)}`;
    call.hidden = false;
    callUntil = performance.now() + CALL_MS;
  });

  return {
    handover() { scheme = null; stage(t('phone.view.handover'), 'handover', null); },
    menu(current) {
      scheme = null;
      stage(t('phone.view.menu'), 'menu', 'menu');
      fill(menuEl, OPTIONS.map((o) => t(`phone.options.${o}`)), (i) => (OPTIONS[i] === current ? 'on' : ''));
    },
    run(chosen) { scheme = chosen; shown = ''; },
    // Each frame. status: the run's ({ phase, round, world }, play.js), or nothing outside a run.
    frame(status, now) {
      if (!call.hidden && now > callUntil) call.hidden = true;
      if (!scheme || !status || status.phase === 'over') return;
      const name = t(`phone.options.${scheme}`), playing = status.phase === 'play' && status.round > 0;
      const key = playing ? `round${status.round}` : 'intro';
      if (key !== shown) {
        shown = key;
        if (playing) stage(t('phone.view.round', { scheme: name, round: status.round, total: ROUNDS }), scheme, 'play');
        else stage(t('phone.view.intro', { scheme: name }), 'intro', 'intro');
      }
      if (playing) renderer.draw(status.world);
    },
  };
}
