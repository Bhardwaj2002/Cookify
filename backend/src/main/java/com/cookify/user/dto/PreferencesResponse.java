package com.cookify.user.dto;

import com.cookify.recipe.domain.DietType;
import com.cookify.user.entity.UserPreference;

public record PreferencesResponse(DietType dietType, Integer maxCalories, Integer minProtein) {

    public static final PreferencesResponse NONE = new PreferencesResponse(null, null, null);

    public static PreferencesResponse from(UserPreference p) {
        return new PreferencesResponse(p.getDietType(), p.getMaxCalories(), p.getMinProtein());
    }
}
