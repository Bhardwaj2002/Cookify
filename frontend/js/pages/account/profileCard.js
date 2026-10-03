import { avatarHtml } from '../../components/avatar.js';
import { profileUrl } from '../../core/config.js';
import { $, displayName, escapeHtml } from '../../core/dom.js';
import { t } from '../../core/i18n.js';

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });
export const formatDate = (iso) => (iso ? dateFormat.format(new Date(iso)) : '');

/** Left-hand summary card: picture, name, contact details, 2FA badge. */
export function renderProfileCard(user) {
  const card = $('#profile-card');
  const row = (labelKey, value) => `
    <div><dt>${escapeHtml(t(labelKey))}</dt>
    <dd>${value ? escapeHtml(value) : `<span class="text-muted">${escapeHtml(t('account.notSet'))}</span>`}</dd></div>`;

  card.innerHTML = `
    ${avatarHtml(user, 'avatar--lg')}
    <h2 class="profile-card__name">${escapeHtml(displayName(user))}</h2>
    <p class="profile-card__username">@${escapeHtml(user.username)}</p>
    <span class="badge ${user.twoFactorEnabled ? 'badge--success' : ''}">
      🔒 2FA ${escapeHtml(t(user.twoFactorEnabled ? 'account.twoFactorOn' : 'account.twoFactorOff'))}
    </span>
    <dl class="details">
      ${row('account.email', user.email)}
      ${row('account.phone', user.phone)}
    </dl>
    <p class="text-muted" style="margin:16px 0 0;font-size:.8rem">
      ${escapeHtml(t('account.memberSince', { date: formatDate(user.createdAt) }))}
    </p>
    <a class="profile-card__link" href="${profileUrl(user.username)}">${escapeHtml(t('account.viewPublic'))} →</a>`;
  card.removeAttribute('aria-busy');
}
