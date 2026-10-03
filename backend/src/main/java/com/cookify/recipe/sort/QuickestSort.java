package com.cookify.recipe.sort;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** Shortest prep + cook time first. */
public class QuickestSort implements SortStrategy {

    @Override
    public List<Order> orders(Root<RecipeEntity> recipe, CriteriaBuilder cb) {
        return List.of(cb.asc(recipe.get("totalMinutes")), SortStrategy.newestFirst(recipe, cb));
    }
}
