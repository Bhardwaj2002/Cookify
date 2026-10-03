import { initPage } from '../components/layout.js';
import { toast } from '../components/toast.js';
import { api } from '../core/api.js';
import { recipeUrl, ROUTES } from '../core/config.js';
import { $, escapeHtml } from '../core/dom.js';
import { clearErrors, clearErrorsOnEdit, setFieldError, showFormAlert } from '../core/forms.js';
import { t, tError } from '../core/i18n.js';
import { COMMON_EQUIPMENT, COURSES, CUISINES, DIETS } from '../core/recipeOptions.js';

/**
 * Create (/pages/recipe-editor.html) or edit (?id=…) a recipe.
 * Saving = POST/PUT the recipe JSON, then upload new media one by one and delete removed media.
 */

await initPage({ access: 'user' });

const form = $('#recipe-form');
clearErrorsOnEdit(form);
const params = new URLSearchParams(location.search);
let recipeId = params.get('id');

const MAX_MEDIA = 10;
const MAX_IMAGE = 10 * 1024 * 1024;
const MAX_VIDEO = 50 * 1024 * 1024;

/** Media shown in the grid: existing ({id,url,type}) or new ({file, previewUrl, type}). */
let media = [];
const removedMediaIds = [];
let equipment = [];

// ---- Static options -----------------------------------------------------------

$('#diet-chips').innerHTML = DIETS.map(({ value, icon }) => `
  <label class="chip"><input type="radio" name="dietType" value="${value}" required>
  <span>${icon} ${escapeHtml(t(`diet.${value}`))}</span></label>`).join('');

const optionList = (values, prefix) =>
  `<option value="" disabled selected>${escapeHtml(t('editor.choose'))}</option>` +
  values.map((v) => `<option value="${v}">${escapeHtml(t(`${prefix}.${v}`))}</option>`).join('');
form.cuisine.innerHTML = optionList(CUISINES, 'cuisine');
form.course.innerHTML = optionList(COURSES, 'course');

// ---- Difficulty stars ------------------------------------------------------------

const picker = $('#difficulty-picker');
picker.innerHTML = [1, 2, 3, 4, 5].map((n) => `
  <label title="${escapeHtml(t(`difficulty.${n}`))}">
    <input type="radio" name="difficulty" value="${n}"><span aria-hidden="true">★</span>
    <span class="visually-hidden">${escapeHtml(t(`difficulty.${n}`))}</span>
  </label>`).join('');
const paintStars = (value) => {
  picker.querySelectorAll('label').forEach((label, i) => label.classList.toggle('is-on', i < value));
  $('[data-difficulty-label]').textContent = value ? t(`difficulty.${value}`) : '';
};
picker.addEventListener('change', () => paintStars(Number(form.difficulty.value)));
picker.addEventListener('mouseover', (e) => {
  const label = e.target.closest('label');
  if (label) paintStars([...picker.children].indexOf(label) + 1);
});
picker.addEventListener('mouseleave', () => paintStars(Number(form.difficulty.value || 0)));

// ---- Ingredients ---------------------------------------------------------------------

const ingredientRows = $('#ingredient-rows');

function addIngredientRow(name = '', quantity = '', focus = false) {
  const li = document.createElement('li');
  li.className = 'ingredient-row';
  li.innerHTML = `
    <input class="input" data-ing-name maxlength="60" placeholder="${escapeHtml(t('editor.ingredientName'))}"
           aria-label="${escapeHtml(t('editor.ingredientName'))}" value="${escapeHtml(name)}">
    <input class="input" data-ing-qty maxlength="60" placeholder="${escapeHtml(t('editor.quantity'))}"
           aria-label="${escapeHtml(t('editor.quantity'))}" value="${escapeHtml(quantity ?? '')}">
    <button type="button" class="icon-btn icon-btn--danger" data-remove aria-label="${escapeHtml(t('editor.remove'))}">✕</button>`;
  ingredientRows.append(li);
  if (focus) li.querySelector('[data-ing-name]').focus();
}

ingredientRows.addEventListener('click', (e) => {
  if (e.target.closest('[data-remove]')) {
    e.target.closest('li').remove();
    if (!ingredientRows.children.length) addIngredientRow();
  }
});
ingredientRows.addEventListener('keydown', (e) => {
  // Enter in the last row's quantity adds a new row (fast keyboard entry).
  if (e.key === 'Enter' && e.target.matches('[data-ing-qty]')) {
    e.preventDefault();
    if (e.target.closest('li') === ingredientRows.lastElementChild) addIngredientRow('', '', true);
  }
});
$('#add-ingredient').addEventListener('click', () => addIngredientRow('', '', true));

// ---- Steps ------------------------------------------------------------------------------

const stepRows = $('#step-rows');

function addStepRow(text = '', focus = false) {
  const li = document.createElement('li');
  li.className = 'step-row';
  li.innerHTML = `
    <textarea class="input" data-step maxlength="1000" placeholder="${escapeHtml(t('editor.stepPlaceholder'))}"
              aria-label="${escapeHtml(t('editor.steps'))}">${escapeHtml(text)}</textarea>
    <div class="row-actions">
      <button type="button" class="icon-btn" data-move="-1" aria-label="${escapeHtml(t('editor.moveUp'))}">▲</button>
      <button type="button" class="icon-btn" data-move="1" aria-label="${escapeHtml(t('editor.moveDown'))}">▼</button>
      <button type="button" class="icon-btn icon-btn--danger" data-remove aria-label="${escapeHtml(t('editor.remove'))}">✕</button>
    </div>`;
  stepRows.append(li);
  updateStepButtons();
  if (focus) li.querySelector('textarea').focus();
}

function updateStepButtons() {
  const rows = [...stepRows.children];
  rows.forEach((row, i) => {
    row.querySelector('[data-move="-1"]').disabled = i === 0;
    row.querySelector('[data-move="1"]').disabled = i === rows.length - 1;
  });
}

stepRows.addEventListener('click', (e) => {
  const row = e.target.closest('li');
  if (e.target.closest('[data-remove]')) {
    row.remove();
    if (!stepRows.children.length) addStepRow();
  }
  const move = e.target.closest('[data-move]');
  if (move) {
    const sibling = move.dataset.move === '-1' ? row.previousElementSibling : row.nextElementSibling;
    if (sibling) {
      move.dataset.move === '-1' ? sibling.before(row) : sibling.after(row);
      move.focus();
    }
  }
  updateStepButtons();
});
$('#add-step').addEventListener('click', () => addStepRow('', true));

// ---- Equipment tags -------------------------------------------------------------------

const tagBox = $('#equipment-input');
const tagInput = tagBox.querySelector('input');

function renderEquipment() {
  tagBox.querySelectorAll('.tag').forEach((tag) => tag.remove());
  for (const name of equipment) {
    const tag = document.createElement('span');
    tag.className = 'tag';
    tag.innerHTML = `${escapeHtml(name)}<button type="button" aria-label="${escapeHtml(t('editor.remove'))} ${escapeHtml(name)}">✕</button>`;
    tag.querySelector('button').addEventListener('click', () => {
      equipment = equipment.filter((e) => e !== name);
      renderEquipment();
    });
    tagBox.insertBefore(tag, tagInput);
  }
  $('#equipment-suggestions').innerHTML = COMMON_EQUIPMENT
    .filter((s) => !equipment.includes(s))
    .slice(0, 8)
    .map((s) => `<button type="button" data-suggest="${escapeHtml(s)}">＋ ${escapeHtml(s)}</button>`).join('');
}

function addEquipment(raw) {
  const name = raw.trim().replace(/\s+/g, ' ').toLowerCase();
  if (name && !equipment.includes(name) && equipment.length < 20) equipment.push(name);
  renderEquipment();
}

tagInput.addEventListener('keydown', (e) => {
  if ((e.key === 'Enter' || e.key === ',') && tagInput.value.trim()) {
    e.preventDefault();
    addEquipment(tagInput.value);
    tagInput.value = '';
  } else if (e.key === 'Enter') {
    e.preventDefault();
  } else if (e.key === 'Backspace' && !tagInput.value && equipment.length) {
    equipment.pop();
    renderEquipment();
  }
});
tagInput.addEventListener('blur', () => {
  if (tagInput.value.trim()) {
    addEquipment(tagInput.value);
    tagInput.value = '';
  }
});
tagBox.addEventListener('click', (e) => e.target === tagBox && tagInput.focus());
$('#equipment-suggestions').addEventListener('click', (e) => {
  const button = e.target.closest('[data-suggest]');
  if (button) addEquipment(button.dataset.suggest);
});

// ---- Media ---------------------------------------------------------------------------------

const dropzone = $('#dropzone');
const mediaInput = $('#media-input');
const mediaGrid = $('#media-grid');

function renderMedia() {
  mediaGrid.innerHTML = media.map((m, i) => `
    <div class="media-tile">
      ${m.type === 'VIDEO'
        ? `<video src="${escapeHtml(m.url ?? m.previewUrl)}" muted playsinline preload="metadata"></video>`
        : `<img src="${escapeHtml(m.url ?? m.previewUrl)}" alt="">`}
      <span class="media-tile__badge">${i === firstImageIndex() ? escapeHtml(t('editor.cover')) : m.type === 'VIDEO' ? '▶ video' : ''}</span>
      <button type="button" class="media-tile__remove" data-index="${i}" aria-label="${escapeHtml(t('editor.remove'))}">✕</button>
    </div>`).join('');
  mediaGrid.querySelectorAll('.media-tile__badge').forEach((b) => (b.hidden = !b.textContent));
}

const firstImageIndex = () => media.findIndex((m) => m.type === 'IMAGE');

function addFiles(files) {
  const error = $('[data-section-error="media"]');
  error.textContent = '';
  for (const file of files) {
    const isVideo = file.type.startsWith('video/');
    const isImage = file.type.startsWith('image/');
    if (!isVideo && !isImage) {
      error.textContent = t('errors.INVALID_MEDIA');
      continue;
    }
    if (file.size > (isVideo ? MAX_VIDEO : MAX_IMAGE)) {
      error.textContent = t(isVideo ? 'editor.videoTooLarge' : 'editor.imageTooLarge', { name: file.name });
      continue;
    }
    if (media.length >= MAX_MEDIA) {
      error.textContent = t('errors.TOO_MANY_MEDIA');
      break;
    }
    media.push({ file, previewUrl: URL.createObjectURL(file), type: isVideo ? 'VIDEO' : 'IMAGE' });
  }
  renderMedia();
}

mediaInput.addEventListener('change', () => {
  addFiles([...mediaInput.files]);
  mediaInput.value = '';
});
dropzone.addEventListener('dragover', (e) => {
  e.preventDefault();
  dropzone.classList.add('is-dragging');
});
dropzone.addEventListener('dragleave', () => dropzone.classList.remove('is-dragging'));
dropzone.addEventListener('drop', (e) => {
  e.preventDefault();
  dropzone.classList.remove('is-dragging');
  addFiles([...e.dataTransfer.files]);
});
mediaGrid.addEventListener('click', (e) => {
  const button = e.target.closest('.media-tile__remove');
  if (!button) return;
  const [removed] = media.splice(Number(button.dataset.index), 1);
  if (removed.id) removedMediaIds.push(removed.id);
  if (removed.previewUrl) URL.revokeObjectURL(removed.previewUrl);
  renderMedia();
});

// ---- Load (edit mode) -------------------------------------------------------------------

async function load() {
  if (!recipeId) {
    addIngredientRow();
    addIngredientRow();
    addStepRow();
    renderEquipment();
    return;
  }
  const recipe = await api.get(`/api/recipes/${encodeURIComponent(recipeId)}`).catch(() => null);
  if (!recipe || !recipe.canEdit) {
    showFormAlert(form, t('editor.cannotEdit'));
    form.querySelector('[data-submit]').disabled = true;
    return;
  }
  document.title = `${t('editor.titleEdit')} · Cookify`;
  $('[data-editor-title]').textContent = t('editor.titleEdit');
  form.querySelector('[data-submit]').textContent = t('common.save');
  form.querySelector('[data-cancel]').href = recipeUrl(recipe.id);

  form.title.value = recipe.title;
  form.description.value = recipe.description ?? '';
  form.querySelector(`input[name="dietType"][value="${recipe.diet.type}"]`).checked = true;
  form.cuisine.value = recipe.cuisine;
  form.course.value = recipe.course;
  for (const name of ['prepMinutes', 'cookMinutes', 'servings', 'calories']) form[name].value = recipe[name];
  form.cost.value = Number(recipe.cost);
  form.protein.value = recipe.protein ?? '';
  form.querySelector(`input[name="difficulty"][value="${recipe.difficulty}"]`).checked = true;
  paintStars(recipe.difficulty);
  recipe.ingredients.forEach((i) => addIngredientRow(i.name, i.quantity));
  recipe.steps.forEach((s) => addStepRow(s));
  equipment = [...recipe.equipment];
  renderEquipment();
  media = recipe.media.map((m) => ({ ...m }));
  renderMedia();
}

// ---- Save ------------------------------------------------------------------------------------

function collect() {
  const num = (name) => (form[name].value === '' ? null : Number(form[name].value));
  return {
    title: form.title.value.trim(),
    description: form.description.value.trim() || null,
    dietType: form.dietType.value || null,
    cuisine: form.cuisine.value || null,
    course: form.course.value || null,
    prepMinutes: num('prepMinutes'),
    cookMinutes: num('cookMinutes'),
    servings: num('servings'),
    cost: num('cost'),
    calories: num('calories'),
    protein: num('protein'),
    difficulty: form.difficulty.value ? Number(form.difficulty.value) : null,
    ingredients: [...ingredientRows.children]
      .map((li) => ({ name: li.querySelector('[data-ing-name]').value.trim(), quantity: li.querySelector('[data-ing-qty]').value.trim() || null }))
      .filter((i) => i.name),
    equipment,
    steps: [...stepRows.querySelectorAll('[data-step]')].map((s) => s.value.trim()).filter(Boolean),
  };
}

/** Shows an error next to its field, section (ingredients[2].name → ingredients) or the top alert. */
function placeError(field, message) {
  const section = field.split(/[[.]/)[0];
  const sectionSlot = form.querySelector(`[data-section-error="${section}"]`);
  if (sectionSlot) {
    sectionSlot.textContent = sectionSlot.textContent ? `${sectionSlot.textContent} · ${message}` : message;
    return true;
  }
  const custom = form.querySelector(`[data-field-error="${section}"]`);
  if (custom) {
    custom.textContent = message;
    return true;
  }
  return setFieldError(form, section, message);
}

function clearAll() {
  clearErrors(form);
  form.querySelectorAll('[data-section-error], [data-field-error]').forEach((el) => (el.textContent = ''));
}

const progress = $('[data-progress]');
const submit = form.querySelector('[data-submit]');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearAll();
  const payload = collect();

  // Quick client-side checks; the server re-validates everything.
  const problems = [];
  if (!payload.title) problems.push(['title', t('editor.required')]);
  if (!payload.dietType) problems.push(['dietType', t('editor.required')]);
  if (!payload.cuisine) problems.push(['cuisine', t('editor.required')]);
  if (!payload.course) problems.push(['course', t('editor.required')]);
  if (!payload.difficulty) problems.push(['difficulty', t('editor.required')]);
  if (!payload.ingredients.length) problems.push(['ingredients', t('editor.needIngredient')]);
  if (!payload.steps.length) problems.push(['steps', t('editor.needStep')]);
  if (problems.length) {
    problems.forEach(([field, message]) => placeError(field, message));
    showFormAlert(form, t('editor.fixErrors'));
    form.querySelector('[aria-invalid="true"], .section-error:not(:empty), .field__error:not(:empty)')
      ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }

  submit.disabled = true;
  submit.classList.add('is-loading');
  try {
    progress.textContent = t('editor.saving');
    const saved = recipeId
      ? await api.put(`/api/recipes/${recipeId}`, payload)
      : await api.post('/api/recipes', payload);

    if (!recipeId) {
      // From now on this is an edit: a retry after a failed upload must not create a duplicate.
      recipeId = saved.id;
      history.replaceState(null, '', `${ROUTES.recipeEditor}?id=${saved.id}`);
    }

    for (const id of removedMediaIds.splice(0)) {
      await api.delete(`/api/recipes/${recipeId}/media/${id}`);
    }
    const pending = media.filter((m) => m.file);
    for (const [i, item] of pending.entries()) {
      progress.textContent = t('editor.uploading', { current: i + 1, total: pending.length });
      const data = new FormData();
      data.append('file', item.file, item.file.name);
      const uploaded = await api.upload(`/api/recipes/${recipeId}/media`, data);
      URL.revokeObjectURL(item.previewUrl);
      Object.assign(item, uploaded, { file: null, previewUrl: null });
    }

    toast(t('editor.saved'), 'success');
    location.href = recipeUrl(recipeId);
  } catch (error) {
    progress.textContent = '';
    if (error.fields?.length) {
      error.fields.forEach(({ field, message }) => placeError(field, message));
      showFormAlert(form, t('editor.fixErrors'));
    } else {
      showFormAlert(form, tError(error.code) ?? error.message);
    }
    form.querySelector('[data-form-alert]').scrollIntoView({ behavior: 'smooth', block: 'center' });
  } finally {
    submit.disabled = false;
    submit.classList.remove('is-loading');
  }
});

await load();
