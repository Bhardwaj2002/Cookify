package com.cookify.recipe.sort;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** Most recently uploaded first. */
public class NewestSort implements SortStrategy {

    @Override
    public List<Order> orders(Root<RecipeEntity> recipe, CriteriaBuilder cb) {
        return List.of(cb.desc(recipe.get("createdAt")), SortStrategy.newestFirst(recipe, cb));
    }
}
