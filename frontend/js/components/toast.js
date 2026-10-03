/** Brief, non-blocking notifications at the bottom of the screen. */

let region;

export function toast(message, type = 'info', durationMs = 3500) {
  region ??= createRegion();
  const el = document.createElement('div');
  el.className = `toast toast--${type}`;
  el.textContent = message;
  region.append(el);
  setTimeout(() => el.remove(), durationMs);
}

function createRegion() {
  const el = document.createElement('div');
  el.className = 'toast-region';
  el.setAttribute('role', 'status');
  el.setAttribute('aria-live', 'polite');
  document.body.append(el);
  return el;
}
