package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Search box: full-text on title/description, partial title match and ingredient names (SQL function from V7). */
public class TextSearchFilter extends RecipeFilter {

    private final String query;

    public TextSearchFilter(String query) {
        this.query = query == null ? null : query.trim();
    }

    @Override
    public boolean isActive() {
        return query != null && !query.isEmpty();
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> q, CriteriaBuilder cb) {
        return cb.isTrue(cb.function("recipe_matches_text", Boolean.class, recipe.get("id"), cb.literal(query)));
    }
}
