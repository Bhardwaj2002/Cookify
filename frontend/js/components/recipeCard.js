import { profileUrl, recipeUrl } from '../core/config.js';
import { displayName, escapeHtml } from '../core/dom.js';
import { formatCost, formatCount, formatMinutes } from '../core/format.js';
import { t } from '../core/i18n.js';
import { avatarHtml } from './avatar.js';

/** Placeholder art for recipes without photos, by course. */
export const COURSE_ICONS = {
  BREAKFAST: '🥞', APPETIZER: '🥟', SOUP: '🍲', SALAD: '🥗', MAIN: '🍛', SIDE: '🥘',
  BREAD: '🫓', RICE: '🍚', SNACK: '🍿', DESSERT: '🍰', BEVERAGE: '🥤',
};

/**
 * A clickable recipe card (cover, diet mark, title, author, quick facts).
 * @param {object} r recipe summary
 * @param {{ reason?: string }} [options] optional "why you're seeing this" line (recommendations)
 */
export function recipeCardHtml(r, { reason } = {}) {
  const cover = r.coverUrl
    ? `<img src="${escapeHtml(r.coverUrl)}" alt="" loading="lazy">`
    : `<span class="recipe-card__placeholder" aria-hidden="true">${COURSE_ICONS[r.course] ?? r.diet.icon}</span>`;
  const rating = r.ratingCount
    ? `⭐ ${Number(r.avgRating).toFixed(1)} <span class="text-muted">(${formatCount(r.ratingCount)})</span>`
    : `<span class="text-muted">${escapeHtml(t('recipe.noRatings'))}</span>`;

  return `
    <article class="recipe-card">
      <a class="recipe-card__cover" href="${recipeUrl(r.id)}" tabindex="-1" aria-hidden="true">${cover}</a>
      <div class="recipe-card__body">
        <div class="recipe-card__tags">
          ${dietMarkHtml(r.diet)}
          <span class="badge">${escapeHtml(t(`course.${r.course}`))}</span>
        </div>
        <h3 class="recipe-card__title"><a href="${recipeUrl(r.id)}">${escapeHtml(r.title)}</a></h3>
        <a class="recipe-card__author" href="${profileUrl(r.author.username)}">
          ${avatarHtml(r.author, 'avatar--xs')}<span>${escapeHtml(displayName(r.author))}</span>
        </a>
        ${reason ? `<p class="recipe-card__reason">✨ ${escapeHtml(reason)}</p>` : ''}
        <ul class="recipe-card__facts">
          <li title="${escapeHtml(t('recipe.totalTime'))}">⏱ ${escapeHtml(formatMinutes(r.totalMinutes))}</li>
          <li title="${escapeHtml(t('recipe.cost'))}">${escapeHtml(formatCost(r.cost))}</li>
          <li title="${escapeHtml(t('recipe.calories'))}">🔥 ${r.calories} kcal</li>
        </ul>
        <div class="recipe-card__footer">
          <span>${rating}</span>
          <span class="text-muted">💬 ${formatCount(r.commentCount ?? 0)} · 👁 ${formatCount(r.views)}</span>
        </div>
      </div>
    </article>`;
}

/** Indian-style veg/non-veg square mark with the diet's colour and label. */
export function dietMarkHtml(diet) {
  return `<span class="diet-mark" style="--diet:${escapeHtml(diet.color)}" title="${escapeHtml(t(`diet.${diet.type}`))}">
    <span class="diet-mark__box" aria-hidden="true"></span>${escapeHtml(t(`diet.${diet.type}`))}</span>`;
}

/** Renders a grid of cards, or an empty-state message. */
export function renderRecipeGrid(container, recipes, emptyMessage) {
  container.innerHTML = recipes.length
    ? `<div class="recipe-grid">${recipes.map(recipeCardHtml).join('')}</div>`
    : `<div class="empty-state"><span class="empty-state__icon" aria-hidden="true">🍳</span><p>${escapeHtml(emptyMessage)}</p></div>`;
}
