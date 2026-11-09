// Puts the current language on the page: <html lang dir>, the title, the live regions, and every element
// marked data-i18n (text), data-i18n-html (our own markup, e.g. <kbd>) or data-i18n-aria (aria-label).
import { getLang, t, DIR } from './index.js';

export function applyLanguage(root = document) {
  const lang = getLang(), html = document.documentElement;
  html.lang = lang;
  html.dir = DIR[lang];
  document.title = t('ui.title');
  // The live regions carry the language too, so a screen reader switches to its Persian voice.
  root.querySelectorAll('[aria-live]').forEach((el) => { el.lang = lang; el.dir = DIR[lang]; });
  root.querySelectorAll('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
  root.querySelectorAll('[data-i18n-html]').forEach((el) => { el.innerHTML = t(el.dataset.i18nHtml); });
  root.querySelectorAll('[data-i18n-aria]').forEach((el) => { el.setAttribute('aria-label', t(el.dataset.i18nAria)); });
}
