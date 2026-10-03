package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

/** Per-serving calories at most {@code maxCalories} and/or protein at least {@code minProtein}. */
public class NutritionFilter extends RecipeFilter {

    private final Integer maxCalories;
    private final Integer minProtein;

    public NutritionFilter(Integer maxCalories, Integer minProtein) {
        this.maxCalories = maxCalories;
        this.minProtein = minProtein;
    }

    @Override
    public boolean isActive() {
        return maxCalories != null || minProtein != null;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> parts = new ArrayList<>();
        if (maxCalories != null) {
            parts.add(cb.lessThanOrEqualTo(recipe.get("calories"), maxCalories));
        }
        if (minProtein != null) {
            parts.add(cb.greaterThanOrEqualTo(recipe.get("protein"), minProtein));
        }
        return cb.and(parts.toArray(Predicate[]::new));
    }
}
