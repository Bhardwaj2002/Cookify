-- V8: ratings, comments, saved recipes and follows (Step 7).

-- Running sum so the average can be updated atomically without re-reading every rating.
ALTER TABLE recipes ADD COLUMN rating_sum BIGINT NOT NULL DEFAULT 0;
UPDATE recipes SET rating_sum = ROUND(avg_rating * rating_count);
ALTER TABLE recipes ADD COLUMN comment_count INT NOT NULL DEFAULT 0;

CREATE TABLE ratings (
    id          BIGSERIAL    PRIMARY KEY,
    recipe_id   BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score       SMALLINT     NOT NULL CHECK (score BETWEEN 1 AND 5),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (recipe_id, user_id)
);
CREATE INDEX idx_ratings_user ON ratings (user_id);

CREATE TABLE comments (
    id          BIGSERIAL     PRIMARY KEY,
    recipe_id   BIGINT        NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body        VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_recipe ON comments (recipe_id, created_at DESC);
CREATE INDEX idx_comments_user   ON comments (user_id);

CREATE TABLE saved_recipes (
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id   BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, recipe_id)
);

-- "Subscribe" in the mockup: followers get the followee's new recipes (emails in Step 9).
CREATE TABLE follows (
    follower_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX idx_follows_followee ON follows (followee_id);
