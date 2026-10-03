import { avatarHtml } from '../components/avatar.js';
import { initComments } from '../components/comments.js';
import { initPage } from '../components/layout.js';
import { initSocialStrip } from '../components/socialStrip.js';
import { COURSE_ICONS, dietMarkHtml } from '../components/recipeCard.js';
import { toast } from '../components/toast.js';
import { api } from '../core/api.js';
import { editRecipeUrl, profileUrl } from '../core/config.js';
import { $, displayName, escapeHtml } from '../core/dom.js';
import { formatCost, formatCount, formatDate, formatMinutes, starsHtml } from '../core/format.js';
import { t, tError } from '../core/i18n.js';

/** Recipe page: /pages/recipe.html?id=… */

const me = await initPage();
const id = new URLSearchParams(location.search).get('id');

let recipe = null;
try {
  recipe = id ? await api.get(`/api/recipes/${encodeURIComponent(id)}`) : null;
} catch {
  recipe = null;
}

if (!recipe) {
  $('[data-not-found]').hidden = false;
} else {
  render(recipe);
}

function render(r) {
  document.title = `${r.title} · Cookify`;
  const el = $('#recipe');
  const fact = (label, value) => `<div class="fact"><dt>${escapeHtml(label)}</dt><dd>${value}</dd></div>`;

  el.innerHTML = `
    <section class="recipe-hero">
      <div class="recipe-hero__head">
        <div>
          <div class="recipe-hero__meta">
            ${dietMarkHtml(r.diet)}
            <span class="badge">${escapeHtml(t(`cuisine.${r.cuisine}`))}</span>
            <span class="badge">${escapeHtml(t(`course.${r.course}`))}</span>
          </div>
          <h1 class="recipe-hero__title">${escapeHtml(r.title)}</h1>
          <div class="recipe-hero__meta">
            <a class="recipe-hero__author" href="${profileUrl(r.author.username)}">
              ${avatarHtml(r.author)}<span>${escapeHtml(displayName(r.author))}</span>
            </a>
            <span>📅 ${escapeHtml(formatDate(r.createdAt))}</span>
            <span>👁 ${escapeHtml(t('recipe.views', { count: formatCount(r.views) }))}</span>
          </div>
        </div>
        ${r.canEdit ? `
          <div class="recipe-hero__actions">
            <a class="btn btn--sm" href="${editRecipeUrl(r.id)}">✏️ ${escapeHtml(t('recipe.edit'))}</a>
            <button type="button" class="btn btn--danger btn--sm" data-action="delete" style="background:var(--white)">🗑 ${escapeHtml(t('recipe.delete'))}</button>
          </div>` : ''}
      </div>

      <div class="gallery" id="gallery"></div>

      <div class="steps-panel">
        <h2>${escapeHtml(t('recipe.method'))}</h2>
        <ol class="steps">${r.steps.map((s) => `<li><span>${escapeHtml(s)}</span></li>`).join('')}</ol>
      </div>

      <div class="social-strip" id="social-strip" style="grid-column:1/-1"></div>
    </section>

    <dl class="facts">
      ${fact(t('recipe.totalTime'), escapeHtml(formatMinutes(r.totalMinutes)))}
      ${fact(t('recipe.prepCook'), `${r.prepMinutes} + ${r.cookMinutes} min`)}
      ${fact(t('recipe.speed'), starsHtml(r.speed))}
      ${fact(t('recipe.difficulty'), starsHtml(r.difficulty, { label: t(`difficulty.${r.difficulty}`) }))}
      ${fact(t('recipe.cost'), escapeHtml(formatCost(r.cost)))}
      ${fact(t('recipe.servings'), r.servings)}
      ${fact(t('recipe.calories'), `${r.calories} kcal`)}
      ${fact(t('recipe.protein'), r.protein != null ? `${r.protein} g` : '—')}
    </dl>

    <div class="recipe-body">
      <section class="card">
        <h2>${escapeHtml(t('recipe.ingredients'))}</h2>
        <p class="text-muted" style="font-size:.85rem">${escapeHtml(t('recipe.ingredientsFor', { count: r.servings }))}</p>
        <ul class="ingredient-list">
          ${r.ingredients.map((i) => `
            <li><label><input type="checkbox"><span class="name">${escapeHtml(i.name)}</span>
            <span class="qty">${escapeHtml(i.quantity ?? '')}</span></label></li>`).join('')}
        </ul>
        ${r.equipment.length ? `
          <h3 style="margin-top:24px">${escapeHtml(t('recipe.equipment'))}</h3>
          <div class="equipment-chips">${r.equipment.map((e) => `<span>${escapeHtml(e)}</span>`).join('')}</div>` : ''}
      </section>
      <section class="card">
        <h2>${escapeHtml(t('recipe.about'))}</h2>
        <p class="description">${escapeHtml(r.description ?? t('recipe.noDescription'))}</p>
      </section>
    </div>

    <section class="card comments" id="comments"></section>`;

  renderGallery(r);
  const strip = initSocialStrip($('#social-strip'), r, me);
  initComments($('#comments'), r.id, me, (delta) => strip.adjustComments(delta));
  if (location.hash === '#comments') $('#comments').scrollIntoView();
  el.hidden = false;
  el.removeAttribute('aria-busy');
  el.querySelector('[data-action="delete"]')?.addEventListener('click', confirmDelete);
}

function renderGallery(r) {
  const gallery = $('#gallery');
  if (!r.media.length) {
    gallery.innerHTML = `<div class="gallery__main"><span class="gallery__placeholder" aria-hidden="true">${COURSE_ICONS[r.course] ?? r.diet.icon}</span></div>`;
    return;
  }
  const show = (index) => {
    const m = r.media[index];
    gallery.querySelector('.gallery__main').innerHTML = m.type === 'VIDEO'
      ? `<video src="${escapeHtml(m.url)}" controls playsinline preload="metadata"></video>`
      : `<img src="${escapeHtml(m.url)}" alt="${escapeHtml(r.title)}">`;
    gallery.querySelectorAll('.gallery__thumb').forEach((b, i) => b.setAttribute('aria-current', String(i === index)));
  };
  gallery.innerHTML = `
    <div class="gallery__main"></div>
    ${r.media.length > 1 ? `<div class="gallery__thumbs">${r.media.map((m, i) => `
      <button type="button" class="gallery__thumb" data-index="${i}" aria-label="${escapeHtml(t('recipe.showMedia', { n: i + 1 }))}">
        ${m.type === 'VIDEO'
          ? `<video src="${escapeHtml(m.url)}" muted preload="metadata"></video><span class="gallery__thumb-play">▶</span>`
          : `<img src="${escapeHtml(m.url)}" alt="" loading="lazy">`}
      </button>`).join('')}</div>` : ''}`;
  gallery.addEventListener('click', (e) => {
    const thumb = e.target.closest('.gallery__thumb');
    if (thumb) show(Number(thumb.dataset.index));
  });
  show(0);
}

function confirmDelete() {
  const dialog = $('#delete-dialog');
  dialog.showModal();
  dialog.querySelector('[data-action="cancel"]').onclick = () => dialog.close();
  dialog.querySelector('[data-action="confirm"]').onclick = async (e) => {
    e.target.disabled = true;
    try {
      await api.delete(`/api/recipes/${recipe.id}`);
      location.href = profileUrl(recipe.author.username);
    } catch (error) {
      dialog.close();
      e.target.disabled = false;
      toast(tError(error.code) ?? error.message, 'error');
    }
  };
}

