package com.cookify.recipe.domain;

import java.util.Set;

/** Vegetarian without root vegetables, onion, garlic, mushrooms or honey. */
public class JainRecipe extends VegRecipe {

    public JainRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.JAIN;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Jain", "🙏", "#6B8E23");
    }

    /** A Jain dish suits every vegetarian eater, and Jains themselves. */
    @Override
    public boolean isSuitableFor(DietType preference) {
        return super.isSuitableFor(preference) || preference == DietType.JAIN;
    }

    @Override
    protected Set<String> forbiddenIngredients() {
        return union(super.forbiddenIngredients(), IngredientRules.JAIN_EXCLUDED);
    }
}
