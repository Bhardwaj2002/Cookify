package com.cookify.recipe.sort;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** "Trending": rating × number of ratings + views, decaying with age (SQL function {@code recipe_popularity}). */
public class PopularitySort implements SortStrategy {

    @Override
    public List<Order> orders(Root<RecipeEntity> recipe, CriteriaBuilder cb) {
        return List.of(
                cb.desc(cb.function("recipe_popularity", Double.class,
                        recipe.get("avgRating"), recipe.get("ratingCount"), recipe.get("views"), recipe.get("createdAt"))),
                SortStrategy.newestFirst(recipe, cb));
    }
}
