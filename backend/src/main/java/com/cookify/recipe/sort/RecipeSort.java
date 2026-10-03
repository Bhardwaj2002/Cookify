package com.cookify.recipe.sort;

/** Sort options exposed by the API, each backed by a {@link SortStrategy}. */
public enum RecipeSort {

    POPULAR(new PopularitySort()),
    TOP_RATED(new TopRatedSort()),
    NEWEST(new NewestSort()),
    QUICKEST(new QuickestSort()),
    CHEAPEST(new CheapestSort());

    private final SortStrategy strategy;

    RecipeSort(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public SortStrategy strategy() {
        return strategy;
    }
}
