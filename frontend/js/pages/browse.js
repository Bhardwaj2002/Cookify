import { initPage } from '../components/layout.js';
import { recipeCardHtml } from '../components/recipeCard.js';
import { api } from '../core/api.js';
import { $, escapeHtml } from '../core/dom.js';
import { formatCost } from '../core/format.js';
import { t } from '../core/i18n.js';
import { COURSES, CUISINES, DIETS } from '../core/recipeOptions.js';

/**
 * Browse & search. All filter state lives in the URL query string (same names as the API), so
 * results are shareable and the back button works. Changes re-query automatically (debounced).
 */

await initPage();

const PAGE_SIZE = 12;
/** Speed stars (mockup) → maximum total minutes; matches Recipe.getSpeed() on the server. */
const SPEED_TO_MINUTES = { 5: 15, 4: 30, 3: 60, 2: 120 };
const RATING_OPTIONS = ['', '3', '4', '4.5'];
const LIST_KEYS = ['cuisine', 'course', 'ingredients', 'equipment'];

const state = readUrl();
let page = 0;
let ranges = { maxCost: 1000, maxCalories: 1000, maxProtein: 60 };
let inflight = null;

// ---- Build controls ------------------------------------------------------------------

const options = await api.get('/api/meta/filters', { auth: false }).catch(() => null);
if (options) {
  ranges = {
    maxCost: Math.max(100, Math.ceil(Number(options.ranges.maxCost) / 50) * 50),
    maxCalories: Math.max(200, Math.ceil(options.ranges.maxCalories / 50) * 50),
    maxProtein: Math.max(20, Math.ceil(options.ranges.maxProtein / 5) * 5),
  };
}

$('#diet').innerHTML = `<option value="">${escapeHtml(t('browse.anyDiet'))}</option>` +
  DIETS.map((d) => `<option value="${d.value}">${d.icon} ${escapeHtml(t(`dietPref.${d.value}`))}</option>`).join('');

$('#ingredient-options').innerHTML = (options?.ingredients ?? [])
  .map((name) => `<option value="${escapeHtml(name)}"></option>`).join('');

const chipGroup = (container, name, values, labelFn, type = 'checkbox') => {
  container.innerHTML = values.map((v) => `
    <label class="chip"><input type="${type}" name="${name}" value="${escapeHtml(v)}"><span>${escapeHtml(labelFn(v))}</span></label>`).join('');
};
chipGroup($('#equipment-chips'), 'equipment', (options?.equipment ?? []).slice(0, 14), (v) => v);
chipGroup($('#course-chips'), 'course', COURSES, (v) => t(`course.${v}`));
chipGroup($('#cuisine-chips'), 'cuisine', CUISINES, (v) => t(`cuisine.${v}`));
chipGroup($('#rating-chips'), 'minRating', RATING_OPTIONS, (v) => (v ? `⭐ ${v}+` : t('browse.any')), 'radio');

function starPicker(container, name) {
  container.innerHTML = [1, 2, 3, 4, 5].map((n) => `
    <label><input type="radio" name="${name}" value="${n}"><span aria-hidden="true">★</span>
    <span class="visually-hidden">${n}</span></label>`).join('');
}
starPicker($('#speed-picker'), 'speed');
starPicker($('#difficulty-picker'), 'difficulty');

for (const [id, max] of [['maxCost', ranges.maxCost], ['maxCalories', ranges.maxCalories], ['minProtein', ranges.maxProtein]]) {
  $(`#${id}`).max = String(max);
}

// ---- State ↔ controls -------------------------------------------------------------------

function readUrl() {
  const p = new URLSearchParams(location.search);
  const s = { sort: p.get('sort') || 'POPULAR', ingredientMatch: p.get('ingredientMatch') || 'ALL' };
  for (const key of ['q', 'diet', 'maxCost', 'maxMinutes', 'maxCalories', 'minProtein', 'maxDifficulty', 'minRating']) {
    if (p.get(key)) s[key] = p.get(key);
  }
  for (const key of LIST_KEYS) {
    s[key] = p.getAll(key).flatMap((v) => v.split(',')).map((v) => v.trim()).filter(Boolean);
  }
  return s;
}

function toQuery(extra = {}) {
  const p = new URLSearchParams();
  for (const [key, value] of Object.entries({ ...state, ...extra })) {
    if (Array.isArray(value)) {
      if (value.length) p.set(key, value.join(','));
    } else if (value !== undefined && value !== null && value !== '') {
      p.set(key, value);
    }
  }
  if (!state.ingredients.length) p.delete('ingredientMatch');
  else if (state.ingredientMatch === 'ALL') p.delete('ingredientMatch');
  if (state.sort === 'POPULAR' && !('page' in extra)) p.delete('sort');
  return p;
}

function syncControls() {
  $('#q').value = state.q ?? '';
  $('#clear-search').hidden = !state.q;
  $('#sort').value = state.sort;
  $('#diet').value = state.diet ?? '';
  $('#veg-only').checked = ['VEG', 'VEGAN', 'JAIN'].includes(state.diet);

  const speed = Object.entries(SPEED_TO_MINUTES).find(([, m]) => String(m) === state.maxMinutes)?.[0];
  paintStars('speed', speed ? Number(speed) : 0);
  paintStars('difficulty', state.maxDifficulty ? Number(state.maxDifficulty) : 0);

  setRange('maxCost', state.maxCost ?? ranges.maxCost);
  setRange('maxCalories', state.maxCalories ?? ranges.maxCalories);
  setRange('minProtein', state.minProtein ?? 0);

  document.querySelectorAll('#filters input[type=checkbox][name], #filters input[type=radio][name=minRating]').forEach((input) => {
    input.checked = input.name === 'minRating'
      ? (state.minRating ?? '') === input.value
      : (state[input.name] ?? []).includes(input.value);
  });
  document.querySelector(`input[name=ingredientMatch][value=${state.ingredientMatch}]`).checked = true;
  renderIngredientTags();
}

function paintStars(name, value) {
  const picker = $(`#${name}-picker`);
  picker.querySelectorAll('label').forEach((label, i) => label.classList.toggle('is-on', i < value));
  picker.querySelectorAll('input').forEach((input) => (input.checked = Number(input.value) === value));
  const label = name === 'speed'
    ? (value >= 2 ? t('browse.speedValue', { minutes: SPEED_TO_MINUTES[value] }) : t('browse.any'))
    : (value ? t(`difficulty.${value}`) + (value < 5 ? ' ' + t('browse.orEasier') : '') : t('browse.any'));
  $(`[data-${name}-value]`).textContent = label;
}

function setRange(id, value) {
  const input = $(`#${id}`);
  input.value = value;
  const percent = ((input.value - input.min) / (input.max - input.min)) * 100;
  input.style.setProperty('--fill', `${percent}%`);
  const isAny = id === 'minProtein' ? Number(input.value) === 0 : Number(input.value) >= Number(input.max);
  const text = { maxCost: () => `≤ ${formatCost(input.value)}`, maxCalories: () => `≤ ${input.value} kcal`, minProtein: () => `≥ ${input.value} g` }[id]();
  document.querySelector(`[data-range-value="${id}"]`).textContent = isAny ? t('browse.any') : text;
  return isAny ? undefined : input.value;
}

// ---- Events -------------------------------------------------------------------------------

let debounceTimer;
function changed({ immediate = false } = {}) {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    history.replaceState(null, '', `${location.pathname}?${toQuery()}`.replace(/\?$/, ''));
    syncControls();
    search();
  }, immediate ? 0 : 300);
}

$('#search-form').addEventListener('submit', (e) => {
  e.preventDefault();
  state.q = $('#q').value.trim() || undefined;
  changed({ immediate: true });
});
$('#q').addEventListener('input', () => {
  state.q = $('#q').value.trim() || undefined;
  $('#clear-search').hidden = !state.q;
  changed();
});
$('#clear-search').addEventListener('click', () => {
  state.q = undefined;
  changed({ immediate: true });
  $('#q').focus();
});
$('#sort').addEventListener('change', () => {
  state.sort = $('#sort').value;
  changed({ immediate: true });
});

$('#veg-only').addEventListener('change', (e) => {
  state.diet = e.target.checked ? 'VEG' : undefined;
  changed({ immediate: true });
});
$('#diet').addEventListener('change', (e) => {
  state.diet = e.target.value || undefined;
  changed({ immediate: true });
});

for (const name of ['speed', 'difficulty']) {
  const picker = $(`#${name}-picker`);
  picker.addEventListener('click', (e) => {
    const input = e.target.closest('label')?.querySelector('input');
    if (!input) return;
    e.preventDefault();
    const value = Number(input.value);
    const key = name === 'speed' ? 'maxMinutes' : 'maxDifficulty';
    const current = name === 'speed'
      ? Number(Object.entries(SPEED_TO_MINUTES).find(([, m]) => String(m) === state.maxMinutes)?.[0] ?? 0)
      : Number(state.maxDifficulty ?? 0);
    // Clicking the selected star again clears it; 1 speed star means "any time".
    const next = value === current ? 0 : value;
    state[key] = name === 'speed' ? (next >= 2 ? String(SPEED_TO_MINUTES[next]) : undefined) : (next ? String(next) : undefined);
    changed({ immediate: true });
  });
}

for (const id of ['maxCost', 'maxCalories', 'minProtein']) {
  $(`#${id}`).addEventListener('input', (e) => {
    state[id] = setRange(id, e.target.value);
    changed();
  });
}

$('#filters').addEventListener('change', (e) => {
  const input = e.target;
  if (input.name === 'minRating') {
    state.minRating = input.value || undefined;
  } else if (input.name === 'ingredientMatch') {
    state.ingredientMatch = input.value;
  } else if (input.type === 'checkbox' && LIST_KEYS.includes(input.name)) {
    state[input.name] = [...document.querySelectorAll(`#filters input[name=${input.name}]:checked`)].map((i) => i.value);
  } else {
    return;
  }
  changed({ immediate: true });
});

// Ingredient tags ("what's in your kitchen")
const tagBox = $('#ingredient-tags');
const tagInput = tagBox.querySelector('input');

function renderIngredientTags() {
  tagBox.querySelectorAll('.tag').forEach((tag) => tag.remove());
  for (const name of state.ingredients) {
    const tag = document.createElement('span');
    tag.className = 'tag';
    tag.innerHTML = `${escapeHtml(name)}<button type="button" aria-label="Remove ${escapeHtml(name)}">✕</button>`;
    tag.querySelector('button').addEventListener('click', () => {
      state.ingredients = state.ingredients.filter((i) => i !== name);
      changed({ immediate: true });
    });
    tagBox.insertBefore(tag, tagInput);
  }
  $('[data-match-hint]').textContent = state.ingredients.length
    ? t(state.ingredientMatch === 'ONLY' ? 'browse.matchOnlyHint' : 'browse.matchAllHint') : t('browse.kitchenHint');
}

function addIngredient() {
  const name = tagInput.value.trim().toLowerCase();
  tagInput.value = '';
  if (name && !state.ingredients.includes(name) && state.ingredients.length < 20) {
    state.ingredients = [...state.ingredients, name];
    changed({ immediate: true });
  }
}
tagInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault();
    addIngredient();
  } else if (e.key === 'Backspace' && !tagInput.value && state.ingredients.length) {
    state.ingredients = state.ingredients.slice(0, -1);
    changed({ immediate: true });
  }
});
// Picking a suggestion from the datalist fires "input" with the full value.
tagInput.addEventListener('input', (e) => {
  if (e.inputType === 'insertReplacementText' || (options?.ingredients ?? []).includes(tagInput.value)) addIngredient();
});
tagBox.addEventListener('click', (e) => e.target === tagBox && tagInput.focus());

$('#reset-filters').addEventListener('click', resetFilters);

function resetFilters() {
  for (const key of Object.keys(state)) delete state[key];
  Object.assign(state, { sort: 'POPULAR', ingredientMatch: 'ALL' });
  LIST_KEYS.forEach((key) => (state[key] = []));
  changed({ immediate: true });
}

// Mobile drawer
const filters = $('#filters');
let backdrop = null;
const closeDrawer = () => {
  filters.classList.remove('is-open');
  backdrop?.remove();
  backdrop = null;
};
$('#open-filters').addEventListener('click', () => {
  filters.classList.add('is-open');
  backdrop = document.createElement('div');
  backdrop.className = 'filters-backdrop';
  backdrop.addEventListener('click', closeDrawer);
  document.body.append(backdrop);
});
$('#close-filters').addEventListener('click', closeDrawer);

// ---- Search & results -------------------------------------------------------------------

async function search({ append = false } = {}) {
  page = append ? page + 1 : 0;
  inflight?.abort();
  const controller = new AbortController();
  inflight = controller;

  const results = $('#results');
  results.setAttribute('aria-busy', 'true');
  if (!append && !$('#results-grid').children.length) {
    $('#results-grid').innerHTML = `<div class="recipe-grid">${'<div class="skeleton"></div>'.repeat(6)}</div>`;
  }

  try {
    const query = toQuery({ page, size: PAGE_SIZE });
    const data = await api.get(`/api/recipes?${query}`, { auth: false });
    if (controller.signal.aborted) return;
    renderResults(data, append);
  } catch (error) {
    if (!controller.signal.aborted) {
      $('#results-grid').innerHTML = `<div class="alert alert--error">${escapeHtml(error.message)}</div>`;
    }
  } finally {
    if (inflight === controller) results.removeAttribute('aria-busy');
  }
}

function renderResults(data, append) {
  const grid = $('#results-grid');
  if (!append) {
    grid.innerHTML = data.content.length
      ? `<div class="recipe-grid">${data.content.map(recipeCardHtml).join('')}</div>`
      : `<div class="empty-state"><span class="empty-state__icon" aria-hidden="true">🥄</span>
           <p>${escapeHtml(t('browse.noResults'))}</p>
           <button type="button" class="btn btn--sm" data-reset>${escapeHtml(t('browse.reset'))}</button></div>`;
    grid.querySelector('[data-reset]')?.addEventListener('click', resetFilters);
  } else {
    grid.querySelector('.recipe-grid').insertAdjacentHTML('beforeend', data.content.map(recipeCardHtml).join(''));
  }
  const more = $('#load-more');
  more.hidden = data.page + 1 >= data.totalPages;
  renderBar(data.totalElements);
}

/** Result count plus a removable pill for each active filter. */
function renderBar(total) {
  const pills = [];
  const pill = (label, remove) => pills.push({ label, remove });
  if (state.q) pill(`“${state.q}”`, () => (state.q = undefined));
  if (state.diet) pill(t(`dietPref.${state.diet}`), () => (state.diet = undefined));
  if (state.maxMinutes) pill(`⏱ ≤ ${state.maxMinutes} min`, () => (state.maxMinutes = undefined));
  if (state.maxDifficulty) pill(`${t('browse.difficulty')} ≤ ${state.maxDifficulty}★`, () => (state.maxDifficulty = undefined));
  if (state.maxCost) pill(`≤ ${formatCost(state.maxCost)}`, () => (state.maxCost = undefined));
  if (state.maxCalories) pill(`≤ ${state.maxCalories} kcal`, () => (state.maxCalories = undefined));
  if (state.minProtein) pill(`≥ ${state.minProtein} g ${t('browse.proteinShort')}`, () => (state.minProtein = undefined));
  if (state.minRating) pill(`⭐ ${state.minRating}+`, () => (state.minRating = undefined));
  for (const key of LIST_KEYS) {
    for (const value of state[key]) {
      const label = key === 'cuisine' ? t(`cuisine.${value}`) : key === 'course' ? t(`course.${value}`)
        : key === 'equipment' ? `🍳 ${value}` : `🥕 ${value}`;
      pill(label, () => (state[key] = state[key].filter((v) => v !== value)));
    }
  }

  const bar = $('#results-bar');
  bar.innerHTML = `<span class="results__count">${escapeHtml(t(total === 1 ? 'browse.countOne' : 'browse.count', { count: total }))}</span>` +
    pills.map((p, i) => `<span class="pill">${escapeHtml(p.label)}<button type="button" data-pill="${i}" aria-label="Remove filter">✕</button></span>`).join('');
  bar.querySelectorAll('[data-pill]').forEach((button) => button.addEventListener('click', () => {
    pills[Number(button.dataset.pill)].remove();
    changed({ immediate: true });
  }));
  $('[data-active-count]').textContent = pills.length ? `(${pills.length})` : '';
}

$('#load-more').addEventListener('click', () => search({ append: true }));
window.addEventListener('popstate', () => {
  Object.assign(state, readUrl());
  syncControls();
  search();
});

syncControls();
await search();
