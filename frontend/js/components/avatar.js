import { escapeHtml, initials } from '../core/dom.js';

/** Avatar markup: the uploaded picture if there is one, otherwise the user's initials. */
export function avatarHtml(user, sizeClass = '') {
  const cls = `avatar ${sizeClass}`.trim();
  if (user?.avatarUrl) {
    return `<span class="${cls}"><img src="${escapeHtml(user.avatarUrl)}" alt="" loading="lazy"></span>`;
  }
  return `<span class="${cls}" aria-hidden="true">${escapeHtml(initials(user ?? {}))}</span>`;
}
