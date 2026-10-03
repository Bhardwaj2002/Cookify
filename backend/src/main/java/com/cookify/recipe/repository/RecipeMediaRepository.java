package com.cookify.recipe.repository;

import com.cookify.recipe.entity.RecipeMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeMediaRepository extends JpaRepository<RecipeMediaEntity, Long> {

    Optional<RecipeMediaEntity> findByIdAndRecipeId(Long id, Long recipeId);
}
