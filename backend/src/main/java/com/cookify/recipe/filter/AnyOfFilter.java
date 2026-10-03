package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** Generic "attribute is one of the selected values" filter; base for cuisine and course. */
public abstract class AnyOfFilter<T> extends RecipeFilter {

    private final String attribute;
    private final List<T> values;

    protected AnyOfFilter(String attribute, List<T> values) {
        this.attribute = attribute;
        this.values = values == null ? List.of() : List.copyOf(values);
    }

    @Override
    public boolean isActive() {
        return !values.isEmpty();
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return recipe.get(attribute).in(values);
    }
}
