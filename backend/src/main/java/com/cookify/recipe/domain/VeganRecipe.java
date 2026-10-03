package com.cookify.recipe.domain;

import java.util.Set;

/** Vegetarian with no animal products at all: also excludes dairy and honey. */
public class VeganRecipe extends VegRecipe {

    public VeganRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.VEGAN;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Vegan", "🌱", "#2F7D4F");
    }

    /** A vegan dish suits every vegetarian eater, and vegans themselves. */
    @Override
    public boolean isSuitableFor(DietType preference) {
        return super.isSuitableFor(preference) || preference == DietType.VEGAN;
    }

    @Override
    protected Set<String> forbiddenIngredients() {
        return union(super.forbiddenIngredients(), IngredientRules.DAIRY_AND_HONEY);
    }
}
