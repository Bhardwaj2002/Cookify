/**
 * Where the API lives.
 * - Production: frontend and API share one CloudFront origin → relative URLs ("").
 * - Local dev: frontend on Live Server (:5500), API on Spring Boot (:8080).
 */
const isLocalDevServer =
  ['localhost', '127.0.0.1'].includes(location.hostname) && location.port !== '8080';

export const API_BASE = isLocalDevServer ? `http://${location.hostname}:8080` : '';

export const ROUTES = {
  home: '/index.html',
  login: '/pages/login.html',
  signup: '/pages/signup.html',
  forgotPassword: '/pages/forgot-password.html',
  resetPassword: '/pages/reset-password.html',
  account: '/pages/account.html',
  profile: '/pages/profile.html',
  browse: '/pages/browse.html',
  recipe: '/pages/recipe.html',
  recipeEditor: '/pages/recipe-editor.html',
  admin: '/pages/admin.html',
  chat: '/pages/chat.html',
};

/** Public profile page for a username. */
export const profileUrl = (username) => `${ROUTES.profile}?u=${encodeURIComponent(username)}`;
export const recipeUrl = (id) => `${ROUTES.recipe}?id=${encodeURIComponent(id)}`;
export const editRecipeUrl = (id) => `${ROUTES.recipeEditor}?id=${encodeURIComponent(id)}`;

/** Where to land after logging in when no ?next= is given. */
export const AFTER_LOGIN = ROUTES.account;
