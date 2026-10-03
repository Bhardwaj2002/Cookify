import { avatarHtml } from '../components/avatar.js';
import { initPage } from '../components/layout.js';
import { toast } from '../components/toast.js';
import { api } from '../core/api.js';
import { ROUTES, profileUrl } from '../core/config.js';
import { $, $$, displayName, escapeHtml } from '../core/dom.js';
import { clearErrors, showApiError, withLoading } from '../core/forms.js';
import { formatDate, timeAgo } from '../core/format.js';
import { t, tError } from '../core/i18n.js';

/** Moderation dashboard (admins only): find users, review warnings, ban / unban. */

const me = await initPage({ access: 'user' });
if (me.role !== 'ADMIN') location.replace(ROUTES.home);

const MAX_WARNINGS = 3;
const state = { status: 'ALL', q: '', page: 0 };
let users = [];

// ---- List ---------------------------------------------------------------------------

async function load() {
  const params = new URLSearchParams({ status: state.status, page: state.page, size: 20 });
  if (state.q) params.set('q', state.q);
  try {
    const data = await api.get(`/api/admin/users?${params}`);
    users = data.content;
    render(data);
    $('#admin-error').hidden = true;
  } catch (error) {
    $('#admin-error').textContent = tError(error.code) ?? error.message;
    $('#admin-error').hidden = false;
  }
}

function statusHtml(u) {
  if (u.role === 'ADMIN') return `<span class="status status--admin">🛡 ${escapeHtml(t('admin.roleAdmin'))}</span>`;
  if (!u.banned) return `<span class="status status--active">● ${escapeHtml(t('admin.active'))}</span>`;
  const label = u.permanentlyBanned ? t('admin.bannedForever') : t('admin.bannedUntil', { date: formatDate(u.bannedUntil) });
  return `<span class="status status--banned" title="${escapeHtml(u.banReason ?? '')}">⛔ ${escapeHtml(label)}</span>`;
}

function render(data) {
  $$('[data-status]').forEach((tab) => tab.setAttribute('aria-selected', String(tab.dataset.status === state.status)));
  const body = $('#user-table tbody');
  if (!users.length) {
    body.innerHTML = `<tr><td colspan="5" class="empty">${escapeHtml(t('admin.none'))}</td></tr>`;
  } else {
    body.innerHTML = users.map((u, i) => `
      <tr>
        <td><a class="user-cell" href="${profileUrl(u.username)}">${avatarHtml(u)}
          <span><span class="user-cell__name">${escapeHtml(displayName(u))}</span>
          <span class="user-cell__meta">@${escapeHtml(u.username)} · ${escapeHtml(u.email)}</span></span></a></td>
        <td><span class="strikes" title="${u.warningsCount} / ${MAX_WARNINGS}">
          ${Array.from({ length: MAX_WARNINGS }, (_, n) => `<span class="strike ${n < u.warningsCount ? 'is-on' : ''}"></span>`).join('')}
          </span> <span class="text-muted" style="font-size:.8rem">${u.warningsCount}</span></td>
        <td>${statusHtml(u)}</td>
        <td class="text-muted" style="font-size:.85rem">${escapeHtml(timeAgo(u.createdAt))}</td>
        <td>
          <button type="button" class="btn btn--ghost btn--sm" data-warnings="${i}" ${u.warningsCount ? '' : 'disabled'}>${escapeHtml(t('admin.viewWarnings'))}</button>
          ${u.role === 'ADMIN' ? '' : u.banned
            ? `<button type="button" class="btn btn--sm" data-unban="${i}">${escapeHtml(t('admin.unban'))}</button>`
            : `<button type="button" class="btn btn--rust btn--sm" data-ban="${i}">${escapeHtml(t('admin.ban'))}</button>`}
        </td>
      </tr>`).join('');
  }
  $('#pager').innerHTML = data.totalPages > 1 ? `
    <button type="button" class="btn btn--ghost btn--sm" data-page="-1" ${data.page === 0 ? 'disabled' : ''}>‹</button>
    <span>${data.page + 1} / ${data.totalPages}</span>
    <button type="button" class="btn btn--ghost btn--sm" data-page="1" ${data.page + 1 >= data.totalPages ? 'disabled' : ''}>›</button>` : '';
}

$$('[data-status]').forEach((tab) => tab.addEventListener('click', () => {
  Object.assign(state, { status: tab.dataset.status, page: 0 });
  load();
}));

let searchTimer;
$('#search').addEventListener('input', (e) => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    Object.assign(state, { q: e.target.value.trim(), page: 0 });
    load();
  }, 300);
});

$('#pager').addEventListener('click', (e) => {
  const button = e.target.closest('[data-page]');
  if (!button) return;
  state.page += Number(button.dataset.page);
  load();
});

// ---- Actions ----------------------------------------------------------------------

const warningsDialog = $('#warnings-dialog');
const banDialog = $('#ban-dialog');
const banForm = $('#ban-form');
let banTarget = null;

$('#user-table').addEventListener('click', async (e) => {
  const warnings = e.target.closest('[data-warnings]');
  const ban = e.target.closest('[data-ban]');
  const unban = e.target.closest('[data-unban]');

  if (warnings) {
    const user = users[warnings.dataset.warnings];
    const list = await api.get(`/api/admin/users/${user.id}/warnings`);
    warningsDialog.querySelector('[data-dialog-title]').textContent = t('admin.warningsFor', { name: user.username });
    $('#warning-list').innerHTML = list.map((w) => `
      <li><strong>${escapeHtml(t(`admin.reason_${w.reason}`))}</strong>
      <span class="text-muted" style="font-size:.8rem"> · ${escapeHtml(timeAgo(w.createdAt))}</span>
      ${w.contentSnippet ? `<q>${escapeHtml(w.contentSnippet)}</q>` : ''}</li>`).join('') ||
      `<li class="empty">${escapeHtml(t('admin.noWarnings'))}</li>`;
    warningsDialog.showModal();
  } else if (ban) {
    banTarget = users[ban.dataset.ban];
    banDialog.querySelector('[data-dialog-title]').textContent = t('admin.banTitle', { name: banTarget.username });
    clearErrors(banForm);
    banForm.reset();
    banDialog.showModal();
    banForm.reason.focus();
  } else if (unban) {
    const user = users[unban.dataset.unban];
    if (!confirm(t('admin.unbanConfirm', { name: user.username }))) return;
    await withLoading(unban, async () => {
      try {
        await api.post(`/api/admin/users/${user.id}/unban`);
        toast(t('admin.unbanned', { name: user.username }), 'success');
        await load();
      } catch (error) {
        toast(tError(error.code) ?? error.message, 'error');
      }
    });
  }
});

warningsDialog.querySelector('[data-action="close"]').addEventListener('click', () => warningsDialog.close());
banDialog.querySelector('[data-action="cancel"]').addEventListener('click', () => banDialog.close());

banForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  clearErrors(banForm);
  const duration = banForm.duration.value;
  const body = {
    permanent: duration === 'permanent',
    days: duration === 'permanent' ? null : Number(duration),
    reason: banForm.reason.value.trim(),
  };
  await withLoading(banForm.querySelector('[type="submit"]'), async () => {
    try {
      await api.post(`/api/admin/users/${banTarget.id}/ban`, body);
      banDialog.close();
      toast(t('admin.bannedToast', { name: banTarget.username }), 'success');
      await load();
    } catch (error) {
      showApiError(banForm, error);
    }
  });
});

await load();
