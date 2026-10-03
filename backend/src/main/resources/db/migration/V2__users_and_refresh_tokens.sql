-- V2: user accounts and refresh tokens (Step 2: auth core).

CREATE TABLE users (
    id                  BIGSERIAL    PRIMARY KEY,
    username            VARCHAR(30)  NOT NULL,           -- case preserved for display, unique ignoring case
    email               VARCHAR(254) NOT NULL,           -- stored lower-case
    phone               VARCHAR(20),
    password_hash       VARCHAR(100) NOT NULL,           -- BCrypt
    first_name          VARCHAR(60),
    last_name           VARCHAR(60),
    age                 SMALLINT     CHECK (age BETWEEN 13 AND 120),
    gender              VARCHAR(20),
    avatar_url          VARCHAR(500),
    bio                 VARCHAR(500),
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    twofa_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    warnings_count      INT          NOT NULL DEFAULT 0,
    banned_until        TIMESTAMPTZ,
    permanently_banned  BOOLEAN      NOT NULL DEFAULT FALSE,
    preferred_lang      VARCHAR(5)   NOT NULL DEFAULT 'en',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_username_ci ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email_ci    ON users (lower(email));
CREATE UNIQUE INDEX uq_users_phone       ON users (phone) WHERE phone IS NOT NULL;

-- Only a SHA-256 hash of each refresh token is stored; the raw token lives in the user's httpOnly cookie.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    user_agent  VARCHAR(255),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
