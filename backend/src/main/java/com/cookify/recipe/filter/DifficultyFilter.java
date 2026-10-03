package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Difficulty at most {@code maxDifficulty} stars. */
public class DifficultyFilter extends RecipeFilter {

    private final Integer maxDifficulty;

    public DifficultyFilter(Integer maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    @Override
    public boolean isActive() {
        return maxDifficulty != null;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.le(recipe.get("difficulty"), maxDifficulty);
    }
}
