package com.cookify.recipe.filter;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * One search criterion. Each subclass turns its own parameters into a SQL predicate;
 * {@link FilterFactory} builds the active ones and the search service ANDs them together.
 * Adding a new filter = adding one subclass.
 */
public abstract class RecipeFilter {

    /** False when the user did not set this filter, so it is skipped. */
    public abstract boolean isActive();

    protected abstract Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb);

    public final Specification<RecipeEntity> toSpecification() {
        return this::toPredicate;
    }

    /** LIKE pattern matching {@code text} anywhere, with user-typed % and _ escaped. */
    protected static String contains(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    protected static final char ESCAPE = '\\';
}
