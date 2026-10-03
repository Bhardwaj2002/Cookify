import { initPage } from '../components/layout.js';
import { attachPasswordRules } from '../components/passwordRules.js';
import { api } from '../core/api.js';
import { AFTER_LOGIN } from '../core/config.js';
import { startSession } from '../core/auth.js';
import { $ } from '../core/dom.js';
import { clearErrors, clearErrorsOnEdit, enablePasswordToggles, showApiError, withLoading } from '../core/forms.js';

await initPage({ access: 'guest' });
enablePasswordToggles();

const form = $('#signup-form');
clearErrorsOnEdit(form);
attachPasswordRules($('#password-rules'), form.password, form.confirmPassword);

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors(form);

  const payload = Object.fromEntries(new FormData(form));
  for (const key of ['firstName', 'lastName', 'phone']) {
    if (!payload[key]?.trim()) payload[key] = null;
  }

  await withLoading(form.querySelector('[type="submit"]'), async () => {
    try {
      const result = await api.post('/api/auth/signup', payload, { auth: false });
      startSession(result);
      location.replace(AFTER_LOGIN);
    } catch (error) {
      showApiError(form, error, {
        fieldMap: { passwordConfirmed: 'confirmPassword' },
        codeToField: { USERNAME_TAKEN: 'username', EMAIL_TAKEN: 'email', PHONE_TAKEN: 'phone' },
      });
    }
  });
});
