package com.cookify.recipe.filter;

import com.cookify.recipe.entity.EquipmentEntity;
import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.List;
import java.util.Locale;

/**
 * "Utensils I have": keeps recipes whose required equipment is all in the user's list
 * (recipes needing no special equipment always match).
 */
public class EquipmentFilter extends RecipeFilter {

    private final List<String> available;

    public EquipmentFilter(List<String> available) {
        this.available = available == null ? List.of()
                : available.stream().map(n -> n.trim().toLowerCase(Locale.ROOT)).filter(n -> !n.isEmpty()).distinct().toList();
    }

    @Override
    public boolean isActive() {
        return !available.isEmpty();
    }

    @Override
    protected Predicate toPredicate(Root<RecipeEntity> recipe, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<RecipeEntity> same = sub.from(RecipeEntity.class);
        Join<RecipeEntity, EquipmentEntity> needed = same.join("equipment");
        Predicate[] owned = available.stream()
                .map(name -> cb.like(needed.get("name"), contains(name), ESCAPE))
                .toArray(Predicate[]::new);
        sub.select(needed.get("id")).where(cb.equal(same, recipe), cb.not(cb.or(owned)));
        return cb.not(cb.exists(sub));
    }
}
