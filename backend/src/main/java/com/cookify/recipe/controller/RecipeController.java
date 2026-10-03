package com.cookify.recipe.controller;

import com.cookify.common.response.PageResponse;
import com.cookify.recipe.dto.FilterOptionsResponse;
import com.cookify.recipe.dto.MediaResponse;
import com.cookify.recipe.dto.RecipeDetailResponse;
import com.cookify.recipe.dto.RecipeRequest;
import com.cookify.recipe.dto.RecipeSearchRequest;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.recipe.service.FilterOptionsService;
import com.cookify.recipe.service.RecipeMediaService;
import com.cookify.recipe.service.RecipeSearchService;
import com.cookify.recipe.service.RecipeService;
import com.cookify.security.jwt.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Recipes")
@RestController
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeMediaService mediaService;
    private final RecipeSearchService searchService;
    private final FilterOptionsService filterOptionsService;

    @Operation(summary = "Search and filter recipes",
            description = "All filters are optional and combined with AND. Sort: POPULAR (default), TOP_RATED, NEWEST, QUICKEST, CHEAPEST.")
    @GetMapping("/api/recipes")
    public PageResponse<RecipeSummaryResponse> search(@ParameterObject @Valid RecipeSearchRequest request) {
        return searchService.search(request);
    }

    @Operation(summary = "Choices and ranges for the browse page filters")
    @GetMapping("/api/meta/filters")
    public FilterOptionsResponse filterOptions() {
        return filterOptionsService.options();
    }

    @Operation(summary = "One recipe with steps, ingredients and media (counts a view)")
    @GetMapping("/api/recipes/{id}")
    public RecipeDetailResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser viewer) {
        return recipeService.view(id, viewer);
    }

    @Operation(summary = "Create a recipe")
    @PostMapping("/api/recipes")
    public ResponseEntity<RecipeDetailResponse> create(@AuthenticationPrincipal AuthUser caller,
                                                       @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.create(caller, request));
    }

    @Operation(summary = "Replace a recipe (author or admin)")
    @PutMapping("/api/recipes/{id}")
    public RecipeDetailResponse update(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
                                       @Valid @RequestBody RecipeRequest request) {
        return recipeService.update(id, caller, request);
    }

    @Operation(summary = "Delete a recipe and its media (author or admin)")
    @DeleteMapping("/api/recipes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        recipeService.delete(id, caller);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Attach a photo (≤ 10 MB) or video (≤ 50 MB); max 10 per recipe")
    @PostMapping(path = "/api/recipes/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> addMedia(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
                                                  @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.add(id, caller, file));
    }

    @Operation(summary = "Remove a photo or video from a recipe")
    @DeleteMapping("/api/recipes/{id}/media/{mediaId}")
    public ResponseEntity<Void> removeMedia(@PathVariable Long id, @PathVariable Long mediaId,
                                            @AuthenticationPrincipal AuthUser caller) {
        mediaService.remove(id, mediaId, caller);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "A user's uploaded recipes, newest first (public)")
    @GetMapping("/api/users/{username}/recipes")
    public PageResponse<RecipeSummaryResponse> byAuthor(@PathVariable String username,
                                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return recipeService.byAuthor(username, page, size);
    }
}
