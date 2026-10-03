package com.cookify.recipe.dto;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;

import java.math.BigDecimal;
import java.util.List;

/** Everything the browse page needs to build its filters: choices, popular names and slider ranges. */
public record FilterOptionsResponse(
        List<DietType> diets,
        List<Cuisine> cuisines,
        List<Course> courses,
        List<String> ingredients,
        List<String> equipment,
        Ranges ranges) {

    public record Ranges(BigDecimal maxCost, int maxMinutes, int maxCalories, int maxProtein) {
    }
}
