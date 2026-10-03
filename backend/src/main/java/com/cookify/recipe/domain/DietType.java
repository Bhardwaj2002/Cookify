package com.cookify.recipe.domain;

/**
 * Dietary categories used for recipes and user preferences. Each maps to a {@code Recipe}
 * subclass in the recipe hierarchy (Step 5).
 */
public enum DietType {
    VEG,
    VEGAN,
    JAIN,
    EGGETARIAN,
    PESCETARIAN,
    NON_VEG
}
