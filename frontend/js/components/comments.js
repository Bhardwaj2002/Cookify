import { api } from '../core/api.js';
import { ROUTES, profileUrl } from '../core/config.js';
import { displayName, escapeHtml } from '../core/dom.js';
import { timeAgo } from '../core/format.js';
import { t, tError } from '../core/i18n.js';
import { avatarHtml } from './avatar.js';
import { toast } from './toast.js';

const PAGE_SIZE = 10;
const MAX_LENGTH = 1000;

/**
 * Comment thread for a recipe: form (or login prompt), newest-first list, delete, load more.
 * @param {HTMLElement} container
 * @param {number} recipeId
 * @param {object|null} me current user
 * @param {(delta: number) => void} onCountChange called with +1 / -1
 */
export function initComments(container, recipeId, me, onCountChange) {
  const next = encodeURIComponent(location.pathname + location.search + '#comments');
  container.innerHTML = `
    <h2>💬 ${escapeHtml(t('comments.title'))}</h2>
    ${me ? `
      <form class="comment-form" novalidate>
        <textarea class="input" name="body" maxlength="${MAX_LENGTH}" required
                  placeholder="${escapeHtml(t('comments.placeholder'))}" aria-label="${escapeHtml(t('comments.placeholder'))}"></textarea>
        <div class="comment-form__foot">
          <span class="comment-form__count">0 / ${MAX_LENGTH}</span>
          <button class="btn btn--rust btn--sm" type="submit">${escapeHtml(t('comments.post'))}</button>
        </div>
        <div class="alert alert--error" data-form-alert hidden></div>
      </form>` : `
      <p class="login-prompt"><a href="${ROUTES.login}?next=${next}">${escapeHtml(t('nav.login'))}</a> ${escapeHtml(t('comments.loginToComment'))}</p>`}
    <ul class="comment-list" aria-live="polite"></ul>
    <button type="button" class="btn btn--ghost btn--sm" data-more hidden>${escapeHtml(t('browse.loadMore'))}</button>`;

  const list = container.querySelector('.comment-list');
  const more = container.querySelector('[data-more]');
  let page = 0;

  const itemHtml = (c) => `
    <li class="comment" data-id="${c.id}">
      <a href="${profileUrl(c.author.username)}" aria-hidden="true" tabindex="-1">${avatarHtml(c.author)}</a>
      <div>
        <div class="comment__head">
          <a class="comment__name" href="${profileUrl(c.author.username)}">${escapeHtml(displayName(c.author))}</a>
          <time class="comment__time" datetime="${escapeHtml(c.createdAt)}">${escapeHtml(timeAgo(c.createdAt))}</time>
        </div>
        <p class="comment__body">${escapeHtml(c.body)}</p>
      </div>
      ${c.canDelete ? `<button type="button" class="icon-btn icon-btn--danger" data-delete aria-label="${escapeHtml(t('comments.delete'))}">🗑</button>` : '<span></span>'}
    </li>`;

  async function load() {
    const data = await api.get(`/api/recipes/${recipeId}/comments?page=${page}&size=${PAGE_SIZE}`);
    if (page === 0 && !data.content.length) {
      list.innerHTML = `<li class="empty">${escapeHtml(t('comments.empty'))}</li>`;
    } else {
      list.querySelector('.empty')?.remove();
      list.insertAdjacentHTML('beforeend', data.content.map(itemHtml).join(''));
    }
    more.hidden = data.page + 1 >= data.totalPages;
  }

  more.addEventListener('click', () => {
    page += 1;
    load();
  });

  list.addEventListener('click', async (e) => {
    const button = e.target.closest('[data-delete]');
    if (!button || !confirm(t('comments.confirmDelete'))) return;
    const item = button.closest('.comment');
    try {
      await api.delete(`/api/comments/${item.dataset.id}`);
      item.remove();
      onCountChange(-1);
      if (!list.children.length) list.innerHTML = `<li class="empty">${escapeHtml(t('comments.empty'))}</li>`;
    } catch (error) {
      toast(tError(error.code) ?? error.message, 'error');
    }
  });

  const form = container.querySelector('.comment-form');
  if (form) {
    const counter = form.querySelector('.comment-form__count');
    form.body.addEventListener('input', () => {
      counter.textContent = `${form.body.value.length} / ${MAX_LENGTH}`;
      form.querySelector('[data-form-alert]').hidden = true;
    });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const body = form.body.value.trim();
      if (!body) return;
      const button = form.querySelector('[type="submit"]');
      button.disabled = true;
      try {
        const created = await api.post(`/api/recipes/${recipeId}/comments`, { body });
        list.querySelector('.empty')?.remove();
        list.insertAdjacentHTML('afterbegin', itemHtml(created));
        form.reset();
        counter.textContent = `0 / ${MAX_LENGTH}`;
        onCountChange(1);
      } catch (error) {
        const alert = form.querySelector('[data-form-alert]');
        alert.textContent = tError(error.code) ?? error.message;
        alert.hidden = false;
      } finally {
        button.disabled = false;
      }
    });
  }

  load();
}
