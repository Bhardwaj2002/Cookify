package com.cookify.recipe.service;

import com.cookify.common.response.PageResponse;
import com.cookify.recipe.dto.RecipeSearchRequest;
import com.cookify.recipe.dto.RecipeSummaryResponse;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.filter.FilterFactory;
import com.cookify.recipe.filter.RecipeFilter;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.recipe.sort.SortStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Filtered, sorted, paginated recipe search: ANDs the active filters and applies the chosen sort. */
@Service
@RequiredArgsConstructor
public class RecipeSearchService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<RecipeSummaryResponse> search(RecipeSearchRequest request) {
        List<Specification<RecipeEntity>> filters = FilterFactory.from(request).stream()
                .map(RecipeFilter::toSpecification)
                .toList();
        Specification<RecipeEntity> spec = sorted(Specification.allOf(filters), request.sortOrDefault().strategy());
        PageRequest page = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault());
        return PageResponse.from(recipeRepository.findAll(spec, page), mapper::toSummary);
    }

    /** Adds ORDER BY to the data query only (the COUNT query used for paging must stay unordered). */
    private static Specification<RecipeEntity> sorted(Specification<RecipeEntity> spec, SortStrategy sort) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(sort.orders(root, cb));
            }
            return spec.toPredicate(root, query, cb);
        };
    }
}
