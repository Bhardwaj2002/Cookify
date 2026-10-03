-- V5: profile details and dietary preferences (Step 4).

-- Storage key of the current avatar (local path or S3 key) so the old file can be deleted on change.
ALTER TABLE users ADD COLUMN avatar_key VARCHAR(300);

ALTER TABLE users ADD CONSTRAINT chk_users_gender
    CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'PREFER_NOT_TO_SAY'));

ALTER TABLE users ADD CONSTRAINT chk_users_preferred_lang
    CHECK (preferred_lang IN ('en', 'hi', 'zh', 'ru', 'es', 'fr'));

-- One row per user; drives the "For You" recommendations later.
CREATE TABLE user_preferences (
    user_id       BIGINT       PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    diet_type     VARCHAR(20)  CHECK (diet_type IN ('VEG', 'VEGAN', 'JAIN', 'EGGETARIAN', 'PESCETARIAN', 'NON_VEG')),
    max_calories  INT          CHECK (max_calories BETWEEN 50 AND 5000),
    min_protein   INT          CHECK (min_protein BETWEEN 0 AND 300),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
