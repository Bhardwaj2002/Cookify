package com.cookify.recipe.dto;

import com.cookify.recipe.entity.MediaKind;
import com.cookify.recipe.entity.RecipeMediaEntity;

public record MediaResponse(Long id, MediaKind type, String url) {

    public static MediaResponse from(RecipeMediaEntity m) {
        return new MediaResponse(m.getId(), m.getKind(), m.getUrl());
    }
}
