import { avatarHtml } from '../../components/avatar.js';
import { toast } from '../../components/toast.js';
import { api } from '../../core/api.js';
import { $ } from '../../core/dom.js';
import { clearErrors, clearErrorsOnEdit, showApiError, withLoading } from '../../core/forms.js';
import { t, tError } from '../../core/i18n.js';
import { squareResize } from '../../core/image.js';

const MAX_SOURCE_BYTES = 15 * 1024 * 1024; // before resizing; the server limit applies after

/**
 * "Profile" tab: picture upload (click or drag & drop) and the details form.
 * @param {object} user current user
 * @param {(user: object) => void} onChange called with the updated user after every save
 */
export function initProfileTab(user, onChange) {
  const form = $('#profile-form');
  clearErrorsOnEdit(form);
  const uploader = $('#avatar-uploader');
  const fileInput = $('#avatar-input');
  const removeButton = $('#avatar-remove');
  const bioCount = $('[data-bio-count]');

  // ---- Details form ----
  const fill = (u) => {
    form.firstName.value = u.firstName ?? '';
    form.lastName.value = u.lastName ?? '';
    form.username.value = u.username;
    form.phone.value = u.phone ?? '';
    form.age.value = u.age ?? '';
    form.gender.value = u.gender ?? '';
    form.bio.value = u.bio ?? '';
    form.preferredLang.value = u.preferredLang ?? 'en';
    updateBioCount();
  };

  function updateBioCount() {
    const length = form.bio.value.length;
    bioCount.textContent = `${length} / 500`;
    bioCount.parentElement.classList.toggle('is-over', length > 500);
  }
  form.bio.addEventListener('input', updateBioCount);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearErrors(form);
    const value = (name) => form[name].value.trim() || null;
    const payload = {
      username: form.username.value.trim(),
      firstName: value('firstName'),
      lastName: value('lastName'),
      phone: value('phone'),
      age: form.age.value ? Number(form.age.value) : null,
      gender: form.gender.value || null,
      bio: value('bio'),
      preferredLang: form.preferredLang.value,
    };
    await withLoading(form.querySelector('[type="submit"]'), async () => {
      try {
        user = await api.put('/api/users/me', payload);
        fill(user);
        onChange(user);
        toast(t('profileForm.saved'), 'success');
      } catch (error) {
        showApiError(form, error, { codeToField: { USERNAME_TAKEN: 'username', PHONE_TAKEN: 'phone' } });
      }
    });
  });

  // ---- Avatar ----
  const renderAvatar = () => {
    $('[data-avatar-preview]').innerHTML = avatarHtml(user);
    removeButton.hidden = !user.avatarUrl;
  };

  async function uploadAvatar(file) {
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      toast(tError('INVALID_IMAGE'), 'error');
      return;
    }
    if (file.size > MAX_SOURCE_BYTES) {
      toast(tError('FILE_TOO_LARGE'), 'error');
      return;
    }
    uploader.setAttribute('aria-busy', 'true');
    try {
      const blob = await squareResize(file);
      const data = new FormData();
      data.append('file', blob, 'avatar.jpg');
      user = await api.upload('/api/users/me/avatar', data);
      renderAvatar();
      onChange(user);
      toast(t('profileForm.avatarSaved'), 'success');
    } catch (error) {
      toast(tError(error.code) ?? error.message, 'error');
    } finally {
      uploader.removeAttribute('aria-busy');
      fileInput.value = '';
    }
  }

  fileInput.addEventListener('change', () => uploadAvatar(fileInput.files[0]));

  uploader.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploader.classList.add('is-dragging');
  });
  uploader.addEventListener('dragleave', () => uploader.classList.remove('is-dragging'));
  uploader.addEventListener('drop', (e) => {
    e.preventDefault();
    uploader.classList.remove('is-dragging');
    uploadAvatar(e.dataTransfer.files[0]);
  });

  removeButton.addEventListener('click', () => withLoading(removeButton, async () => {
    try {
      user = await api.delete('/api/users/me/avatar');
      renderAvatar();
      onChange(user);
      toast(t('profileForm.avatarRemoved'), 'success');
    } catch (error) {
      toast(tError(error.code) ?? error.message, 'error');
    }
  }));

  fill(user);
  renderAvatar();
}
