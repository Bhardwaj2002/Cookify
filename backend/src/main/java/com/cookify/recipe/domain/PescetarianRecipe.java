package com.cookify.recipe.domain;

import java.util.EnumSet;
import java.util.Set;

/** Fish and seafood (and eggs/dairy), but no meat. */
public class PescetarianRecipe extends Recipe {

    private static final Set<DietType> SUITABLE_FOR = EnumSet.of(DietType.PESCETARIAN, DietType.NON_VEG);

    public PescetarianRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.PESCETARIAN;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Pescetarian", "🐟", "#2C7FB8");
    }

    @Override
    public boolean isSuitableFor(DietType preference) {
        return preference == null || SUITABLE_FOR.contains(preference);
    }

    @Override
    protected Set<String> forbiddenIngredients() {
        return IngredientRules.MEAT;
    }
}
