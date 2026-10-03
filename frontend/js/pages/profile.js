import { avatarHtml } from '../components/avatar.js';
import { renderRecipeCarousel } from '../components/carousel.js';
import { initPage } from '../components/layout.js';
import { toast } from '../components/toast.js';
import { api } from '../core/api.js';
import { ROUTES } from '../core/config.js';
import { $, displayName, escapeHtml } from '../core/dom.js';
import { formatCount } from '../core/format.js';
import { t, tError } from '../core/i18n.js';

/**
 * Public profile (mockup "Profile Page"): /pages/profile.html?u=<username>; without ?u= it shows
 * your own. Uploaded recipes for everyone, plus your private "Saved recipes" on your own profile.
 */

const me = await initPage();
const username = new URLSearchParams(location.search).get('u') ?? me?.username;
let profile = null;

if (!username) {
  location.replace(ROUTES.login + '?next=' + encodeURIComponent(location.pathname));
} else {
  try {
    profile = await api.get(`/api/users/${encodeURIComponent(username)}`);
    render();
  } catch {
    $('[data-not-found]').hidden = false;
  }
}

function render() {
  const isMe = me?.id === profile.id;
  const memberSince = new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' })
    .format(new Date(profile.createdAt));
  const facts = [
    profile.age ? `🎂 ${t('publicProfile.age', { age: profile.age })}` : null,
    `🗓️ ${t('publicProfile.joined', { date: memberSince })}`,
  ].filter(Boolean);
  const stat = (value, label) => `<div><strong>${formatCount(value)}</strong><span>${escapeHtml(label)}</span></div>`;

  const hero = $('#profile-hero');
  hero.innerHTML = `
    ${avatarHtml(profile)}
    <div>
      <h1 class="profile-hero__name">${escapeHtml(displayName(profile))}</h1>
      <p class="profile-hero__username">@${escapeHtml(profile.username)}</p>
      <div class="stats">
        ${stat(profile.recipeCount, t('publicProfile.recipes'))}
        ${stat(profile.followerCount, t('publicProfile.followers'))}
        ${stat(profile.followingCount, t('publicProfile.following'))}
      </div>
      ${profile.bio ? `<p class="profile-hero__bio">${escapeHtml(profile.bio)}</p>` : ''}
      <ul class="profile-hero__facts">${facts.map((f) => `<li>${escapeHtml(f)}</li>`).join('')}</ul>
    </div>
    <div class="profile-hero__actions">
      ${isMe
        ? `<a class="btn btn--rust" href="${ROUTES.account}#profile">✏️ ${escapeHtml(t('publicProfile.edit'))}</a>`
        : `<button class="btn btn--rust ${profile.following ? 'is-active' : ''}" type="button" data-follow aria-pressed="${profile.following}">
             ${profile.following ? '✓ ' + escapeHtml(t('social.subscribed')) : '🔔 ' + escapeHtml(t('publicProfile.subscribe'))}
           </button>
           <a class="btn btn--ghost" style="background:var(--white)" data-message
              href="${me ? `${ROUTES.chat}?with=${encodeURIComponent(profile.username)}` : `${ROUTES.login}?next=${encodeURIComponent(location.pathname + location.search)}`}">
             💬 ${escapeHtml(t('chat.message'))}</a>`}
    </div>`;
  hero.hidden = false;
  hero.removeAttribute('aria-busy');
  hero.querySelector('[data-follow]')?.addEventListener('click', toggleFollow);
  document.title = `${displayName(profile)} (@${profile.username}) · Cookify`;

  $('[data-recipes]').hidden = false;
  $('[data-upload-own]').hidden = !isMe;
  $('[data-saved]').hidden = !isMe;
}

async function toggleFollow(event) {
  if (!me) {
    location.href = `${ROUTES.login}?next=${encodeURIComponent(location.pathname + location.search)}`;
    return;
  }
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const path = `/api/users/${encodeURIComponent(profile.username)}/follow`;
    const result = profile.following ? await api.delete(path) : await api.put(path);
    Object.assign(profile, { following: result.following, followerCount: result.followerCount });
    toast(t(result.following ? 'social.subscribedToast' : 'social.unsubscribedToast', { name: profile.username }), 'success');
    render();
  } catch (error) {
    toast(tError(error.code) ?? error.message, 'error');
    button.disabled = false;
  }
}

async function loadLists() {
  const isMe = me?.id === profile.id;
  const uploaded = await api.get(`/api/users/${encodeURIComponent(profile.username)}/recipes?size=24`, { auth: false });
  renderRecipeCarousel($('#recipe-list'), uploaded.content, t(isMe ? 'publicProfile.noRecipesOwn' : 'publicProfile.noRecipes'));
  if (isMe) {
    const saved = await api.get('/api/users/me/saved?size=24');
    renderRecipeCarousel($('#saved-list'), saved.content, t('publicProfile.noSaved'));
  }
}

if (profile) await loadLists();
