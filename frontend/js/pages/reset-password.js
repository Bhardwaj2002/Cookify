import { initPage } from '../components/layout.js';
import { attachPasswordRules } from '../components/passwordRules.js';
import { api } from '../core/api.js';
import { $ } from '../core/dom.js';
import { clearErrors, clearErrorsOnEdit, enablePasswordToggles, showApiError, withLoading } from '../core/forms.js';
import { t, tError } from '../core/i18n.js';

await initPage();
enablePasswordToggles();

const token = new URLSearchParams(location.search).get('token');
const form = $('#reset-form');
clearErrorsOnEdit(form);

const showPanel = (name) => {
  for (const panel of document.querySelectorAll('[data-panel]')) panel.hidden = panel.dataset.panel !== name;
};

if (!token) {
  $('[data-invalid-message]').textContent = t('reset.missingToken');
  showPanel('invalid');
} else {
  attachPasswordRules($('#password-rules'), form.password, form.confirmPassword);
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors(form);

  await withLoading(form.querySelector('[type="submit"]'), async () => {
    try {
      await api.post('/api/auth/reset-password', {
        token,
        password: form.password.value,
        confirmPassword: form.confirmPassword.value,
      }, { auth: false });
      // Strip the one-time token from the address bar / history.
      history.replaceState(null, '', location.pathname);
      showPanel('done');
    } catch (error) {
      if (error.code === 'INVALID_RESET_TOKEN') {
        $('[data-invalid-message]').textContent = tError(error.code) ?? error.message;
        showPanel('invalid');
      } else {
        showApiError(form, error, { fieldMap: { passwordConfirmed: 'confirmPassword' } });
      }
    }
  });
});
