import { api, refreshSession, tokenStore } from './api.js';
import { AFTER_LOGIN, ROUTES } from './config.js';

/** Session helpers shared by every page. */

/**
 * Non-secret hint that this browser has logged in before, so guest visitors don't fire a
 * refresh request (and a 401) on every page load. The real session is the httpOnly cookie.
 */
const SESSION_HINT_KEY = 'cookify.hasSession';
const sessionHint = {
  get: () => attempt(() => localStorage.getItem(SESSION_HINT_KEY) === '1'),
  set: () => attempt(() => localStorage.setItem(SESSION_HINT_KEY, '1')),
  clear: () => attempt(() => localStorage.removeItem(SESSION_HINT_KEY)),
};

let currentUserPromise = null;

/**
 * Resolves to the logged-in user, or null. Uses the stored access token if present,
 * otherwise tries the refresh cookie. Cached for the lifetime of the page.
 */
export function getCurrentUser() {
  currentUserPromise ??= (async () => {
    if (!tokenStore.get()) {
      if (!sessionHint.get()) return null;
      const session = await refreshSession();
      if (!session) {
        sessionHint.clear();
        return null;
      }
      return session.user;
    }
    try {
      return await api.get('/api/users/me');
    } catch {
      return null;
    }
  })();
  return currentUserPromise;
}

/** Saves the tokens from a signup / login / verify response. */
export function startSession(authResponse) {
  tokenStore.set(authResponse.accessToken);
  sessionHint.set();
  currentUserPromise = Promise.resolve(authResponse.user);
}

export async function logout() {
  try {
    await api.post('/api/auth/logout', undefined, { auth: false });
  } finally {
    tokenStore.clear();
    sessionHint.clear();
    currentUserPromise = Promise.resolve(null);
    location.href = ROUTES.login;
  }
}

/** For protected pages: returns the user or redirects to login (coming back afterwards). */
export async function requireUser() {
  const user = await getCurrentUser();
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search);
    location.replace(`${ROUTES.login}?next=${next}`);
    return new Promise(() => {}); // halt the page script while navigating away
  }
  return user;
}

/** For login/signup pages: already logged in → skip ahead. */
export async function redirectIfLoggedIn() {
  if (await getCurrentUser()) location.replace(nextUrl());
}

/** The ?next= target after login, restricted to same-site paths. */
export function nextUrl() {
  const next = new URLSearchParams(location.search).get('next');
  return next && next.startsWith('/') && !next.startsWith('//') ? next : AFTER_LOGIN;
}

function attempt(fn) {
  try {
    return fn();
  } catch {
    return null;
  }
}
