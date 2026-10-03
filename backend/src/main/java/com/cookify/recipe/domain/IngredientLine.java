package com.cookify.recipe.domain;

/** One line of a recipe's ingredient list, e.g. name "paneer", quantity "200 g". */
public record IngredientLine(String name, String quantity) {
}
