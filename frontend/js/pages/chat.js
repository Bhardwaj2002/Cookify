import { avatarHtml } from '../components/avatar.js';
import { initPage, setChatBadge } from '../components/layout.js';
import { toast } from '../components/toast.js';
import { api } from '../core/api.js';
import { ROUTES, profileUrl } from '../core/config.js';
import { $, displayName, escapeHtml } from '../core/dom.js';
import { formatDate, timeAgo } from '../core/format.js';
import { t, tError } from '../core/i18n.js';
import { connectRealtime, onRealtime, sendTyping } from '../core/realtime.js';

/**
 * Chat: /pages/chat.html?c=<conversationId> opens a conversation, ?with=<username> starts one.
 * History comes from REST; new messages and typing hints arrive live over WebSocket.
 */

const me = await initPage({ access: 'user' });
const PAGE = 30;

let conversations = [];
let activeId = null;
let oldestId = null;
const shownIds = new Set();

const chatEl = $('#chat');
const inbox = $('#inbox');
const messagesEl = $('#messages');
const composer = $('#composer');
const time = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' });

// ---- Inbox ------------------------------------------------------------------------------

async function loadInbox() {
  conversations = await api.get('/api/chat/conversations');
  renderInbox();
}

function renderInbox() {
  if (!conversations.length) {
    inbox.innerHTML = `<li class="empty">${escapeHtml(t('chat.noConversations'))}<br>
      <a href="${ROUTES.browse}">${escapeHtml(t('chat.findCooks'))}</a></li>`;
  } else {
    inbox.innerHTML = conversations.map((c) => `
      <li class="inbox__item ${c.unreadCount ? 'is-unread' : ''}">
        <a href="?c=${c.id}" data-open="${c.id}" aria-current="${c.id === activeId}">
          ${avatarHtml(c.with)}
          <span style="min-width:0">
            <span class="inbox__name">${escapeHtml(displayName(c.with))}</span>
            <span class="inbox__snippet">${c.lastMessage
              ? escapeHtml((c.lastMessage.sender.id === me.id ? t('chat.you') + ': ' : '') + c.lastMessage.body)
              : escapeHtml(t('chat.sayHi'))}</span>
          </span>
          <span class="inbox__meta">
            ${c.lastMessageAt ? `<span>${escapeHtml(timeAgo(c.lastMessageAt))}</span>` : ''}
            ${c.unreadCount ? `<span class="unread-badge">${c.unreadCount}</span>` : ''}
          </span>
        </a>
      </li>`).join('');
  }
  setChatBadge(conversations.reduce((sum, c) => sum + (c.id === activeId ? 0 : c.unreadCount), 0));
}

inbox.addEventListener('click', (e) => {
  const link = e.target.closest('[data-open]');
  if (!link) return;
  e.preventDefault();
  openConversation(Number(link.dataset.open));
});

// ---- Thread -------------------------------------------------------------------------------

async function openConversation(id, { push = true } = {}) {
  activeId = id;
  oldestId = null;
  shownIds.clear();
  const convo = conversations.find((c) => c.id === id);
  if (!convo) return;
  if (push) history.replaceState(null, '', `?c=${id}`);

  $('#thread-empty').hidden = true;
  for (const el of ['#thread-head', '#messages', '#composer']) $(el).hidden = false;
  $('#typing').hidden = true;
  chatEl.classList.add('is-thread-open');
  $('#thread-head').innerHTML = `
    <button type="button" class="icon-btn thread__back" data-back aria-label="${escapeHtml(t('chat.back'))}">←</button>
    <a href="${profileUrl(convo.with.username)}">${avatarHtml(convo.with)}</a>
    <a href="${profileUrl(convo.with.username)}">${escapeHtml(displayName(convo.with))}
      <span class="text-muted" style="font-weight:400">@${escapeHtml(convo.with.username)}</span></a>`;
  $('#thread-head [data-back]').addEventListener('click', () => {
    chatEl.classList.remove('is-thread-open');
    activeId = null;
    history.replaceState(null, '', location.pathname);
    renderInbox();
  });

  messagesEl.innerHTML = '';
  const page = await api.get(`/api/chat/conversations/${id}/messages?size=${PAGE}`);
  renderMessages(page, { prepend: false });
  messagesEl.scrollTop = messagesEl.scrollHeight;
  composer.body.focus();
  await markRead(convo);
}

function bubbleHtml(m) {
  const mine = m.sender.id === me.id;
  return `<div class="bubble ${mine ? 'is-mine' : ''}" data-id="${m.id}">
    <p class="bubble__text">${escapeHtml(m.body)}</p>
    <time class="bubble__time" datetime="${escapeHtml(m.createdAt)}">${escapeHtml(time.format(new Date(m.createdAt)))}</time>
  </div>`;
}

/** Renders messages with a date divider whenever the day changes. */
function renderMessages(list, { prepend }) {
  const fresh = list.filter((m) => !shownIds.has(m.id));
  fresh.forEach((m) => shownIds.add(m.id));
  if (!fresh.length) return;
  let lastDay = prepend ? null : messagesEl.querySelector('.bubble:last-of-type time')?.dateTime?.slice(0, 10);
  let html = '';
  for (const m of fresh) {
    const day = m.createdAt.slice(0, 10);
    if (day !== lastDay) html += `<div class="day-divider">${escapeHtml(formatDate(m.createdAt))}</div>`;
    lastDay = day;
    html += bubbleHtml(m);
  }
  if (prepend) {
    const before = messagesEl.scrollHeight;
    messagesEl.querySelector('.thread__older')?.remove();
    messagesEl.insertAdjacentHTML('afterbegin', html);
    messagesEl.scrollTop += messagesEl.scrollHeight - before;
  } else {
    messagesEl.insertAdjacentHTML('beforeend', html);
  }
  oldestId = Math.min(oldestId ?? Infinity, ...fresh.map((m) => m.id));
  if (list.length === PAGE && !messagesEl.querySelector('.thread__older')) {
    messagesEl.insertAdjacentHTML('afterbegin',
      `<button type="button" class="btn btn--ghost btn--sm thread__older">${escapeHtml(t('chat.loadOlder'))}</button>`);
    messagesEl.querySelector('.thread__older').addEventListener('click', loadOlder);
  }
}

async function loadOlder() {
  const page = await api.get(`/api/chat/conversations/${activeId}/messages?size=${PAGE}&beforeId=${oldestId}`);
  messagesEl.querySelector('.thread__older')?.remove();
  renderMessages(page, { prepend: true });
}

async function markRead(convo) {
  if (!convo.unreadCount) return;
  convo.unreadCount = 0;
  renderInbox();
  await api.post(`/api/chat/conversations/${convo.id}/read`).catch(() => {});
}

// ---- Composer -----------------------------------------------------------------------------

let lastTyping = 0;
composer.body.addEventListener('input', () => {
  composer.body.style.height = 'auto';
  composer.body.style.height = `${Math.min(composer.body.scrollHeight, 140)}px`;
  $('#composer-error').hidden = true;
  if (activeId && Date.now() - lastTyping > 2000) {
    lastTyping = Date.now();
    sendTyping(activeId);
  }
});

composer.body.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    composer.requestSubmit();
  }
});

composer.addEventListener('submit', async (e) => {
  e.preventDefault();
  const body = composer.body.value.trim();
  if (!body || !activeId) return;
  const button = composer.querySelector('[type="submit"]');
  button.disabled = true;
  try {
    const sent = await api.post(`/api/chat/conversations/${activeId}/messages`, { body });
    composer.reset();
    composer.body.style.height = 'auto';
    onMessage(sent);
  } catch (error) {
    $('#composer-error').textContent = tError(error.code) ?? error.message;
    $('#composer-error').hidden = false;
  } finally {
    button.disabled = false;
    composer.body.focus();
  }
});

// ---- Live updates ---------------------------------------------------------------------------

/** Handles a message from REST (own) or WebSocket (anyone); duplicates are ignored. */
function onMessage(m) {
  let convo = conversations.find((c) => c.id === m.conversationId);
  if (!convo) {
    loadInbox(); // a brand-new conversation started by someone else
    return;
  }
  convo.lastMessage = m;
  convo.lastMessageAt = m.createdAt;
  conversations = [convo, ...conversations.filter((c) => c !== convo)];

  if (m.conversationId === activeId) {
    const nearBottom = messagesEl.scrollHeight - messagesEl.scrollTop - messagesEl.clientHeight < 120;
    renderMessages([m], { prepend: false });
    if (nearBottom || m.sender.id === me.id) messagesEl.scrollTop = messagesEl.scrollHeight;
    $('#typing').hidden = true;
    if (m.sender.id !== me.id && document.visibilityState === 'visible') {
      api.post(`/api/chat/conversations/${m.conversationId}/read`).catch(() => {});
    }
  } else if (m.sender.id !== me.id && !shownIds.has(m.id)) {
    convo.unreadCount += 1;
    toast(t('chat.newFrom', { name: displayName(m.sender) }));
  }
  renderInbox();
}

let typingTimer;
onRealtime('message', onMessage);
onRealtime('typing', (event) => {
  if (event.conversationId !== activeId) return;
  const typing = $('#typing');
  typing.textContent = t('chat.typing', { name: event.username });
  typing.hidden = false;
  clearTimeout(typingTimer);
  typingTimer = setTimeout(() => (typing.hidden = true), 3000);
});
onRealtime('error', (error) => toast(tError(error.error?.code) ?? error.error?.message ?? 'Chat error', 'error'));
onRealtime('status', (status) => {
  const el = $('#connection');
  el.classList.toggle('is-online', status === 'online');
  el.textContent = t(status === 'online' ? 'chat.live' : 'chat.offline');
});

// ---- Start ------------------------------------------------------------------------------------

connectRealtime();
await loadInbox();

const params = new URLSearchParams(location.search);
if (params.get('with')) {
  try {
    const convo = await api.post('/api/chat/conversations', { username: params.get('with') });
    if (!conversations.some((c) => c.id === convo.id)) conversations.unshift(convo);
    renderInbox();
    await openConversation(convo.id);
  } catch (error) {
    toast(tError(error.code) ?? error.message, 'error');
  }
} else if (params.get('c')) {
  await openConversation(Number(params.get('c')), { push: false });
}
