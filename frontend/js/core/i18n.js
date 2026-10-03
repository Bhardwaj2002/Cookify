/**
 * Tiny i18n layer. Static text uses data attributes; scripts use t():
 *   <span data-i18n="login.title"></span>
 *   <input data-i18n-placeholder="login.passwordPlaceholder">
 *   t('chat.typing', { name: 'Meera' })
 * Six languages ship in /locales; any key missing from a translation falls back to English.
 * <html lang> is kept in sync so the browser's own translation (e.g. Google Translate) also
 * handles user-written content such as recipe text and comments.
 */

const STORAGE_KEY = 'cookify.lang';
const FALLBACK = 'en';

/** Code → name shown in the language menu (in its own script). */
export const LANGUAGES = {
  en: 'English',
  hi: 'हिन्दी',
  zh: '中文',
  ru: 'Русский',
  es: 'Español',
  fr: 'Français',
};
export const SUPPORTED_LANGUAGES = Object.keys(LANGUAGES);

let messages = {};
let fallbackMessages = {};
let current = FALLBACK;

export async function initI18n() {
  current = preferredLanguage();
  fallbackMessages = await load(FALLBACK);
  messages = current === FALLBACK ? fallbackMessages : await load(current);
  document.documentElement.lang = current;
  translatePage();
}

export function currentLanguage() {
  return current;
}

/** True when the visitor picked a language on this device (it then wins over the profile setting). */
export function hasLocalLanguageChoice() {
  return SUPPORTED_LANGUAGES.includes(readStored());
}

/** Remembers the choice on this device and reloads the page in that language. */
export function setLanguage(lang, { reload = true } = {}) {
  if (!SUPPORTED_LANGUAGES.includes(lang)) return;
  try {
    localStorage.setItem(STORAGE_KEY, lang);
  } catch {
    /* storage unavailable: the choice lasts for this page only */
  }
  if (reload && lang !== current) location.reload();
}

export function t(key, vars = {}) {
  const template = lookup(messages, key) ?? lookup(fallbackMessages, key) ?? key;
  return template.replace(/\{(\w+)\}/g, (_, name) => vars[name] ?? `{${name}}`);
}

/** Returns a translated message for an API error code, or null if none exists. */
export function tError(code) {
  const key = `errors.${code}`;
  return lookup(messages, key) ?? lookup(fallbackMessages, key) ?? null;
}

export function translatePage(root = document) {
  root.querySelectorAll('[data-i18n]').forEach((el) => {
    el.textContent = t(el.dataset.i18n);
  });
  root.querySelectorAll('[data-i18n-html]').forEach((el) => {
    el.innerHTML = t(el.dataset.i18nHtml);
  });
  root.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
    el.placeholder = t(el.dataset.i18nPlaceholder);
  });
  root.querySelectorAll('[data-i18n-aria-label]').forEach((el) => {
    el.setAttribute('aria-label', t(el.dataset.i18nAriaLabel));
  });
  const titleKey = document.documentElement.dataset.i18nTitle;
  if (titleKey) document.title = `${t(titleKey)} · Cookify`;
}

function readStored() {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

/** Stored choice, else the browser's language if we support it, else English. */
function preferredLanguage() {
  const stored = readStored();
  if (SUPPORTED_LANGUAGES.includes(stored)) return stored;
  const browser = (navigator.language || '').slice(0, 2).toLowerCase();
  return SUPPORTED_LANGUAGES.includes(browser) ? browser : FALLBACK;
}

async function load(lang) {
  try {
    const res = await fetch(`/locales/${lang}.json`);
    return res.ok ? await res.json() : {};
  } catch {
    return {};
  }
}

function lookup(obj, key) {
  const value = key.split('.').reduce((node, part) => node?.[part], obj);
  return typeof value === 'string' ? value : null;
}
