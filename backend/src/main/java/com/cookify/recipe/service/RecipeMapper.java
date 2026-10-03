package com.cookify.recipe.service;

import com.cookify.recipe.domain.IngredientLine;
import com.cookify.recipe.domain.Recipe;
import com.cookify.recipe.domain.RecipeFactory;
import com.cookify.recipe.domain.RecipeSpec;
import com.cookify.recipe.dto.AuthorInfo;
import com.cookify.recipe.dto.DietInfo;
import com.cookify.recipe.dto.MediaResponse;
import com.cookify.recipe.dto.RecipeDetailResponse;
import com.cookify.recipe.dto.RecipeRequest;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.recipe.dto.ViewerState;
import com.cookify.recipe.entity.EquipmentEntity;
import com.cookify.recipe.entity.MediaKind;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.entity.RecipeMediaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Converts between API DTOs, the domain {@link Recipe} hierarchy and JPA entities. */
@Component
public class RecipeMapper {

    /** Request → domain spec, with names normalised (trimmed, lower-case ingredients and equipment). */
    public RecipeSpec toSpec(RecipeRequest r) {
        List<IngredientLine> ingredients = r.ingredients().stream()
                .map(i -> new IngredientLine(normalizeName(i.name()), blankToNull(i.quantity())))
                .toList();
        List<String> equipment = r.equipment() == null ? List.of()
                : r.equipment().stream().map(RecipeMapper::normalizeName).distinct().toList();
        List<String> steps = r.steps().stream().map(String::trim).toList();
        return new RecipeSpec(r.title().trim(), blankToNull(r.description()), r.dietType(), r.cuisine(), r.course(),
                r.prepMinutes(), r.cookMinutes(), r.servings(), r.cost(), r.calories(), r.protein(), r.difficulty(),
                ingredients, equipment, steps);
    }

    /** Entity → domain object (the right diet subclass), used for badges, speed and diet rules. */
    public Recipe toDomain(RecipeEntity e, boolean withLists) {
        List<IngredientLine> ingredients = withLists
                ? e.getIngredients().stream().map(i -> new IngredientLine(i.getIngredient().getName(), i.getQuantity())).toList()
                : List.of();
        List<String> equipment = withLists ? e.getEquipment().stream().map(EquipmentEntity::getName).toList() : List.of();
        return RecipeFactory.create(new RecipeSpec(e.getTitle(), e.getDescription(), e.getDietType(), e.getCuisine(),
                e.getCourse(), e.getPrepMinutes(), e.getCookMinutes(), e.getServings(), e.getCost(), e.getCalories(),
                e.getProtein(), e.getDifficulty(), ingredients, equipment, withLists ? e.getSteps() : List.of()));
    }

    public RecipeSummaryResponse toSummary(RecipeEntity e) {
        Recipe recipe = toDomain(e, false);
        return new RecipeSummaryResponse(e.getId(), e.getTitle(), DietInfo.of(recipe.getDietType(), recipe.getDietBadge()),
                e.getCuisine(), e.getCourse(), recipe.getTotalMinutes(), recipe.getSpeed(), e.getDifficulty(),
                e.getCost(), e.getCalories(), coverUrl(e), AuthorInfo.from(e.getAuthor()), e.getViews(),
                e.getAvgRating(), e.getRatingCount(), e.getCommentCount(), e.getCreatedAt());
    }

    public RecipeDetailResponse toDetail(RecipeEntity e, boolean canEdit, ViewerState viewer) {
        Recipe recipe = toDomain(e, true);
        return new RecipeDetailResponse(e.getId(), e.getTitle(), e.getDescription(),
                DietInfo.of(recipe.getDietType(), recipe.getDietBadge()), e.getCuisine(), e.getCourse(),
                e.getPrepMinutes(), e.getCookMinutes(), recipe.getTotalMinutes(), recipe.getSpeed(), e.getServings(),
                e.getCost(), e.getCalories(), e.getProtein(), e.getDifficulty(),
                e.getIngredients().stream()
                        .map(i -> new RecipeDetailResponse.IngredientResponse(i.getIngredient().getName(), i.getQuantity()))
                        .toList(),
                e.getEquipment().stream().map(EquipmentEntity::getName).toList(),
                List.copyOf(e.getSteps()),
                e.getMedia().stream().map(MediaResponse::from).toList(),
                AuthorInfo.from(e.getAuthor()), e.getViews(), e.getAvgRating(), e.getRatingCount(), e.getCommentCount(),
                e.getCreatedAt(), e.getUpdatedAt(), canEdit, viewer);
    }

    private static String coverUrl(RecipeEntity e) {
        return e.getMedia().stream()
                .filter(m -> m.getKind() == MediaKind.IMAGE)
                .map(RecipeMediaEntity::getUrl)
                .findFirst()
                .orElse(null);
    }

    static String normalizeName(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
