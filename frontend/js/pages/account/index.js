import { initPage, refreshHeader } from '../../components/layout.js';
import { $$ } from '../../core/dom.js';
import { enablePasswordToggles } from '../../core/forms.js';
import { renderProfileCard } from './profileCard.js';
import { initPreferencesTab } from './preferencesTab.js';
import { initProfileTab } from './profileTab.js';
import { initSecurityTab } from './securityTab.js';

/** Account settings: three tabs (Profile / Preferences / Security), selected via the URL hash. */

const user = await initPage({ access: 'user' });
enablePasswordToggles();

const onUserChange = (updated) => {
  renderProfileCard(updated);
  refreshHeader(updated);
};

// ---- Tabs ----
const TABS = ['profile', 'preferences', 'security'];

function selectTab(name, { focus = false } = {}) {
  const active = TABS.includes(name) ? name : 'profile';
  for (const tab of $$('[role="tab"]')) {
    const selected = tab.dataset.tab === active;
    tab.setAttribute('aria-selected', String(selected));
    tab.tabIndex = selected ? 0 : -1;
    if (selected && focus) tab.focus();
  }
  for (const panel of $$('[role="tabpanel"]')) panel.hidden = panel.id !== `panel-${active}`;
  if (location.hash.slice(1) !== active) history.replaceState(null, '', `#${active}`);
}

for (const tab of $$('[role="tab"]')) {
  tab.addEventListener('click', () => selectTab(tab.dataset.tab));
  tab.addEventListener('keydown', (e) => {
    const step = { ArrowRight: 1, ArrowLeft: -1 }[e.key];
    if (!step) return;
    const next = TABS[(TABS.indexOf(tab.dataset.tab) + step + TABS.length) % TABS.length];
    selectTab(next, { focus: true });
  });
}
window.addEventListener('hashchange', () => selectTab(location.hash.slice(1)));
selectTab(location.hash.slice(1));

// ---- Content ----
renderProfileCard(user);
initProfileTab(user, onUserChange);
await Promise.all([initPreferencesTab(), initSecurityTab(user, onUserChange)]);
