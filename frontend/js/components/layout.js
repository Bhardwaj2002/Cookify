import { api } from '../core/api.js';
import { getCurrentUser, logout, redirectIfLoggedIn, requireUser } from '../core/auth.js';
import { ROUTES } from '../core/config.js';
import { escapeHtml } from '../core/dom.js';
import {
  currentLanguage, hasLocalLanguageChoice, initI18n, LANGUAGES, setLanguage, t, translatePage,
} from '../core/i18n.js';
import { avatarHtml } from './avatar.js';
import { chefHat } from './icons.js';

/** Re-renders the header after the user's name or picture changes. */
export function refreshHeader(user) {
  renderHeader(user);
}

/**
 * Bootstraps every page: translations, header (auth-aware) and footer.
 * @param {{ access?: 'public' | 'user' | 'guest' }} options
 *   public – anyone; user – must be logged in; guest – logged-in users are sent onwards
 * @returns {Promise<object|null>} the current user
 */
export async function initPage({ access = 'public' } = {}) {
  await initI18n();
  renderFooter();

  let user = null;
  if (access === 'user') user = await requireUser();
  else if (access === 'guest') await redirectIfLoggedIn();
  else user = await getCurrentUser();

  // On a new device, use the language saved in the user's profile.
  if (user?.preferredLang && !hasLocalLanguageChoice() && user.preferredLang !== currentLanguage()) {
    setLanguage(user.preferredLang);
    return new Promise(() => {}); // page reloads in the right language
  }

  renderHeader(user);
  document.body.classList.add('is-ready');
  return user;
}

function renderHeader(user) {
  const header = document.getElementById('site-header');
  if (!header) return;

  const nav = user
    ? `${user.role === 'ADMIN' ? `<a class="nav__link" href="${ROUTES.admin}">🛡 <span data-i18n="nav.admin"></span></a>` : ''}
       <a class="nav__link nav__badge" href="${ROUTES.chat}">💬 <span data-i18n="nav.chat"></span><span class="unread-badge" data-chat-badge hidden></span></a>
       <a class="btn btn--sm nav__upload" href="${ROUTES.recipeEditor}">＋ <span data-i18n="nav.upload"></span></a>
       <a class="nav__user" href="${ROUTES.account}">
         ${avatarHtml(user)}
         <span>${escapeHtml(user.username)}</span>
       </a>
       <button type="button" class="btn btn--ghost btn--sm" data-action="logout" data-i18n="nav.logout"></button>`
    : `<a class="btn btn--ghost btn--sm" href="${ROUTES.login}" data-i18n="nav.login"></a>
       <a class="btn btn--sm" href="${ROUTES.signup}" data-i18n="nav.signup"></a>`;

  header.className = 'site-header';
  header.innerHTML = `
    <div class="site-header__inner">
      <a class="logo" href="${ROUTES.home}" aria-label="Cookify home">
        <span><span class="logo__cook">COOK</span><span class="logo__ify">ify</span></span>
        <span class="logo__hat">${chefHat}</span>
      </a>
      <nav class="nav" aria-label="${escapeHtml(t('nav.label'))}">
        <a class="nav__link" href="${ROUTES.browse}">🔍 <span data-i18n="nav.browse"></span></a>
        ${nav}
        <label class="lang-select" title="${escapeHtml(t('language.label'))}">
          <span aria-hidden="true">🌐</span>
          <select data-language aria-label="${escapeHtml(t('language.label'))}">
            ${Object.entries(LANGUAGES).map(([code, name]) =>
              `<option value="${code}" ${code === currentLanguage() ? 'selected' : ''}>${escapeHtml(name)}</option>`).join('')}
          </select>
        </label>
      </nav>
    </div>`;
  translatePage(header);
  header.querySelector('[data-action="logout"]')?.addEventListener('click', logout);
  header.querySelector('[data-language]').addEventListener('change', (e) => changeLanguage(e.target.value, user));
  if (user) {
    api.get('/api/chat/unread').then((r) => setChatBadge(r.unread)).catch(() => {});
  }
}

/**
 * Switches the UI language. Logged-in users also get it saved to their profile so it follows them
 * to other devices (the profile API replaces all editable fields, so the current values are resent).
 */
async function changeLanguage(lang, user) {
  setLanguage(lang, { reload: false });
  if (user) {
    try {
      const me = await api.get('/api/users/me');
      await api.put('/api/users/me', {
        username: me.username, firstName: me.firstName, lastName: me.lastName, phone: me.phone,
        age: me.age, gender: me.gender, bio: me.bio, preferredLang: lang,
      });
    } catch {
      /* the local choice still applies */
    }
  }
  location.reload();
}

/** Shows the unread-messages count on the header's Chat link. */
export function setChatBadge(count) {
  const badge = document.querySelector('[data-chat-badge]');
  if (!badge) return;
  badge.textContent = count > 99 ? '99+' : String(count);
  badge.hidden = !count;
}

function renderFooter() {
  const footer = document.getElementById('site-footer');
  if (!footer) return;
  footer.className = 'site-footer';
  footer.innerHTML = `
    <div class="site-footer__inner">
      <div>
        <h2 data-i18n="footer.contact"></h2>
        <p><span data-i18n="footer.contactInfo"></span> +91 1234567890 · +91 0987654321</p>
      </div>
      <a href="${ROUTES.home}#about" data-i18n="footer.about"></a>
    </div>`;
  translatePage(footer);
}
