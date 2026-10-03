-- V7: search, filters and popularity (Step 6).

-- Total time as a real column so it can be filtered, indexed and sorted on.
ALTER TABLE recipes ADD COLUMN total_minutes INT GENERATED ALWAYS AS (prep_minutes + cook_minutes) STORED;

-- Full-text search over title (weight A) and description (weight B), with English stemming
-- ("tomatoes" finds "tomato").
ALTER TABLE recipes ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(description, '')), 'B')
) STORED;

CREATE INDEX idx_recipes_search     ON recipes USING GIN (search_vector);
CREATE INDEX idx_recipes_diet       ON recipes (diet_type);
CREATE INDEX idx_recipes_cuisine    ON recipes (cuisine);
CREATE INDEX idx_recipes_course     ON recipes (course);
CREATE INDEX idx_recipes_cost       ON recipes (cost);
CREATE INDEX idx_recipes_total_time ON recipes (total_minutes);
CREATE INDEX idx_recipes_calories   ON recipes (calories);
CREATE INDEX idx_recipes_rating     ON recipes (avg_rating DESC, rating_count DESC);
CREATE INDEX idx_ingredients_name_trgm ON ingredients USING GIN (name gin_trgm_ops);

-- Keyword match used by the search box: full-text on title/description, plus partial matches on
-- the title ("pan" → "Paneer Tikka") and on ingredient names ("tofu" finds dishes using tofu).
CREATE FUNCTION recipe_matches_text(p_recipe_id BIGINT, p_query TEXT) RETURNS BOOLEAN
    LANGUAGE sql STABLE AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipes r
        WHERE r.id = p_recipe_id
          AND (r.search_vector @@ websearch_to_tsquery('english', p_query)
               OR r.title ILIKE '%' || p_query || '%')
    ) OR EXISTS (
        SELECT 1 FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id
        WHERE ri.recipe_id = p_recipe_id AND i.name ILIKE '%' || p_query || '%'
    )
$$;

-- "Trending" score: quality (rating weighted by how many people rated) + reach (views),
-- minus a small penalty per week of age so new recipes get a chance.
CREATE FUNCTION recipe_popularity(p_avg_rating NUMERIC, p_rating_count INT, p_views BIGINT, p_created_at TIMESTAMPTZ)
    RETURNS DOUBLE PRECISION
    LANGUAGE sql STABLE AS $$
    SELECT 2.0 * p_avg_rating::double precision * ln(1 + p_rating_count)
         + ln(1 + p_views)
         - 0.5 * EXTRACT(EPOCH FROM (now() - p_created_at)) / 604800.0
$$;
