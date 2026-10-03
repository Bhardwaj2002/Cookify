package com.cookify.recipe.filter;

import com.cookify.recipe.domain.Cuisine;

import java.util.List;

/** Regionality, e.g. Pan-Asian or English. */
public class CuisineFilter extends AnyOfFilter<Cuisine> {

    public CuisineFilter(List<Cuisine> cuisines) {
        super("cuisine", cuisines);
    }
}
