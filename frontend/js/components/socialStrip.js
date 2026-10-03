import { api } from '../core/api.js';
import { ROUTES } from '../core/config.js';
import { escapeHtml } from '../core/dom.js';
import { formatCount, starsHtml } from '../core/format.js';
import { t, tError } from '../core/i18n.js';
import { toast } from './toast.js';

/**
 * Recipe page action bar (mockup: Subscribe button, comment bubble, star rating) plus Save.
 * Logged-out visitors are sent to log in when they try an action.
 */
export function initSocialStrip(container, recipe, me) {
  const isAuthor = me?.id === recipe.author.id;
  const state = {
    following: recipe.viewer.followingAuthor,
    saved: recipe.viewer.saved,
    myRating: recipe.viewer.myRating,
    avg: Number(recipe.avgRating),
    count: recipe.ratingCount,
    comments: recipe.commentCount,
  };
  const loginUrl = `${ROUTES.login}?next=${encodeURIComponent(location.pathname + location.search)}`;
  const requireLogin = () => {
    if (!me) location.href = loginUrl;
    return Boolean(me);
  };

  function render() {
    container.innerHTML = `
      ${isAuthor ? '' : `
        <button type="button" class="btn btn--rust btn--sm ${state.following ? 'is-active' : ''}" data-follow aria-pressed="${state.following}">
          ${state.following ? '✓ ' + escapeHtml(t('social.subscribed')) : '🔔 ' + escapeHtml(t('publicProfile.subscribe'))}
        </button>`}
      <button type="button" class="btn btn--sm ${state.saved ? 'is-active' : 'btn--ghost'}" data-save aria-pressed="${state.saved}"
              style="${state.saved ? '' : 'background:var(--white)'}">
        ${state.saved ? '🔖 ' + escapeHtml(t('social.saved')) : '🔖 ' + escapeHtml(t('social.save'))}
      </button>
      <a class="comment-bubble" href="#comments" title="${escapeHtml(t('comments.title'))}">💬 ${formatCount(state.comments)}</a>
      <span class="rating-summary">
        ${starsHtml(state.avg)}
        <strong>${state.count ? state.avg.toFixed(1) : ''}</strong>
        <span class="text-muted">${escapeHtml(state.count ? t('recipe.ratings', { count: formatCount(state.count) }) : t('recipe.noRatings'))}</span>
      </span>
      ${isAuthor ? '' : `
        <span class="rate" role="group" aria-label="${escapeHtml(t('social.yourRating'))}">
          <span class="rate__label">${escapeHtml(t('social.yourRating'))}</span>
          <span class="rate__stars">${[1, 2, 3, 4, 5].map((n) => `
            <button type="button" class="rate__star ${state.myRating >= n ? 'is-on' : ''}" data-rate="${n}"
                    aria-label="${n} / 5" aria-pressed="${state.myRating === n}">★</button>`).join('')}</span>
          ${state.myRating ? `<button type="button" class="rate__clear" data-unrate>${escapeHtml(t('social.clearRating'))}</button>` : ''}
        </span>`}`;
  }

  async function run(button, action) {
    if (!requireLogin()) return;
    button.disabled = true;
    try {
      await action();
      render();
    } catch (error) {
      toast(tError(error.code) ?? error.message, 'error');
      button.disabled = false;
    }
  }

  container.addEventListener('click', (e) => {
    const follow = e.target.closest('[data-follow]');
    const save = e.target.closest('[data-save]');
    const rate = e.target.closest('[data-rate]');
    const unrate = e.target.closest('[data-unrate]');
    const username = encodeURIComponent(recipe.author.username);

    if (follow) {
      run(follow, async () => {
        const result = state.following
          ? await api.delete(`/api/users/${username}/follow`)
          : await api.put(`/api/users/${username}/follow`);
        state.following = result.following;
        toast(t(result.following ? 'social.subscribedToast' : 'social.unsubscribedToast', { name: recipe.author.username }), 'success');
      });
    } else if (save) {
      run(save, async () => {
        state.saved ? await api.delete(`/api/recipes/${recipe.id}/save`) : await api.put(`/api/recipes/${recipe.id}/save`);
        state.saved = !state.saved;
        toast(t(state.saved ? 'social.savedToast' : 'social.unsavedToast'), 'success');
      });
    } else if (rate) {
      run(rate, async () => {
        const result = await api.put(`/api/recipes/${recipe.id}/rating`, { score: Number(rate.dataset.rate) });
        Object.assign(state, { avg: Number(result.avgRating), count: result.ratingCount, myRating: result.myRating });
        toast(t('social.ratedToast', { score: result.myRating }), 'success');
      });
    } else if (unrate) {
      run(unrate, async () => {
        const result = await api.delete(`/api/recipes/${recipe.id}/rating`);
        Object.assign(state, { avg: Number(result.avgRating), count: result.ratingCount, myRating: null });
      });
    }
  });

  // Preview stars on hover.
  container.addEventListener('mouseover', (e) => {
    const star = e.target.closest('[data-rate]');
    if (!star) return;
    container.querySelectorAll('[data-rate]').forEach((s) => s.classList.toggle('is-on', Number(s.dataset.rate) <= Number(star.dataset.rate)));
  });
  container.addEventListener('mouseout', (e) => {
    if (e.target.closest('.rate__stars') && !e.relatedTarget?.closest?.('.rate__stars')) {
      container.querySelectorAll('[data-rate]').forEach((s) => s.classList.toggle('is-on', state.myRating >= Number(s.dataset.rate)));
    }
  });

  render();
  return {
    adjustComments(delta) {
      state.comments += delta;
      render();
    },
  };
}
