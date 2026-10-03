-- V6: recipes, ingredients, equipment and media (Step 5).

CREATE TABLE recipes (
    id            BIGSERIAL     PRIMARY KEY,
    author_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title         VARCHAR(120)  NOT NULL,
    description   VARCHAR(1000),
    diet_type     VARCHAR(20)   NOT NULL CHECK (diet_type IN ('VEG', 'VEGAN', 'JAIN', 'EGGETARIAN', 'PESCETARIAN', 'NON_VEG')),
    cuisine       VARCHAR(30)   NOT NULL,
    course        VARCHAR(30)   NOT NULL,
    prep_minutes  INT           NOT NULL CHECK (prep_minutes BETWEEN 0 AND 1440),
    cook_minutes  INT           NOT NULL CHECK (cook_minutes BETWEEN 0 AND 1440),
    servings      INT           NOT NULL CHECK (servings BETWEEN 1 AND 50),
    cost          NUMERIC(10,2) NOT NULL CHECK (cost >= 0),          -- total cost in INR
    calories      INT           NOT NULL CHECK (calories BETWEEN 0 AND 5000), -- per serving
    protein       INT           CHECK (protein BETWEEN 0 AND 300),   -- grams per serving
    difficulty    SMALLINT      NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    steps         JSONB         NOT NULL,                            -- ordered list of instruction strings
    views         BIGINT        NOT NULL DEFAULT 0,
    avg_rating    NUMERIC(3,2)  NOT NULL DEFAULT 0,
    rating_count  INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_recipes_author  ON recipes (author_id);
CREATE INDEX idx_recipes_created ON recipes (created_at DESC);

-- Shared, normalised (lower-case) ingredient and equipment names: these power the
-- "what's in my kitchen" filters in Step 6.
CREATE TABLE ingredients (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(60)  NOT NULL UNIQUE
);

CREATE TABLE recipe_ingredients (
    id             BIGSERIAL    PRIMARY KEY,
    recipe_id      BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    ingredient_id  BIGINT       NOT NULL REFERENCES ingredients (id),
    quantity       VARCHAR(60),
    position       INT          NOT NULL
);
CREATE INDEX idx_recipe_ingredients_recipe     ON recipe_ingredients (recipe_id);
CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients (ingredient_id);

CREATE TABLE equipment (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(60)  NOT NULL UNIQUE
);

CREATE TABLE recipe_equipment (
    recipe_id     BIGINT  NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    equipment_id  BIGINT  NOT NULL REFERENCES equipment (id),
    PRIMARY KEY (recipe_id, equipment_id)
);
CREATE INDEX idx_recipe_equipment_equipment ON recipe_equipment (equipment_id);

CREATE TABLE recipe_media (
    id           BIGSERIAL    PRIMARY KEY,
    recipe_id    BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    media_type   VARCHAR(10)  NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    storage_key  VARCHAR(300) NOT NULL,
    url          VARCHAR(500) NOT NULL,
    position     INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_recipe_media_recipe ON recipe_media (recipe_id);
