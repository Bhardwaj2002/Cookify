package com.cookify.demo;

import com.cookify.recipe.dto.RecipeDetailResponse;
import com.cookify.recipe.dto.RecipeRequest;
import com.cookify.recipe.service.RecipeMediaService;
import com.cookify.recipe.service.RecipeService;
import com.cookify.security.jwt.AuthUser;
import com.cookify.user.entity.Gender;
import com.cookify.user.entity.Role;
import com.cookify.user.entity.User;
import com.cookify.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads sample cooks and recipes from {@code demo/demo-data.json} once (skipped if the first demo
 * user already exists). Recipes go through {@link RecipeService}, so the diet rules validate the
 * sample data too. Views, ratings and upload dates are then set directly for realistic sorting.
 * Each recipe gets its photo from {@code demo/images/<title-slug>.jpg} (Wikimedia Commons, see
 * {@code docs/IMAGE_CREDITS.md}); photos are also added to demo recipes seeded before they existed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    record DemoUser(String username, String email, String firstName, String lastName, Short age, Gender gender, String bio) {
    }

    record DemoRecipe(String author, int daysAgo, long views, BigDecimal avgRating, int ratingCount, RecipeRequest recipe) {
    }

    record DemoData(String password, DemoUser admin, List<DemoUser> users, List<DemoRecipe> recipes) {
    }

    private final JsonMapper jsonMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecipeService recipeService;
    private final RecipeMediaService mediaService;
    private final JdbcTemplate jdbc;

    /** Off in most tests: they reload the dataset per test and don't need 39 photo writes each time. */
    @Value("${app.demo-data.photos:true}")
    private boolean photos;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        DemoData data;
        try (InputStream in = new ClassPathResource("demo/demo-data.json").getInputStream()) {
            data = jsonMapper.readValue(in, DemoData.class);
        }
        String hash = passwordEncoder.encode(data.password());

        // The admin account is ensured separately so it also appears in databases seeded earlier.
        if (!userRepository.existsByUsernameIgnoreCase(data.admin().username())) {
            User admin = toUser(data.admin(), hash);
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Created demo admin '{}'", admin.getUsername());
        }

        if (userRepository.existsByUsernameIgnoreCase(data.users().getFirst().username())) {
            log.info("Demo data already present, skipping");
            attachMissingPhotos(data);
            return;
        }

        Map<String, User> users = new HashMap<>();
        for (DemoUser u : data.users()) {
            users.put(u.username(), userRepository.save(toUser(u, hash)));
        }

        for (DemoRecipe r : data.recipes()) {
            User author = users.get(r.author());
            RecipeDetailResponse created = recipeService.create(new AuthUser(author.getId(), author.getUsername(), author.getRole()), r.recipe());
            jdbc.update("""
                    UPDATE recipes SET views = ?, avg_rating = ?, rating_count = ?, rating_sum = ROUND(? * ?),
                           created_at = now() - make_interval(days => ?), updated_at = now() - make_interval(days => ?)
                    WHERE id = ?""",
                    r.views(), r.avgRating(), r.ratingCount(), r.avgRating(), r.ratingCount(),
                    r.daysAgo(), r.daysAgo(), created.id());
        }
        log.info("Loaded demo data: {} cooks, {} recipes (password: {})", users.size(), data.recipes().size(), data.password());
        attachMissingPhotos(data);
    }

    /** Adds the bundled photo to every demo recipe that has no media yet. */
    private void attachMissingPhotos(DemoData data) throws IOException {
        if (!photos) {
            return;
        }
        int added = 0;
        for (DemoRecipe r : data.recipes()) {
            ClassPathResource photo = new ClassPathResource("demo/images/" + slug(r.recipe().title()) + ".jpg");
            if (!photo.exists()) {
                continue;
            }
            List<Long> ids = jdbc.queryForList("""
                    SELECT r.id FROM recipes r JOIN users u ON u.id = r.author_id
                    WHERE lower(u.username) = lower(?) AND r.title = ?
                      AND NOT EXISTS (SELECT 1 FROM recipe_media m WHERE m.recipe_id = r.id)""",
                    Long.class, r.author(), r.recipe().title());
            if (ids.isEmpty()) {
                continue;
            }
            User author = userRepository.findByUsernameIgnoreCase(r.author()).orElseThrow();
            AuthUser caller = new AuthUser(author.getId(), author.getUsername(), author.getRole());
            byte[] bytes;
            try (InputStream in = photo.getInputStream()) {
                bytes = in.readAllBytes();
            }
            for (Long id : ids) {
                mediaService.add(id, caller, bytes);
                added++;
            }
        }
        if (added > 0) {
            log.info("Added {} demo recipe photos", added);
        }
    }

    /** "Thai Green Curry with Tofu" → "thai-green-curry-with-tofu" (the photo's file name). */
    static String slug(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static User toUser(DemoUser u, String passwordHash) {
        User user = new User();
        user.setUsername(u.username());
        user.setEmail(u.email());
        user.setPasswordHash(passwordHash);
        user.setFirstName(u.firstName());
        user.setLastName(u.lastName());
        user.setAge(u.age());
        user.setGender(u.gender());
        user.setBio(u.bio());
        return user;
    }
}
