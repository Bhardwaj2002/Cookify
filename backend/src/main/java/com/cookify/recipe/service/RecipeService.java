package com.cookify.recipe.service;

import com.cookify.common.exception.FieldValidationException;
import com.cookify.common.exception.ForbiddenException;
import com.cookify.common.exception.NotFoundException;
import com.cookify.common.response.ErrorResponse.FieldError;
import com.cookify.common.response.PageResponse;
import com.cookify.media.StorageService;
import com.cookify.moderation.ModerationService;
import com.cookify.moderation.Warning;
import com.cookify.notification.ActivityEvents;
import org.springframework.context.ApplicationEventPublisher;
import com.cookify.recipe.domain.IngredientLine;
import com.cookify.recipe.domain.Recipe;
import com.cookify.recipe.domain.RecipeFactory;
import com.cookify.recipe.domain.RecipeSpec;
import com.cookify.recipe.dto.RecipeDetailResponse;
import com.cookify.recipe.dto.RecipeRequest;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.recipe.dto.ViewerState;
import com.cookify.recipe.entity.EquipmentEntity;
import com.cookify.recipe.entity.IngredientEntity;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.entity.RecipeIngredientEntity;
import com.cookify.recipe.entity.RecipeMediaEntity;
import com.cookify.recipe.repository.EquipmentRepository;
import com.cookify.recipe.repository.IngredientRepository;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.security.jwt.AuthUser;
import com.cookify.social.follow.Follow;
import com.cookify.social.follow.FollowRepository;
import com.cookify.social.rating.RatingRepository;
import com.cookify.social.saved.SavedRecipe;
import com.cookify.social.saved.SavedRecipeRepository;
import com.cookify.user.entity.Role;
import com.cookify.user.entity.User;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserService userService;
    private final StorageService storage;
    private final RecipeMapper mapper;
    private final RatingRepository ratingRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final FollowRepository followRepository;
    private final ModerationService moderationService;
    private final ApplicationEventPublisher events;

    @Transactional
    public RecipeDetailResponse create(AuthUser caller, RecipeRequest request) {
        screen(caller, request);
        RecipeSpec spec = validated(request);
        RecipeEntity entity = new RecipeEntity();
        entity.setAuthor(userService.getById(caller.id()));
        apply(spec, entity);
        RecipeEntity saved = recipeRepository.saveAndFlush(entity);
        events.publishEvent(new ActivityEvents.RecipePublished(saved.getId()));
        return mapper.toDetail(saved, true, ViewerState.NONE);
    }

    @Transactional
    public RecipeDetailResponse update(Long id, AuthUser caller, RecipeRequest request) {
        RecipeEntity entity = getEditable(id, caller);
        screen(caller, request);
        RecipeSpec spec = validated(request);
        apply(spec, entity);
        RecipeEntity saved = recipeRepository.saveAndFlush(entity);
        return mapper.toDetail(saved, true, viewerState(saved, caller));
    }

    @Transactional
    public void delete(Long id, AuthUser caller) {
        RecipeEntity entity = getEditable(id, caller);
        List<String> keys = entity.getMedia().stream().map(RecipeMediaEntity::getStorageKey).toList();
        recipeRepository.delete(entity);
        afterCommit(() -> keys.forEach(storage::delete));
    }

    /** Recipe page. Counts a view unless the viewer is the author. {@code viewer} may be null. */
    @Transactional
    public RecipeDetailResponse view(Long id, AuthUser viewer) {
        RecipeEntity entity = get(id);
        boolean isAuthor = viewer != null && viewer.id().equals(entity.getAuthor().getId());
        if (!isAuthor) {
            recipeRepository.incrementViews(id);
            entity.setViews(entity.getViews() + 1);
        }
        return mapper.toDetail(entity, canEdit(entity, viewer), viewerState(entity, viewer));
    }

    /** The viewer's own rating, bookmark and subscription for this recipe. */
    private ViewerState viewerState(RecipeEntity entity, AuthUser viewer) {
        if (viewer == null) {
            return ViewerState.NONE;
        }
        Integer myRating = ratingRepository.findByRecipeIdAndUserId(entity.getId(), viewer.id())
                .map(r -> (int) r.getScore()).orElse(null);
        boolean saved = savedRecipeRepository.existsById(new SavedRecipe.Key(viewer.id(), entity.getId()));
        boolean following = followRepository.existsById(new Follow.Key(viewer.id(), entity.getAuthor().getId()));
        return new ViewerState(myRating, saved, following);
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> byAuthor(String username, int page, int size) {
        User author = userService.getPublicByUsername(username);
        return PageResponse.from(recipeRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId(), PageRequest.of(page, size)),
                mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public long countByAuthor(Long authorId) {
        return recipeRepository.countByAuthorId(authorId);
    }

    // ---- Shared with RecipeMediaService and the social features -------------------

    public RecipeEntity get(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("RECIPE_NOT_FOUND", "Recipe not found"));
    }

    /** Loads a recipe the caller may change: its author, or an admin. */
    RecipeEntity getEditable(Long id, AuthUser caller) {
        RecipeEntity entity = get(id);
        if (!canEdit(entity, caller)) {
            throw new ForbiddenException("NOT_RECIPE_OWNER", "Only the author can change this recipe");
        }
        return entity;
    }

    static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    // ---- Internals --------------------------------------------------------------

    private static boolean canEdit(RecipeEntity entity, AuthUser caller) {
        return caller != null && (caller.role() == Role.ADMIN || caller.id().equals(entity.getAuthor().getId()));
    }

    /** Abusive language anywhere in the recipe text earns the author a warning. */
    private void screen(AuthUser caller, RecipeRequest r) {
        List<String> texts = new ArrayList<>();
        texts.add(r.title());
        texts.add(r.description());
        texts.addAll(r.steps());
        r.ingredients().forEach(i -> texts.add(i.name()));
        if (r.equipment() != null) {
            texts.addAll(r.equipment());
        }
        moderationService.screen(caller, Warning.Reason.ABUSIVE_RECIPE, texts.toArray(String[]::new));
    }

    /** Normalises the request and runs the diet subclass's business rules. */
    private RecipeSpec validated(RecipeRequest request) {
        RecipeSpec spec = mapper.toSpec(request);
        Recipe recipe = RecipeFactory.create(spec);

        List<FieldError> errors = new ArrayList<>();
        recipe.validate().forEach(v -> errors.add(new FieldError(v.field(), v.message())));
        Set<String> seen = new HashSet<>();
        for (IngredientLine line : spec.ingredients()) {
            if (!seen.add(line.name())) {
                errors.add(new FieldError("ingredients", "Ingredient listed twice: " + line.name()));
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return spec;
    }

    private void apply(RecipeSpec spec, RecipeEntity e) {
        e.setTitle(spec.title());
        e.setDescription(spec.description());
        e.setDietType(spec.dietType());
        e.setCuisine(spec.cuisine());
        e.setCourse(spec.course());
        e.setPrepMinutes(spec.prepMinutes());
        e.setCookMinutes(spec.cookMinutes());
        e.setServings(spec.servings());
        e.setCost(spec.cost());
        e.setCalories(spec.calories());
        e.setProtein(spec.protein());
        e.setDifficulty((short) spec.difficulty());
        e.setSteps(new ArrayList<>(spec.steps()));

        e.getIngredients().clear();
        int position = 0;
        for (IngredientLine line : spec.ingredients()) {
            IngredientEntity ingredient = ingredientRepository.findByName(line.name())
                    .orElseGet(() -> ingredientRepository.save(new IngredientEntity(line.name())));
            e.getIngredients().add(new RecipeIngredientEntity(e, ingredient, line.quantity(), position++));
        }

        e.getEquipment().clear();
        for (String name : spec.equipment()) {
            EquipmentEntity item = equipmentRepository.findByName(name)
                    .orElseGet(() -> equipmentRepository.save(new EquipmentEntity(name)));
            e.getEquipment().add(item);
        }
    }
}
