package com.cookify.recipe.domain;

/** Contains meat; no ingredient restrictions. Only suits non-vegetarians. */
public class NonVegRecipe extends Recipe {

    public NonVegRecipe(RecipeSpec spec) {
        super(spec);
    }

    @Override
    public DietType getDietType() {
        return DietType.NON_VEG;
    }

    @Override
    public DietBadge getDietBadge() {
        return new DietBadge("Non-veg", "🍗", "#C2413A");
    }

    @Override
    public boolean isSuitableFor(DietType preference) {
        return preference == null || preference == DietType.NON_VEG;
    }
}
