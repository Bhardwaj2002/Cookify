package com.cookify.recommendation;

import com.cookify.common.response.PageResponse;
import com.cookify.recipe.domain.DietType;
import com.cookify.recipe.domain.RecipeFactory;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.repository.RecipeRepository;
import com.cookify.recipe.service.RecipeMapper;
import com.cookify.recommendation.RecommendationResponse.Reason;
import com.cookify.security.jwt.AuthUser;
import com.cookify.user.dto.PreferencesResponse;
import com.cookify.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "For You" feed. Hard rules: only diets the user can eat (from the Recipe class hierarchy),
 * never their own or already-rated recipes. Ranking: trending score plus boosts for followed
 * cooks, cuisines they liked/saved, and recipes within their calorie / protein targets.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    // Trending scores span roughly 20–60 for established recipes, so boosts are on the same scale:
    // a followed cook's recipe should beat almost anything else.
    static final double FOLLOW_BOOST = 20.0;
    static final double CUISINE_BOOST = 8.0;
    static final double NUTRITION_BOOST = 4.0;

    /** Shared FROM/WHERE: candidate recipes for :uid. */
    private static final String CANDIDATES = """
            FROM recipes r
            LEFT JOIN follows f ON f.followee_id = r.author_id AND f.follower_id = :uid
            LEFT JOIN (
                SELECT DISTINCT r2.cuisine
                FROM recipes r2
                WHERE r2.id IN (SELECT s.recipe_id FROM saved_recipes s WHERE s.user_id = :uid)
                   OR r2.id IN (SELECT ra.recipe_id FROM ratings ra WHERE ra.user_id = :uid AND ra.score >= 4)
            ) liked ON liked.cuisine = r.cuisine
            WHERE r.author_id <> :uid
              AND r.diet_type IN (:diets)
              AND NOT EXISTS (SELECT 1 FROM ratings ra WHERE ra.recipe_id = r.id AND ra.user_id = :uid)
            """;

    private static final String SELECT_SCORED = """
            SELECT r.id,
                   f.follower_id IS NOT NULL AS followed,
                   liked.cuisine IS NOT NULL AS liked_cuisine,
                   (:maxCalories IS NOT NULL AND r.calories <= :maxCalories) AS fits_calories,
                   (:minProtein IS NOT NULL AND COALESCE(r.protein, 0) >= :minProtein) AS fits_protein,
                   recipe_popularity(r.avg_rating, r.rating_count, r.views, r.created_at)
                     + CASE WHEN f.follower_id IS NOT NULL THEN :followBoost ELSE 0 END
                     + CASE WHEN liked.cuisine IS NOT NULL THEN :cuisineBoost ELSE 0 END
                     + CASE WHEN :maxCalories IS NOT NULL AND r.calories <= :maxCalories THEN :nutritionBoost ELSE 0 END
                     + CASE WHEN :minProtein IS NOT NULL AND COALESCE(r.protein, 0) >= :minProtein THEN :nutritionBoost ELSE 0 END
                     AS score
            """ + CANDIDATES + """
            ORDER BY score DESC, r.id DESC
            LIMIT :limit OFFSET :offset
            """;

    private record Scored(long id, boolean followed, boolean likedCuisine, boolean fitsCalories, boolean fitsProtein) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper mapper;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<RecommendationResponse> forUser(AuthUser user, int page, int size) {
        PreferencesResponse prefs = userService.getPreferences(user.id());
        Set<DietType> diets = RecipeFactory.dietsSuitableFor(prefs.dietType());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("uid", user.id())
                .addValue("diets", diets.stream().map(Enum::name).toList())
                .addValue("maxCalories", prefs.maxCalories(), java.sql.Types.INTEGER)
                .addValue("minProtein", prefs.minProtein(), java.sql.Types.INTEGER)
                .addValue("followBoost", FOLLOW_BOOST)
                .addValue("cuisineBoost", CUISINE_BOOST)
                .addValue("nutritionBoost", NUTRITION_BOOST)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);

        List<Scored> scored = jdbc.query(SELECT_SCORED, params, (rs, i) -> new Scored(rs.getLong("id"),
                rs.getBoolean("followed"), rs.getBoolean("liked_cuisine"),
                rs.getBoolean("fits_calories"), rs.getBoolean("fits_protein")));
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + CANDIDATES, params, Long.class);

        Map<Long, RecipeEntity> byId = recipeRepository.findAllById(scored.stream().map(Scored::id).toList()).stream()
                .collect(Collectors.toMap(RecipeEntity::getId, Function.identity()));
        List<RecommendationResponse> content = scored.stream()
                .filter(s -> byId.containsKey(s.id()))
                .map(s -> new RecommendationResponse(mapper.toSummary(byId.get(s.id())), reasons(s, prefs.dietType())))
                .toList();

        long totalElements = total == null ? 0 : total;
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    private static List<Reason> reasons(Scored s, DietType preference) {
        List<Reason> reasons = new ArrayList<>();
        if (s.followed()) reasons.add(Reason.FOLLOWED_AUTHOR);
        if (s.likedCuisine()) reasons.add(Reason.LIKED_CUISINE);
        if (preference != null) reasons.add(Reason.FITS_DIET);
        if (s.fitsCalories()) reasons.add(Reason.FITS_CALORIES);
        if (s.fitsProtein()) reasons.add(Reason.FITS_PROTEIN);
        if (reasons.isEmpty()) reasons.add(Reason.TRENDING);
        return reasons;
    }
}
