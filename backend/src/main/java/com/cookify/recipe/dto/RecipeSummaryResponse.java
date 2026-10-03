package com.cookify.recipe.dto;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;

import java.math.BigDecimal;
import java.time.Instant;

/** Recipe card data for lists and grids. */
public record RecipeSummaryResponse(
        Long id,
        String title,
        DietInfo diet,
        Cuisine cuisine,
        Course course,
        int totalMinutes,
        int speed,
        int difficulty,
        BigDecimal cost,
        int calories,
        String coverUrl,
        AuthorInfo author,
        long views,
        BigDecimal avgRating,
        int ratingCount,
        int commentCount,
        Instant createdAt) {
}
