-- V4: record why a refresh token was revoked, so a token that was just rotated can be
-- tolerated for a few seconds (parallel page loads / tabs) while logout and reuse stay strict.
ALTER TABLE refresh_tokens ADD COLUMN revoke_reason VARCHAR(20)
    CHECK (revoke_reason IN ('ROTATED', 'LOGOUT', 'REVOKED'));
