package com.cookify.recipe.domain;

import java.util.EnumSet;
import java.util.Set;

/** Lacto-vegetarian: no meat, seafood or eggs; dairy allowed. */
public class VegRecipe extends Recipe {

    /** Everyone except vegans and Jains (who have stricter rules) can eat a plain veg dish. */
    private static final Set<DietType> SUITABLE_FOR =
            EnumSet.of(DietType.VEG, DietType.EGGETARIAN, DietType.PESCETARIAN, DietType.NON_VEG);

    public VegRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.VEG;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Vegetarian", "🥦", "#3E8E5A");
    }

    @Override
    public boolean isSuitableFor(DietType preference) {
        return preference == null || SUITABLE_FOR.contains(preference);
    }

    @Override
    protected Set<String> forbiddenIngredients() {
        return union(union(super.forbiddenIngredients(), IngredientRules.MEAT),
                union(IngredientRules.SEAFOOD, IngredientRules.EGG));
    }
}
