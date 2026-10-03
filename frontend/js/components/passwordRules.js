/**
 * Live checklist mirroring the backend's password rule (PasswordValidator):
 * 9–72 characters, no spaces, only letters/digits/standard symbols, and matching confirmation.
 */
const RULES = {
  length: (p) => p.length >= 9 && p.length <= 72,
  noSpaces: (p) => p.length > 0 && !/\s/.test(p),
  allowedChars: (p) => p.length > 0 && /^[\x21-\x7E]+$/.test(p),
};

export function attachPasswordRules(list, passwordInput, confirmInput) {
  const update = () => {
    const p = passwordInput.value;
    for (const [rule, check] of Object.entries(RULES)) {
      list.querySelector(`[data-rule="${rule}"]`)?.classList.toggle('is-ok', check(p));
    }
    const match = list.querySelector('[data-rule="match"]');
    match?.classList.toggle('is-ok', p.length > 0 && confirmInput?.value === p);
  };
  passwordInput.addEventListener('input', update);
  confirmInput?.addEventListener('input', update);
  update();
}

export function isPasswordValid(p) {
  return Object.values(RULES).every((check) => check(p));
}
