package com.cookify.recipe.filter;

/** How the "ingredients" filter interprets the list. */
public enum IngredientMatch {
    /** Recipe uses every listed ingredient ("I want something with paneer and spinach"). */
    ALL,
    /** Recipe needs nothing beyond the listed ingredients and pantry staples ("what can I cook with what I have?"). */
    ONLY
}
