package com.cookify.recipe.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A recipe in the domain model (independent of the database). Each diet is a subclass:
 *
 * <pre>
 *                    Recipe (abstract)
 *        ┌──────────┬────────┴─────────┬───────────────┐
 *     VegRecipe  EggetarianRecipe  PescetarianRecipe  NonVegRecipe
 *     ┌───┴────┐
 * VeganRecipe JainRecipe
 * </pre>
 *
 * Subclasses override (polymorphism):
 * <ul>
 *   <li>{@link #getDietType()} / {@link #getDietBadge()} – what the recipe is and how it is shown</li>
 *   <li>{@link #forbiddenIngredients()} – extended by subclasses via {@code super}, e.g. Vegan = Veg + dairy</li>
 *   <li>{@link #isSuitableFor(DietType)} – which eaters may cook it, e.g. a Jain recipe suits vegetarians too</li>
 * </ul>
 */
public abstract class Recipe {

    protected final RecipeSpec spec;

    protected Recipe(RecipeSpec spec) {
        this.spec = spec;
    }

    // ---- Overridden by each diet --------------------------------------------

    public abstract DietType getDietType();

    public abstract DietBadge getDietBadge();

    /** Can someone following {@code preference} eat this? {@code null} means no preference. */
    public abstract boolean isSuitableFor(DietType preference);

    /** Ingredient keywords this diet does not allow. Subclasses add to their parent's set. */
    protected Set<String> forbiddenIngredients() {
        return Set.of();
    }

    // ---- Shared behaviour -----------------------------------------------------

    /**
     * Business rules on top of field validation: common checks plus the diet's forbidden
     * ingredients (e.g. a Jain recipe containing onion).
     */
    public final List<Violation> validate() {
        List<Violation> violations = new ArrayList<>();
        if (spec.prepMinutes() + spec.cookMinutes() == 0) {
            violations.add(new Violation("cookMinutes", "Prep and cook time cannot both be 0"));
        }
        Set<String> forbidden = forbiddenIngredients();
        Set<String> reported = new HashSet<>();
        for (IngredientLine line : spec.ingredients()) {
            IngredientRules.findForbidden(line.name(), forbidden)
                    .filter(reported::add)
                    .ifPresent(word -> violations.add(new Violation("ingredients",
                            "%s recipes cannot contain %s (found \"%s\")".formatted(getDietBadge().label(), word, line.name()))));
        }
        return violations;
    }

    public int getTotalMinutes() {
        return spec.prepMinutes() + spec.cookMinutes();
    }

    /** "Speed" stars from the mockup, derived from total time: 5 = 15 min or less, 1 = over 2 hours. */
    public int getSpeed() {
        int minutes = getTotalMinutes();
        if (minutes <= 15) return 5;
        if (minutes <= 30) return 4;
        if (minutes <= 60) return 3;
        if (minutes <= 120) return 2;
        return 1;
    }

    public RecipeSpec getSpec() {
        return spec;
    }

    protected static Set<String> union(Set<String> base, Set<String> more) {
        Set<String> all = new HashSet<>(base);
        all.addAll(more);
        return Set.copyOf(all);
    }
}
