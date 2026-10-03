import { initPage } from '../components/layout.js';
import { createOtpInput } from '../components/otpInput.js';
import { api } from '../core/api.js';
import { nextUrl, startSession } from '../core/auth.js';
import { $ } from '../core/dom.js';
import { clearErrors, clearErrorsOnEdit, enablePasswordToggles, setFieldError, showApiError, showFormAlert, withLoading } from '../core/forms.js';
import { t, tError } from '../core/i18n.js';

/**
 * Login in up to two steps:
 *  1. identifier + password → tokens, or { twoFactorRequired, challengeId, maskedEmail }
 *  2. emailed 6-digit code (+ "remember this device") → tokens
 */

await initPage({ access: 'guest' });
enablePasswordToggles();

const passwordStep = $('[data-step="password"]');
const otpStep = $('[data-step="otp"]');
const loginForm = $('#login-form');
const otpForm = $('#otp-form');
clearErrorsOnEdit(loginForm);

let challengeId = null;
let countdown = null;

const query = new URLSearchParams(location.search);
if (query.has('banned')) {
  showFormAlert(loginForm, t('errors.ACCOUNT_BANNED_LONG'));
} else if (query.has('next')) {
  showFormAlert(loginForm, t('login.sessionExpired'), 'info');
}

// ---- Step 1 ----------------------------------------------------------------

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors(loginForm);
  const identifier = loginForm.identifier.value.trim();
  const password = loginForm.password.value;

  if (!identifier) setFieldError(loginForm, 'identifier', t('login.identifier'));
  if (!password) setFieldError(loginForm, 'password', t('login.password'));
  if (!identifier || !password) return;

  await withLoading(loginForm.querySelector('[type="submit"]'), async () => {
    try {
      const result = await api.post('/api/auth/login', { identifier, password }, { auth: false });
      if (result.twoFactorRequired) showOtpStep(result);
      else finishLogin(result);
    } catch (error) {
      showApiError(loginForm, error);
    }
  });
});

// ---- Step 2 ----------------------------------------------------------------

const otp = createOtpInput($('#otp-boxes'), {
  onComplete: () => otpForm.requestSubmit(),
});

function showOtpStep({ challengeId: id, maskedEmail, expiresIn }) {
  challengeId = id;
  $('[data-otp-subtitle]').textContent = t('otp.subtitle', { email: maskedEmail });
  passwordStep.hidden = true;
  otpStep.hidden = false;
  clearErrors(otpForm);
  otp.clear();
  startCountdown(expiresIn);
}

otpForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearErrors(otpForm);
  const code = otp.value();
  if (code.length !== 6) {
    otp.showError();
    showFormAlert(otpForm, t('otp.incomplete'));
    return;
  }

  await withLoading(otpForm.querySelector('[type="submit"]'), async () => {
    try {
      const result = await api.post('/api/auth/verify-otp', {
        challengeId,
        code,
        rememberDevice: otpForm.rememberDevice.checked,
      }, { auth: false });
      finishLogin(result);
    } catch (error) {
      otp.showError();
      if (error.code === 'OTP_EXPIRED') {
        backToPasswordStep(tError('OTP_EXPIRED') ?? error.message);
      } else {
        showFormAlert(otpForm, error.message); // includes "N attempt(s) left"
        otp.clear();
      }
    }
  });
});

function startCountdown(seconds) {
  clearInterval(countdown);
  const timer = $('[data-otp-timer]');
  const endsAt = Date.now() + seconds * 1000;
  const tick = () => {
    const left = Math.max(0, Math.round((endsAt - Date.now()) / 1000));
    const time = `${Math.floor(left / 60)}:${String(left % 60).padStart(2, '0')}`;
    timer.textContent = left > 0 ? t('otp.expiresIn', { time }) : t('otp.expired');
    timer.classList.toggle('is-expired', left === 0);
    if (left === 0) clearInterval(countdown);
  };
  tick();
  countdown = setInterval(tick, 1000);
}

function backToPasswordStep(message) {
  clearInterval(countdown);
  otpStep.hidden = true;
  passwordStep.hidden = false;
  loginForm.password.value = '';
  showFormAlert(loginForm, message);
  loginForm.password.focus();
}

function finishLogin(authResponse) {
  startSession(authResponse);
  location.replace(nextUrl());
}
