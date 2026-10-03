import { toast } from '../../components/toast.js';
import { api } from '../../core/api.js';
import { logout } from '../../core/auth.js';
import { $, escapeHtml } from '../../core/dom.js';
import { clearErrors, showApiError, withLoading } from '../../core/forms.js';
import { t, tError } from '../../core/i18n.js';
import { formatDate } from './profileCard.js';

/**
 * "Security" tab: 2FA switch (confirmed with the password), remembered devices, logout.
 * @param {object} user current user
 * @param {(user: object) => void} onChange called with the updated user after 2FA changes
 */
export async function initSecurityTab(user, onChange) {
  const toggle = $('#two-factor-toggle');
  const dialog = $('#two-factor-dialog');
  const dialogForm = $('#two-factor-form');
  let pendingEnabled = null;

  toggle.checked = user.twoFactorEnabled;

  toggle.addEventListener('click', (event) => {
    event.preventDefault(); // don't flip until the password is confirmed
    pendingEnabled = !user.twoFactorEnabled;
    dialog.querySelector('[data-dialog-title]').textContent =
      t(pendingEnabled ? 'account.confirmTitleOn' : 'account.confirmTitleOff');
    clearErrors(dialogForm);
    dialogForm.reset();
    dialog.showModal();
    dialogForm.password.focus();
  });

  dialog.querySelector('[data-action="cancel"]').addEventListener('click', () => dialog.close());

  dialogForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearErrors(dialogForm);
    await withLoading(dialogForm.querySelector('[type="submit"]'), async () => {
      try {
        user = await api.put('/api/users/me/two-factor', {
          enabled: pendingEnabled,
          password: dialogForm.password.value,
        });
        toggle.checked = user.twoFactorEnabled;
        dialog.close();
        onChange(user);
        await loadDevices();
        toast(t(user.twoFactorEnabled ? 'account.twoFactorEnabled' : 'account.twoFactorDisabled'), 'success');
      } catch (error) {
        showApiError(dialogForm, error, { codeToField: { INVALID_PASSWORD: 'password' } });
      }
    });
  });

  $('#device-list').addEventListener('click', async (event) => {
    const button = event.target.closest('[data-forget]');
    if (!button) return;
    await withLoading(button, async () => {
      try {
        await api.delete(`/api/users/me/devices/${button.dataset.forget}`);
        toast(t('account.deviceForgotten'), 'success');
        await loadDevices();
      } catch (error) {
        toast(tError(error.code) ?? error.message, 'error');
      }
    });
  });

  $('#logout-button').addEventListener('click', logout);

  await loadDevices();
}

async function loadDevices() {
  const list = $('#device-list');
  const devices = await api.get('/api/users/me/devices');
  if (devices.length === 0) {
    list.innerHTML = `<li class="empty">${escapeHtml(t('account.devicesEmpty'))}</li>`;
    return;
  }
  list.innerHTML = devices.map((d) => `
    <li class="device">
      <span class="device__icon" aria-hidden="true">${/Mobile|Android|iPhone/i.test(d.userAgent ?? '') ? '📱' : '💻'}</span>
      <div class="device__info">
        <div class="device__name">${escapeHtml(describeUserAgent(d.userAgent))}</div>
        <div class="device__meta">
          ${escapeHtml(d.lastUsedAt
            ? t('account.deviceLastUsed', { date: formatDate(d.lastUsedAt) })
            : t('account.deviceNeverUsed', { date: formatDate(d.createdAt) }))}
          · ${escapeHtml(t('account.deviceExpires', { date: formatDate(d.expiresAt) }))}
        </div>
      </div>
      <button type="button" class="btn btn--danger btn--sm" data-forget="${d.id}">${escapeHtml(t('account.forget'))}</button>
    </li>`).join('');
}

/** "Mozilla/5.0 (Macintosh...) Chrome/129..." → "Chrome on macOS". */
function describeUserAgent(ua = '') {
  const browser = /Edg\//.test(ua) ? 'Edge'
    : /Chrome\//.test(ua) ? 'Chrome'
      : /Firefox\//.test(ua) ? 'Firefox'
        : /Safari\//.test(ua) ? 'Safari' : 'Browser';
  const os = /iPhone|iPad/.test(ua) ? 'iOS'
    : /Android/.test(ua) ? 'Android'
      : /Mac OS X/.test(ua) ? 'macOS'
        : /Windows/.test(ua) ? 'Windows'
          : /Linux/.test(ua) ? 'Linux' : 'unknown OS';
  return `${browser} on ${os}`;
}
