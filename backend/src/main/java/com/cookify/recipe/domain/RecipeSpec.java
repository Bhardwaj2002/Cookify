package com.cookify.recipe.domain;

import java.math.BigDecimal;
import java.util.List;

/** Plain description of a recipe, independent of storage. Input to {@link RecipeFactory}. */
public record RecipeSpec(
        String title,
        String description,
        DietType dietType,
        Cuisine cuisine,
        Course course,
        int prepMinutes,
        int cookMinutes,
        int servings,
        BigDecimal cost,
        int calories,
        Integer protein,
        int difficulty,
        List<IngredientLine> ingredients,
        List<String> equipment,
        List<String> steps) {

    public RecipeSpec {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        equipment = equipment == null ? List.of() : List.copyOf(equipment);
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    /** Minimal spec used to ask a diet's class questions such as {@code isSuitableFor}. */
    static RecipeSpec prototype(DietType dietType) {
        return new RecipeSpec("", null, dietType, Cuisine.OTHER, Course.MAIN, 0, 0, 1, BigDecimal.ZERO, 0, null, 1,
                List.of(), List.of(), List.of());
    }
}
