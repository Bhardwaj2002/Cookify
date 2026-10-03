import { renderRecipeCarousel } from '../components/carousel.js';
import { chefHat } from '../components/icons.js';
import { initPage } from '../components/layout.js';
import { recipeCardHtml, renderRecipeGrid } from '../components/recipeCard.js';
import { api } from '../core/api.js';
import { $ } from '../core/dom.js';
import { t } from '../core/i18n.js';

$('[data-hero-hat]').innerHTML = chefHat;

const user = await initPage();
$('[data-guest-actions]').hidden = Boolean(user);
$('[data-user-actions]').hidden = !user;

/** "For You" carousel (logged-in users): each card says why it was picked. */
async function loadForYou() {
  const feed = await api.get('/api/users/me/recommendations?size=12');
  if (!feed.content.length) return;
  renderRecipeCarousel($('#for-you'), feed.content, '',
    (item) => recipeCardHtml(item.recipe, { reason: t(`reasons.${item.reasons[0]}`, { cuisine: t(`cuisine.${item.recipe.cuisine}`) }) }));
  $('[data-for-you]').hidden = false;
}

async function loadLatest() {
  const latest = await api.get('/api/recipes?size=8&sort=NEWEST', { auth: false });
  if (latest.content.length) {
    renderRecipeGrid($('#latest-recipes'), latest.content, t('landing.noRecipes'));
    $('[data-latest]').hidden = false;
  }
}

// The landing page still works if the API is down.
await Promise.allSettled([user ? loadForYou() : Promise.resolve(), loadLatest()]);
