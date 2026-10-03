package com.cookify.recipe.dto;

import com.cookify.recipe.domain.DietBadge;
import com.cookify.recipe.domain.DietType;

/** Diet type plus its display badge (from the recipe's domain subclass). */
public record DietInfo(DietType type, String label, String icon, String color) {

    public static DietInfo of(DietType type, DietBadge badge) {
        return new DietInfo(type, badge.label(), badge.icon(), badge.color());
    }
}
