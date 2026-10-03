package com.cookify.recipe.dto;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;
import com.cookify.recipe.filter.IngredientMatch;
import com.cookify.recipe.sort.RecipeSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Query parameters of {@code GET /api/recipes}. Every filter is optional; lists accept
 * comma-separated values ({@code ingredients=paneer,spinach}) or repeated parameters.
 */
public record RecipeSearchRequest(
        @Schema(description = "Keywords: title, description or ingredient") @Size(max = 100) String q,
        @Schema(description = "Show what someone on this diet can eat (VEG includes vegan and Jain)") DietType diet,
        List<Cuisine> cuisine,
        List<Course> course,
        @Schema(description = "Ingredients you have / want") @Size(max = 20) List<@Size(max = 60) String> ingredients,
        @Schema(description = "ALL = uses all listed; ONLY = needs nothing else (pantry staples assumed)") IngredientMatch ingredientMatch,
        @Schema(description = "Utensils you have; recipes needing other equipment are hidden") @Size(max = 20) List<@Size(max = 60) String> equipment,
        @DecimalMin("0") @DecimalMax("100000") BigDecimal maxCost,
        @Min(1) @Max(1440) Integer maxMinutes,
        @Min(0) @Max(5000) Integer maxCalories,
        @Min(0) @Max(300) Integer minProtein,
        @Min(1) @Max(5) Integer maxDifficulty,
        @DecimalMin("0") @DecimalMax("5") BigDecimal minRating,
        RecipeSort sort,
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size) {

    public RecipeSort sortOrDefault() {
        return sort == null ? RecipeSort.POPULAR : sort;
    }

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 12 : size;
    }
}
