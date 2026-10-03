/** Small DOM utilities. */

const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

/** Escapes text for safe interpolation into HTML strings. Always use for user-provided values. */
export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ESCAPES[c]);
}

export const $ = (selector, root = document) => root.querySelector(selector);
export const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

/** Initials for an avatar placeholder: "Ayush Singh" → "AS", "chef_ayush" → "C". */
export function initials(user) {
  const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
  if (name) return name.split(/\s+/).map((p) => p[0]).slice(0, 2).join('');
  return (user.username || '?')[0];
}

export function displayName(user) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;
}
