package com.cookify.social.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request/response bodies for {@code /api/recipes/{id}/rating}. */
public final class RatingDtos {

    private RatingDtos() {
    }

    public record RatingRequest(@NotNull @Min(1) @Max(5) Integer score) {
    }

    /** Updated recipe totals plus the caller's own score ({@code null} after removing it). */
    public record RatingResponse(BigDecimal avgRating, int ratingCount, Integer myRating) {
    }
}
