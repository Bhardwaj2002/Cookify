package com.cookify.recipe.service;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;
import com.cookify.recipe.dto.FilterOptionsResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Filter choices for the browse page, with ingredient/equipment names ordered by how often recipes use them. */
@Service
@RequiredArgsConstructor
public class FilterOptionsService {

    private final EntityManager em;

    @Transactional(readOnly = true)
    public FilterOptionsResponse options() {
        List<String> ingredients = em.createQuery("""
                SELECT i.name FROM RecipeIngredientEntity ri JOIN ri.ingredient i
                GROUP BY i.name ORDER BY COUNT(ri) DESC, i.name""", String.class)
                .setMaxResults(150).getResultList();
        List<String> equipment = em.createQuery("""
                SELECT e.name FROM RecipeEntity r JOIN r.equipment e
                GROUP BY e.name ORDER BY COUNT(r) DESC, e.name""", String.class)
                .setMaxResults(60).getResultList();
        Object[] max = em.createQuery("""
                SELECT COALESCE(MAX(r.cost), 0), COALESCE(MAX(r.totalMinutes), 0),
                       COALESCE(MAX(r.calories), 0), COALESCE(MAX(r.protein), 0)
                FROM RecipeEntity r""", Object[].class).getSingleResult();

        return new FilterOptionsResponse(
                Arrays.asList(DietType.values()), Arrays.asList(Cuisine.values()), Arrays.asList(Course.values()),
                ingredients, equipment,
                new FilterOptionsResponse.Ranges((BigDecimal) max[0], ((Number) max[1]).intValue(),
                        ((Number) max[2]).intValue(), ((Number) max[3]).intValue()));
    }
}
