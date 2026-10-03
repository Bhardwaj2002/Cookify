package com.cookify.social.saved;

import com.cookify.recipe.entity.RecipeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** A recipe bookmarked by a user ("Saved Recipes" on the profile). */
@Entity
@Table(name = "saved_recipes")
@Getter
@NoArgsConstructor
public class SavedRecipe {

    @Embeddable
    public record Key(@Column(name = "user_id") Long userId, @Column(name = "recipe_id") Long recipeId) implements Serializable {
    }

    @EmbeddedId
    private Key id;

    @MapsId("recipeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SavedRecipe(Long userId, RecipeEntity recipe) {
        this.id = new Key(userId, recipe.getId());
        this.recipe = recipe;
    }
}
