-- V3: email 2FA, trusted devices and password reset (Step 3).

-- Pending login challenges. The client holds the opaque challenge id; the 6-digit code is emailed.
CREATE TABLE otp_codes (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    challenge_hash  VARCHAR(64)  NOT NULL UNIQUE,
    code_hash       VARCHAR(64)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    expires_at      TIMESTAMPTZ  NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_codes_user ON otp_codes (user_id);

-- "Remember this device": skips the OTP step for a while on a known browser.
CREATE TABLE trusted_devices (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    user_agent    VARCHAR(255),
    expires_at    TIMESTAMPTZ  NOT NULL,
    last_used_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_trusted_devices_user ON trusted_devices (user_id);

-- One-time links emailed by "Forgot password?".
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
