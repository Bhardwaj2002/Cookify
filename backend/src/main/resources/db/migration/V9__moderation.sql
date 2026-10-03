-- V9: moderation — warnings and bans (Step 8).

CREATE TABLE warnings (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason           VARCHAR(40)  NOT NULL,          -- e.g. ABUSIVE_COMMENT, ABUSIVE_RECIPE
    content_snippet  VARCHAR(200),                   -- what was blocked (for admins)
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_warnings_user ON warnings (user_id, created_at DESC);

ALTER TABLE users ADD COLUMN ban_reason VARCHAR(200);
ALTER TABLE users ADD COLUMN banned_at  TIMESTAMPTZ;
CREATE INDEX idx_users_banned ON users (banned_until) WHERE banned_until IS NOT NULL OR permanently_banned;
