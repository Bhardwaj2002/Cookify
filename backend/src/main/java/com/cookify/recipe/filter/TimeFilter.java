package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/** Prep + cook time at most {@code maxMinutes}. */
public class TimeFilter extends RecipeFilter {

    private final Integer maxMinutes;

    public TimeFilter(Integer maxMinutes) {
        this.maxMinutes = maxMinutes;
    }

    @Override
    public boolean isActive() {
        return maxMinutes != null;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.lessThanOrEqualTo(recipe.get("totalMinutes"), maxMinutes);
    }
}
