package com.cookify.recipe.sort;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** Highest average rating first; more ratings wins a tie. */
public class TopRatedSort implements SortStrategy {

    @Override
    public List<Order> orders(Root<RecipeEntity> recipe, CriteriaBuilder cb) {
        return List.of(cb.desc(recipe.get("avgRating")), cb.desc(recipe.get("ratingCount")),
                SortStrategy.newestFirst(recipe, cb));
    }
}
