import { API_BASE } from './config.js';

/**
 * Thin fetch wrapper for the Cookify API.
 *
 * Auth model: the short-lived access token is kept in sessionStorage (per tab) and sent as a
 * Bearer header; the long-lived refresh token is an httpOnly cookie the browser sends to
 * /api/auth/* automatically. On a 401 the client refreshes once and retries.
 */

const TOKEN_KEY = 'cookify.accessToken';

/** Error thrown for any non-2xx response, carrying the API's { error: { code, message, fields } }. */
export class ApiError extends Error {
  constructor(status, error = {}) {
    super(error.message || `Request failed (${status})`);
    this.status = status;
    this.code = error.code || 'UNKNOWN';
    this.fields = error.fields || [];
  }
}

export const tokenStore = {
  get: () => safeStorage(() => sessionStorage.getItem(TOKEN_KEY)),
  set: (token) => safeStorage(() => sessionStorage.setItem(TOKEN_KEY, token)),
  clear: () => safeStorage(() => sessionStorage.removeItem(TOKEN_KEY)),
};

let refreshInFlight = null;

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share one request.
 * @returns {Promise<object|null>} the AuthResponse, or null if there is no valid session
 */
export function refreshSession() {
  refreshInFlight ??= fetch(`${API_BASE}/api/auth/refresh`, { method: 'POST', credentials: 'include' })
    .then(async (res) => {
      if (!res.ok) {
        tokenStore.clear();
        return null;
      }
      const body = await res.json();
      tokenStore.set(body.accessToken);
      return body;
    })
    .catch(() => null)
    .finally(() => {
      refreshInFlight = null;
    });
  return refreshInFlight;
}

async function request(method, path, { body, auth = true, retry = true } = {}) {
  const isForm = body instanceof FormData;
  const headers = { Accept: 'application/json' };
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';

  const token = auth ? tokenStore.get() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      credentials: 'include',
      body: body === undefined || isForm ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Cannot reach the server. Is the backend running?' });
  }

  if (res.status === 401 && auth && retry) {
    const session = await refreshSession();
    if (session) return request(method, path, { body, auth, retry: false });
  }

  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) {
    const error = new ApiError(res.status, data?.error);
    if (error.code === 'ACCOUNT_BANNED' && token) endBannedSession();
    throw error;
  }
  return data;
}

/** A logged-in user was just suspended: drop the session and show the login page with a notice. */
function endBannedSession() {
  tokenStore.clear();
  safeStorage(() => localStorage.removeItem('cookify.hasSession'));
  if (!location.pathname.endsWith('/login.html')) location.href = '/pages/login.html?banned=1';
}

export const api = {
  get: (path, opts) => request('GET', path, opts),
  post: (path, body, opts) => request('POST', path, { ...opts, body }),
  put: (path, body, opts) => request('PUT', path, { ...opts, body }),
  delete: (path, opts) => request('DELETE', path, opts),
  /** multipart/form-data upload; the browser sets the boundary header. */
  upload: (path, formData, opts) => request('POST', path, { ...opts, body: formData }),
};

function safeStorage(fn) {
  try {
    return fn();
  } catch {
    return null;
  }
}
