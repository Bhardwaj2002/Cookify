/**
 * Six single-digit boxes that behave like one input: auto-advance, backspace to previous,
 * arrow keys, and pasting a full code fills every box.
 */
export function createOtpInput(container, { length = 6, onComplete } = {}) {
  container.classList.add('otp');
  container.innerHTML = Array.from({ length }, (_, i) =>
    `<input class="otp__digit" inputmode="numeric" autocomplete="${i === 0 ? 'one-time-code' : 'off'}"
            maxlength="1" pattern="[0-9]" aria-label="Digit ${i + 1}">`).join('');
  const boxes = [...container.querySelectorAll('input')];

  const value = () => boxes.map((b) => b.value).join('');
  const notifyIfComplete = () => {
    if (value().length === length) onComplete?.(value());
  };

  boxes.forEach((box, i) => {
    box.addEventListener('input', () => {
      box.value = box.value.replace(/\D/g, '').slice(-1);
      container.classList.remove('has-error');
      if (box.value && i < length - 1) boxes[i + 1].focus();
      notifyIfComplete();
    });
    box.addEventListener('keydown', (e) => {
      if (e.key === 'Backspace' && !box.value && i > 0) boxes[i - 1].focus();
      if (e.key === 'ArrowLeft' && i > 0) boxes[i - 1].focus();
      if (e.key === 'ArrowRight' && i < length - 1) boxes[i + 1].focus();
    });
    box.addEventListener('paste', (e) => {
      const digits = (e.clipboardData?.getData('text') ?? '').replace(/\D/g, '').slice(0, length);
      if (!digits) return;
      e.preventDefault();
      boxes.forEach((b, j) => (b.value = digits[j] ?? ''));
      boxes[Math.min(digits.length, length - 1)].focus();
      notifyIfComplete();
    });
  });

  return {
    value,
    focus: () => boxes[0].focus(),
    clear: () => {
      boxes.forEach((b) => (b.value = ''));
      boxes[0].focus();
    },
    showError: () => container.classList.add('has-error'),
  };
}
