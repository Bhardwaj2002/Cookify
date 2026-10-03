/**
 * Display formatting shared by recipe cards and pages. Dates and numbers follow the page language
 * (<html lang>, set by i18n), so a Hindi page shows Hindi month names.
 */

const locale = () => document.documentElement.lang || undefined;
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const compact = { format: (n) => new Intl.NumberFormat(locale(), { notation: 'compact', maximumFractionDigits: 1 }).format(n) };

/** 75 → "1 h 15 min", 40 → "40 min". */
export function formatMinutes(total) {
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${m} min` : `${h} h`;
}

export const formatCost = (value) => inr.format(Number(value));
export const formatCount = (value) => compact.format(value);

export function formatDate(iso, style = 'medium') {
  return iso ? new Intl.DateTimeFormat(locale(), { dateStyle: style }).format(new Date(iso)) : '';
}

const relative = { format: (value, unit) => new Intl.RelativeTimeFormat(locale(), { numeric: 'auto' }).format(value, unit) };

/** "3 minutes ago", "yesterday"… */
export function timeAgo(iso) {
  const seconds = (new Date(iso).getTime() - Date.now()) / 1000;
  const units = [['year', 31536000], ['month', 2592000], ['week', 604800], ['day', 86400], ['hour', 3600], ['minute', 60]];
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) return relative.format(Math.round(seconds / size), unit);
  }
  return relative.format(0, 'second');
}

/** ★★★☆☆ for a 1–5 value (rounded to the nearest half for ratings). */
export function starsHtml(value, { max = 5, label = '' } = {}) {
  const rounded = Math.round(Number(value) * 2) / 2;
  let html = '';
  for (let i = 1; i <= max; i++) {
    const cls = rounded >= i ? 'is-full' : rounded >= i - 0.5 ? 'is-half' : '';
    html += `<span class="star ${cls}" aria-hidden="true">★</span>`;
  }
  return `<span class="stars" role="img" aria-label="${label || `${rounded} of ${max}`}">${html}</span>`;
}
