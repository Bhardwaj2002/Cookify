import { escapeHtml } from '../core/dom.js';
import { recipeCardHtml } from './recipeCard.js';

/**
 * Horizontal recipe slider with ‹ › arrows (mockup: "Slider Wheel Arrows").
 * Cards snap into place; arrows scroll by one visible width and hide at the ends.
 * {@code toCard} turns an item into card HTML (defaults to a plain recipe card).
 */
export function renderRecipeCarousel(container, recipes, emptyMessage, toCard = (r) => recipeCardHtml(r)) {
  if (!recipes.length) {
    container.innerHTML = `<div class="empty-state"><span class="empty-state__icon" aria-hidden="true">🍳</span><p>${escapeHtml(emptyMessage)}</p></div>`;
    return;
  }
  container.innerHTML = `
    <div class="carousel">
      <button type="button" class="carousel__arrow carousel__arrow--prev" aria-label="Previous">‹</button>
      <div class="carousel__track">${recipes.map((r) => `<div class="carousel__item">${toCard(r)}</div>`).join('')}</div>
      <button type="button" class="carousel__arrow carousel__arrow--next" aria-label="Next">›</button>
    </div>`;

  const track = container.querySelector('.carousel__track');
  const prev = container.querySelector('.carousel__arrow--prev');
  const next = container.querySelector('.carousel__arrow--next');
  const update = () => {
    prev.hidden = track.scrollLeft <= 4;
    next.hidden = track.scrollLeft + track.clientWidth >= track.scrollWidth - 4;
  };
  prev.addEventListener('click', () => track.scrollBy({ left: -track.clientWidth * 0.9, behavior: 'smooth' }));
  next.addEventListener('click', () => track.scrollBy({ left: track.clientWidth * 0.9, behavior: 'smooth' }));
  track.addEventListener('scroll', update, { passive: true });
  window.addEventListener('resize', update);
  requestAnimationFrame(update);
}
