import { toast } from '../../components/toast.js';
import { api } from '../../core/api.js';
import { $, escapeHtml } from '../../core/dom.js';
import { clearErrors, showApiError, withLoading } from '../../core/forms.js';
import { t } from '../../core/i18n.js';
import { DIETS } from '../../core/recipeOptions.js';

/** Diet options, in the order shown. '' = no preference. */
const DIET_CHOICES = [{ value: '', icon: '🍽️' }, ...DIETS];

/**
 * Sliders store "no limit" at their extreme: calories at max (2000) and protein at 0 both mean
 * null on the server.
 */
const SLIDERS = {
  maxCalories: { noValueAt: 'max', format: (v) => `${v} kcal`, noValueKey: 'prefs.noLimit' },
  minProtein: { noValueAt: 'min', format: (v) => `${v} g`, noValueKey: 'prefs.noMinimum' },
};

/** "Preferences" tab: diet chips and calorie / protein sliders. */
export async function initPreferencesTab() {
  const form = $('#preferences-form');

  $('#diet-chips').innerHTML = DIET_CHOICES.map(({ value, icon }) => `
    <label class="chip">
      <input type="radio" name="dietType" value="${value}">
      <span>${icon} ${escapeHtml(t(value ? `diet.${value}` : 'diet.NONE'))}</span>
    </label>`).join('');

  for (const name of Object.keys(SLIDERS)) {
    form[name].addEventListener('input', () => updateSlider(form, name));
  }

  const prefs = await api.get('/api/users/me/preferences');
  fill(form, prefs);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearErrors(form);
    await withLoading(form.querySelector('[type="submit"]'), async () => {
      try {
        const saved = await api.put('/api/users/me/preferences', {
          dietType: form.dietType.value || null,
          maxCalories: sliderValue(form, 'maxCalories'),
          minProtein: sliderValue(form, 'minProtein'),
        });
        fill(form, saved);
        toast(t('prefs.saved'), 'success');
      } catch (error) {
        showApiError(form, error);
      }
    });
  });
}

function fill(form, prefs) {
  form.querySelector(`input[name="dietType"][value="${prefs.dietType ?? ''}"]`).checked = true;
  form.maxCalories.value = prefs.maxCalories ?? form.maxCalories.max;
  form.minProtein.value = prefs.minProtein ?? form.minProtein.min;
  Object.keys(SLIDERS).forEach((name) => updateSlider(form, name));
}

function sliderValue(form, name) {
  const input = form[name];
  const { noValueAt } = SLIDERS[name];
  return input.value === input[noValueAt] ? null : Number(input.value);
}

function updateSlider(form, name) {
  const input = form[name];
  const { format, noValueKey } = SLIDERS[name];
  const percent = ((input.value - input.min) / (input.max - input.min)) * 100;
  input.style.setProperty('--fill', `${percent}%`);
  const value = sliderValue(form, name);
  form.querySelector(`[data-range-value="${name}"]`).textContent = value === null ? t(noValueKey) : format(value);
}
