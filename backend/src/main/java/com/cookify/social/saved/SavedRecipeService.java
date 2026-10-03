package com.cookify.social.saved;

import com.cookify.common.response.PageResponse;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.recipe.service.RecipeMapper;
import com.cookify.recipe.service.RecipeService;
import com.cookify.security.jwt.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bookmarks. Saving twice or removing a recipe that isn't saved are both no-ops. */
@Service
@RequiredArgsConstructor
public class SavedRecipeService {

    private final SavedRecipeRepository repository;
    private final RecipeService recipeService;
    private final RecipeMapper mapper;

    @Transactional
    public void save(Long recipeId, AuthUser caller) {
        var recipe = recipeService.get(recipeId);
        var key = new SavedRecipe.Key(caller.id(), recipeId);
        if (!repository.existsById(key)) {
            repository.save(new SavedRecipe(caller.id(), recipe));
        }
    }

    @Transactional
    public void unsave(Long recipeId, AuthUser caller) {
        recipeService.get(recipeId);
        repository.deleteById(new SavedRecipe.Key(caller.id(), recipeId));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> list(AuthUser caller, int page, int size) {
        return PageResponse.from(repository.findByIdUserIdOrderByCreatedAtDesc(caller.id(), PageRequest.of(page, size)),
                saved -> mapper.toSummary(saved.getRecipe()));
    }
}
