package com.cookify.recipe.sort;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** How search results are ordered (strategy pattern). Each strategy ends with a stable tie-breaker. */
public interface SortStrategy {

    List<Order> orders(Root<RecipeEntity> recipe, CriteriaBuilder cb);

    static Order newestFirst(Root<RecipeEntity> recipe, CriteriaBuilder cb) {
        return cb.desc(recipe.get("id"));
    }
}
