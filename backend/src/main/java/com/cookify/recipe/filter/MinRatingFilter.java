package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;

/** Average rating at least {@code minRating} (unrated recipes are excluded). */
public class MinRatingFilter extends RecipeFilter {

    private final BigDecimal minRating;

    public MinRatingFilter(BigDecimal minRating) {
        this.minRating = minRating;
    }

    @Override
    public boolean isActive() {
        return minRating != null && minRating.signum() > 0;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.and(cb.greaterThanOrEqualTo(recipe.get("avgRating"), minRating), cb.gt(recipe.get("ratingCount"), 0));
    }
}
