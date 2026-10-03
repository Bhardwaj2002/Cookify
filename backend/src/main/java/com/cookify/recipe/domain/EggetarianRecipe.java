package com.cookify.recipe.domain;

import java.util.EnumSet;
import java.util.Set;

/** Vegetarian plus eggs. */
public class EggetarianRecipe extends Recipe {

    private static final Set<DietType> SUITABLE_FOR = EnumSet.of(DietType.EGGETARIAN, DietType.PESCETARIAN, DietType.NON_VEG);

    public EggetarianRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.EGGETARIAN;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Eggetarian", "🥚", "#D4A017");
    }

    @Override
    public boolean isSuitableFor(DietType preference) {
        return preference == null || SUITABLE_FOR.contains(preference);
    }

    @Override
    protected Set<String> forbiddenIngredients() {
        return union(IngredientRules.MEAT, IngredientRules.SEAFOOD);
    }
}
