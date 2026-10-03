package com.cookify.recipe.dto;

/** What the current viewer has done with a recipe (all empty for anonymous visitors). */
public record ViewerState(Integer myRating, boolean saved, boolean followingAuthor) {

    public static final ViewerState NONE = new ViewerState(null, false, false);
}
