package com.cookify.recipe.filter;

import com.cookify.recipe.dto.RecipeSearchRequest;

import java.util.List;
import java.util.stream.Stream;

/** Builds the active {@link RecipeFilter}s for a search request. */
public final class FilterFactory {

    private FilterFactory() {
    }

    public static List<RecipeFilter> from(RecipeSearchRequest r) {
        return Stream.of(
                        new TextSearchFilter(r.q()),
                        new DietFilter(r.diet()),
                        new CuisineFilter(r.cuisine()),
                        new CourseFilter(r.course()),
                        new IngredientFilter(r.ingredients(), r.ingredientMatch()),
                        new EquipmentFilter(r.equipment()),
                        new BudgetFilter(r.maxCost()),
                        new TimeFilter(r.maxMinutes()),
                        new NutritionFilter(r.maxCalories(), r.minProtein()),
                        new DifficultyFilter(r.maxDifficulty()),
                        new MinRatingFilter(r.minRating()))
                .filter(RecipeFilter::isActive)
                .toList();
    }
}
