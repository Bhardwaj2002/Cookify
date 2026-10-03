package com.cookify.recipe.dto;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Create / replace a recipe. Media files are uploaded separately to {@code /api/recipes/{id}/media}. */
public record RecipeRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull DietType dietType,
        @NotNull Cuisine cuisine,
        @NotNull Course course,
        @NotNull @Min(0) @Max(1440) Integer prepMinutes,
        @NotNull @Min(0) @Max(1440) Integer cookMinutes,
        @NotNull @Min(1) @Max(50) Integer servings,
        @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal cost,
        @NotNull @Min(0) @Max(5000) Integer calories,
        @Min(0) @Max(300) Integer protein,
        @NotNull @Min(1) @Max(5) Integer difficulty,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull IngredientInput> ingredients,
        @Size(max = 20) List<@NotBlank @Size(max = 60) String> equipment,
        @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 1000) String> steps) {

    public record IngredientInput(@NotBlank @Size(max = 60) String name, @Size(max = 60) String quantity) {
    }
}
