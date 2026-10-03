import { initPage } from '../components/layout.js';
import { api } from '../core/api.js';
import { API_BASE } from '../core/config.js';
import { $ } from '../core/dom.js';
import { clearErrors, setFieldError, showApiError, withLoading } from '../core/forms.js';
import { t } from '../core/i18n.js';

await initPage();

const form = $('#forgot-form');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors(form);
  const identifier = form.identifier.value.trim();
  if (!identifier) {
    setFieldError(form, 'identifier', t('login.identifier'));
    return;
  }

  await withLoading(form.querySelector('[type="submit"]'), async () => {
    try {
      await api.post('/api/auth/forgot-password', { identifier }, { auth: false });
      $('[data-panel="form"]').hidden = true;
      $('[data-panel="sent"]').hidden = false;
      $('[data-dev-hint]').hidden = !API_BASE.includes('localhost') && !API_BASE.includes('127.0.0.1');
    } catch (error) {
      showApiError(form, error);
    }
  });
});
