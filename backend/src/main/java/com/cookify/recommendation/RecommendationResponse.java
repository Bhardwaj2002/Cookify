package com.cookify.recommendation;

import com.cookify.recipe.dto.RecipeSummaryResponse;

import java.util.List;

/** A recommended recipe plus the reasons it was picked (shown on the card). */
public record RecommendationResponse(RecipeSummaryResponse recipe, List<Reason> reasons) {

    public enum Reason {
        FOLLOWED_AUTHOR,
        LIKED_CUISINE,
        FITS_CALORIES,
        FITS_PROTEIN,
        FITS_DIET,
        TRENDING
    }
}
