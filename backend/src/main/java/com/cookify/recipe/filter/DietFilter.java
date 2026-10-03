package com.cookify.recipe.filter;

import com.cookify.recipe.domain.DietType;
import com.cookify.recipe.domain.RecipeFactory;
import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Set;

/**
 * Recipes someone following {@code preference} can eat. The allowed diets come from the recipe
 * class hierarchy ({@code isSuitableFor}), e.g. VEG → Vegetarian, Vegan and Jain recipes.
 */
public class DietFilter extends RecipeFilter {

    private final DietType preference;

    public DietFilter(DietType preference) {
        this.preference = preference;
    }

    @Override
    public boolean isActive() {
        return preference != null;
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Set<DietType> allowed = RecipeFactory.dietsSuitableFor(preference);
        return recipe.get("dietType").in(allowed);
    }
}
