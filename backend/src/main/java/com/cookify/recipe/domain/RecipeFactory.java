package com.cookify.recipe.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Creates the {@link Recipe} subclass matching a spec's diet type (factory pattern). */
public final class RecipeFactory {

    private RecipeFactory() {
    }

    public static Recipe create(RecipeSpec spec) {
        return switch (spec.dietType()) {
            case VEG -> new VegRecipe(spec);
            case VEGAN -> new VeganRecipe(spec);
            case JAIN -> new JainRecipe(spec);
            case EGGETARIAN -> new EggetarianRecipe(spec);
            case PESCETARIAN -> new PescetarianRecipe(spec);
            case NON_VEG -> new NonVegRecipe(spec);
        };
    }

    /**
     * Recipe diets someone with {@code preference} can eat, asked of each subclass in turn
     * (used by the diet filter and recommendations). E.g. VEG → {VEG, VEGAN, JAIN}.
     */
    public static Set<DietType> dietsSuitableFor(DietType preference) {
        return Arrays.stream(DietType.values())
                .filter(diet -> create(RecipeSpec.prototype(diet)).isSuitableFor(preference))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DietType.class)));
    }
}
