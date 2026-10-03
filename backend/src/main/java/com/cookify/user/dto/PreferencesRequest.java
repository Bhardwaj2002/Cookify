package com.cookify.user.dto;

import com.cookify.recipe.domain.DietType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Dietary preferences; {@code null} means "no preference". */
public record PreferencesRequest(
        DietType dietType,
        @Min(50) @Max(5000) Integer maxCalories,
        @Min(0) @Max(300) Integer minProtein) {
}
