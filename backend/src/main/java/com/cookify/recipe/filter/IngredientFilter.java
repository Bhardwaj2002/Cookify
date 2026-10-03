package com.cookify.recipe.filter;

import com.cookify.recipe.entity.IngredientEntity;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.entity.RecipeIngredientEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Available ingredients". Names match partially ("tomato" matches "tomato puree").
 * In {@link IngredientMatch#ONLY} mode, common pantry staples are assumed to be at hand.
 */
public class IngredientFilter extends RecipeFilter {

    /** Always-available basics that should not stop a recipe from matching "only what I have". */
    static final List<String> PANTRY_STAPLES = List.of(
            "salt", "water", "oil", "sugar", "pepper", "turmeric", "chilli powder", "cumin", "mustard seeds",
            "garam masala", "coriander powder", "hing", "asafoetida", "curry leaves");

    private final List<String> names;
    private final IngredientMatch mode;

    public IngredientFilter(List<String> names, IngredientMatch mode) {
        this.names = names == null ? List.of()
                : names.stream().map(n -> n.trim().toLowerCase(Locale.ROOT)).filter(n -> !n.isEmpty()).distinct().toList();
        this.mode = mode == null ? IngredientMatch.ALL : mode;
    }

    @Override
    public boolean isActive() {
        return !names.isEmpty();
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return mode == IngredientMatch.ALL ? usesAll(recipe, query, cb) : needsOnly(recipe, query, cb);
    }

    /** One EXISTS per wanted ingredient. */
    private Predicate usesAll(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> all = new ArrayList<>();
        for (String name : names) {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<RecipeIngredientEntity> line = sub.from(RecipeIngredientEntity.class);
            Join<RecipeIngredientEntity, IngredientEntity> ingredient = line.join("ingredient");
            sub.select(line.get("id")).where(
                    cb.equal(line.get("recipe"), recipe),
                    cb.like(ingredient.get("name"), contains(name), ESCAPE));
            all.add(cb.exists(sub));
        }
        return cb.and(all.toArray(Predicate[]::new));
    }

    /** NOT EXISTS an ingredient that is neither in the user's list nor a pantry staple. */
    private Predicate needsOnly(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<RecipeIngredientEntity> line = sub.from(RecipeIngredientEntity.class);
        Expression<String> name = line.join("ingredient").get("name");

        List<Predicate> available = new ArrayList<>();
        for (String have : names) {
            available.add(cb.like(name, contains(have), ESCAPE));
        }
        for (String staple : PANTRY_STAPLES) {
            available.add(cb.like(name, contains(staple), ESCAPE));
        }
        sub.select(line.get("id")).where(
                cb.equal(line.get("recipe"), recipe),
                cb.not(cb.or(available.toArray(Predicate[]::new))));
        return cb.not(cb.exists(sub));
    }
}
