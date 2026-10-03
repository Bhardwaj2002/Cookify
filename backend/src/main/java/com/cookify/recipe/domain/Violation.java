package com.cookify.recipe.domain;

/** A business-rule failure found by {@link Recipe#validate()}, tied to a request field. */
public record Violation(String field, String message) {
}
