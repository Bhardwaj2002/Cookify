import { t, tError } from './i18n.js';

/** Helpers for showing API validation errors next to inputs and handling submit buttons. */

/** Clears all field errors and the form-level alert. */
export function clearErrors(form) {
  form.querySelectorAll('.field.has-error').forEach((f) => f.classList.remove('has-error'));
  form.querySelectorAll('.field__error').forEach((e) => (e.textContent = ''));
  form.querySelectorAll('[aria-invalid]').forEach((i) => i.removeAttribute('aria-invalid'));
  const alert = form.querySelector('[data-form-alert]');
  if (alert) alert.hidden = true;
}

/** Marks one input as invalid with a message. */
export function setFieldError(form, name, message) {
  const input = form.elements[name];
  const field = input?.closest('.field');
  if (!field) return false;
  field.classList.add('has-error');
  input.setAttribute('aria-invalid', 'true');
  const slot = field.querySelector('.field__error');
  if (slot) slot.textContent = message;
  return true;
}

/** Shows a message in the form's alert box. */
export function showFormAlert(form, message, type = 'error') {
  const alert = form.querySelector('[data-form-alert]');
  if (!alert) return;
  alert.className = `alert alert--${type}`;
  alert.textContent = message;
  alert.hidden = false;
}

/**
 * Displays an ApiError: field errors go next to their inputs (with optional name mapping,
 * e.g. passwordConfirmed → confirmPassword), everything else in the form alert.
 * {@code codeToField} routes specific error codes (e.g. USERNAME_TAKEN) to an input.
 */
export function showApiError(form, error, { fieldMap = {}, codeToField = {} } = {}) {
  const message = tError(error.code) ?? error.message;
  if (codeToField[error.code] && setFieldError(form, codeToField[error.code], message)) {
    form.elements[codeToField[error.code]].focus();
    return;
  }
  let placed = 0;
  for (const { field, message: fieldMessage } of error.fields ?? []) {
    if (setFieldError(form, fieldMap[field] ?? field, capitalize(fieldMessage))) placed++;
  }
  if (placed === 0) showFormAlert(form, message);
  form.querySelector('[aria-invalid="true"]')?.focus();
}

/** Disables the submit button with a spinner while {@code task} runs. */
export async function withLoading(button, task) {
  button.disabled = true;
  button.classList.add('is-loading');
  try {
    return await task();
  } finally {
    button.disabled = false;
    button.classList.remove('is-loading');
  }
}

/** Adds show/hide buttons to every password input inside .password-wrap. */
export function enablePasswordToggles(root = document) {
  root.querySelectorAll('.password-toggle').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = btn.parentElement.querySelector('input');
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.textContent = t(show ? 'common.hide' : 'common.show');
      btn.setAttribute('aria-pressed', String(show));
    });
  });
}

function capitalize(s) {
  return s ? s[0].toUpperCase() + s.slice(1) : s;
}

/**
 * Removes a field's error as soon as the user edits it, instead of waiting for the next submit.
 * Also clears section-level errors (e.g. the ingredients list) when something inside changes.
 */
export function clearErrorsOnEdit(form) {
  const handler = (event) => {
    const field = event.target.closest('.field');
    if (field) {
      field.classList.remove('has-error');
      field.querySelectorAll('.field__error').forEach((e) => (e.textContent = ''));
      field.querySelectorAll('[aria-invalid]').forEach((i) => i.removeAttribute('aria-invalid'));
    }
    const section = event.target.closest('section, .card');
    section?.querySelectorAll('[data-section-error]').forEach((e) => (e.textContent = ''));
  };
  form.addEventListener('input', handler);
  form.addEventListener('change', handler);
}
