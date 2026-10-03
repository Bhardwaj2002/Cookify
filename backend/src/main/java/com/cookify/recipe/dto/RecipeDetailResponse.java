package com.cookify.recipe.dto;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Everything needed to show one recipe page. {@code canEdit} is true for its author and admins. */
public record RecipeDetailResponse(
        Long id,
        String title,
        String description,
        DietInfo diet,
        Cuisine cuisine,
        Course course,
        int prepMinutes,
        int cookMinutes,
        int totalMinutes,
        int speed,
        int servings,
        BigDecimal cost,
        int calories,
        Integer protein,
        int difficulty,
        List<IngredientResponse> ingredients,
        List<String> equipment,
        List<String> steps,
        List<MediaResponse> media,
        AuthorInfo author,
        long views,
        BigDecimal avgRating,
        int ratingCount,
        int commentCount,
        Instant createdAt,
        Instant updatedAt,
        boolean canEdit,
        ViewerState viewer) {

    public record IngredientResponse(String name, String quantity) {
    }
}
