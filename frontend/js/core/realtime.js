import { refreshSession, tokenStore } from './api.js';
import { API_BASE } from './config.js';

/**
 * Live chat connection (STOMP over WebSocket on /ws). Requires the StompJs UMD bundle on the page.
 * Authenticates with the access token in the CONNECT frame and reconnects automatically,
 * fetching a fresh token first (access tokens only live 15 minutes).
 */

const WS_URL = API_BASE
  ? `${API_BASE.replace(/^http/, 'ws')}/ws`
  : `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;

let client = null;
const listeners = { message: new Set(), typing: new Set(), error: new Set(), status: new Set() };

export function connectRealtime() {
  if (client) return client;
  client = new window.StompJs.Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    heartbeatIncoming: 20000,
    heartbeatOutgoing: 20000,
    beforeConnect: async () => {
      const session = await refreshSession();
      const token = session?.accessToken ?? tokenStore.get();
      client.connectHeaders = { Authorization: `Bearer ${token}` };
    },
    onConnect: () => {
      client.subscribe('/user/queue/messages', (frame) => emit('message', JSON.parse(frame.body)));
      client.subscribe('/user/queue/typing', (frame) => emit('typing', JSON.parse(frame.body)));
      client.subscribe('/user/queue/errors', (frame) => emit('error', JSON.parse(frame.body)));
      emit('status', 'online');
    },
    onWebSocketClose: () => emit('status', 'offline'),
    onStompError: () => emit('status', 'offline'),
  });
  client.activate();
  return client;
}

export function onRealtime(event, callback) {
  listeners[event].add(callback);
  return () => listeners[event].delete(callback);
}

/** Tells the other member "… is typing" (throttled by the caller). */
export function sendTyping(conversationId) {
  if (client?.connected) {
    client.publish({ destination: '/app/chat.typing', body: JSON.stringify({ conversationId }) });
  }
}

function emit(event, payload) {
  listeners[event].forEach((callback) => callback(payload));
}
