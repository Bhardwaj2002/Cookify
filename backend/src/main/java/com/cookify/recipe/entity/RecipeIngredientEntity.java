package com.cookify.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One ingredient line of a recipe: which ingredient, how much, and its position in the list. */
@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor
public class RecipeIngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id")
    private IngredientEntity ingredient;

    @Column(length = 60)
    private String quantity;

    @Column(nullable = false)
    private int position;

    public RecipeIngredientEntity(RecipeEntity recipe, IngredientEntity ingredient, String quantity, int position) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.position = position;
    }
}
