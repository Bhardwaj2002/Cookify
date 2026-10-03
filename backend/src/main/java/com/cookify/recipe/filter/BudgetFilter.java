package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;

/** Total cost at most {@code maxCost} (INR). */
public class BudgetFilter extends RecipeFilter {

    private final BigDecimal maxCost;

    public BudgetFilter(BigDecimal maxCost) {
        this.maxCost = maxCost;
    }

    @Override
    public boolean isActive() {
        return maxCost != null;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.lessThanOrEqualTo(recipe.get("cost"), maxCost);
    }
}
