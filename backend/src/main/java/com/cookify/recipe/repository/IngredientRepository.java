package com.cookify.recipe.repository;

import com.cookify.recipe.entity.IngredientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {

    Optional<IngredientEntity> findByName(String name);
}
