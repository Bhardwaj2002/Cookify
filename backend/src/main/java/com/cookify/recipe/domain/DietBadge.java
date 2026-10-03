package com.cookify.recipe.domain;

/** How a recipe's diet is displayed: label, emoji icon and badge colour (Indian veg/non-veg mark style). */
public record DietBadge(String label, String icon, String color) {
}
