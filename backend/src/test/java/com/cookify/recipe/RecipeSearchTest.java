package com.cookify.recipe;

import com.cookify.demo.DemoDataLoader;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 6: every filter and sort, run against the 39-recipe demo dataset. Loading the dataset
 * through RecipeService also proves all demo recipes satisfy their diet rules.
 */
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeSearchTest {

    private static final int TOTAL = 39;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DemoDataLoader loader;

    @BeforeEach
    void loadDemoData() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        loader.run(null);
    }

    // ---- No filters, paging ----------------------------------------------------

    @Test
    void noFiltersReturnsEverythingPaged() throws Exception {
        mockMvc.perform(get("/api/recipes").param("size", "5").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(TOTAL))
                .andExpect(jsonPath("$.totalPages").value(8));
    }

    // ---- Keyword search ----------------------------------------------------------

    @Test
    void keywordSearchesTitleDescriptionAndIngredients() throws Exception {
        assertThat(titles("q", "paneer")).containsExactly("Jain Paneer Butter Masala");
        assertThat(titles("q", "pan")).contains("Jain Paneer Butter Masala", "Classic Fluffy Pancakes"); // partial title
        assertThat(titles("q", "tofu")).containsExactly("Thai Green Curry with Tofu");
        assertThat(titles("q", "chickpea")).containsExactlyInAnyOrder("Hummus", "Chana Masala", "Quinoa Buddha Bowl"); // ingredient
        assertThat(titles("q", "noodle")).contains("Veg Hakka Noodles", "Pad Thai"); // stemming: "noodles"
        assertThat(titles("q", "creamy")).contains("Jain Paneer Butter Masala", "Mushroom Risotto"); // description
    }

    // ---- Diet (uses the Recipe class hierarchy) ---------------------------------------

    @Test
    void dietFilterFollowsRecipeSubclasses() throws Exception {
        assertThat(all("diet", "VEG")).allSatisfy(r -> assertThat(diet(r)).isIn("VEG", "VEGAN", "JAIN")).hasSize(26);
        assertThat(all("diet", "JAIN")).allSatisfy(r -> assertThat(diet(r)).isEqualTo("JAIN")).hasSize(4);
        assertThat(all("diet", "VEGAN")).allSatisfy(r -> assertThat(diet(r)).isEqualTo("VEGAN")).hasSize(12);
        assertThat(all("diet", "EGGETARIAN")).allSatisfy(r -> assertThat(diet(r)).isNotIn("PESCETARIAN", "NON_VEG"));
        assertThat(all("diet", "NON_VEG")).hasSize(TOTAL);
    }

    // ---- Cuisine / course ------------------------------------------------------------

    @Test
    void cuisineAndCourseAcceptSeveralValues() throws Exception {
        assertThat(all("cuisine", "PAN_ASIAN,THAI")).extracting(r -> r.get("cuisine")).containsOnly("PAN_ASIAN", "THAI").hasSize(3);
        assertThat(titles("course", "DESSERT")).containsExactlyInAnyOrder("Gulab Jamun", "Eggless Chocolate Cake");
    }

    // ---- Ingredients & equipment -----------------------------------------------------

    @Test
    void ingredientsAllModeNeedsEveryIngredient() throws Exception {
        assertThat(titles("ingredients", "paneer")).containsExactly("Jain Paneer Butter Masala");
        List<String> both = titles("ingredients", "tomato,garlic");
        assertThat(both).contains("Shakshuka", "Penne Arrabbiata", "Tomato Basil Soup").doesNotContain("Hummus", "Masala Dosa");
    }

    @Test
    void ingredientsOnlyModeFindsWhatYouCanCook() throws Exception {
        // Mustard seeds, curry leaves, turmeric, oil and salt are pantry staples.
        assertThat(titles("ingredients", "cooked rice,lemon,peanuts,green chilli", "ingredientMatch", "ONLY"))
                .containsExactly("Lemon Rice");
        assertThat(titles("ingredients", "banana,oats,almond milk,peanut butter,dates", "ingredientMatch", "ONLY"))
                .containsExactly("Banana Oat Smoothie");
        assertThat(titles("ingredients", "banana", "ingredientMatch", "ONLY")).isEmpty();
    }

    @Test
    void equipmentFilterKeepsRecipesYouCanMake() throws Exception {
        List<String> blenderOnly = titles("equipment", "blender");
        assertThat(blenderOnly).containsExactlyInAnyOrder("Mango Lassi", "Hummus", "Banana Oat Smoothie", "Greek Salad");
        assertThat(titles("equipment", "wok,saucepan,sushi mat"))
                .contains("Veg Hakka Noodles", "Vegetable Sushi Rolls", "Greek Salad")
                .doesNotContain("Margherita Pizza");
    }

    // ---- Numeric ranges --------------------------------------------------------------

    @Test
    void numericFiltersRespectLimits() throws Exception {
        assertThat(all("maxCost", "100")).allSatisfy(r -> assertThat(num(r, "cost")).isLessThanOrEqualTo(100)).isNotEmpty();
        assertThat(all("maxMinutes", "15")).allSatisfy(r -> assertThat(num(r, "totalMinutes")).isLessThanOrEqualTo(15)).isNotEmpty();
        assertThat(all("maxCalories", "250")).allSatisfy(r -> assertThat(num(r, "calories")).isLessThanOrEqualTo(250)).isNotEmpty();
        assertThat(all("maxDifficulty", "1")).allSatisfy(r -> assertThat(num(r, "difficulty")).isEqualTo(1)).isNotEmpty();
        assertThat(all("minRating", "4.8")).allSatisfy(r -> assertThat(num(r, "avgRating")).isGreaterThanOrEqualTo(4.8)).hasSize(6);
        assertThat(titles("minProtein", "35")).containsExactlyInAnyOrder("Tandoori Chicken", "Mutton Rogan Josh", "Fish and Chips", "Full English Breakfast");
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        List<Map<String, Object>> found = all("diet", "VEG", "course", "BREAKFAST", "maxMinutes", "30");
        assertThat(found).isNotEmpty().allSatisfy(r -> {
            assertThat(diet(r)).isIn("VEG", "VEGAN", "JAIN");
            assertThat(r.get("course")).isEqualTo("BREAKFAST");
            assertThat(num(r, "totalMinutes")).isLessThanOrEqualTo(30);
        });
        assertThat(found).extracting(r -> r.get("title")).contains("Moong Dal Chilla").doesNotContain("Masala Dosa", "Egg Bhurji");
    }

    // ---- Sorting -----------------------------------------------------------------------

    @Test
    void sortStrategiesOrderResults() throws Exception {
        assertThat(titles("sort", "POPULAR").getFirst()).isEqualTo("Butter Chicken");
        assertThat(titles("sort", "TOP_RATED").subList(0, 2)).containsExactly("Butter Chicken", "Gulab Jamun");
        assertThat(titles("sort", "NEWEST").getFirst()).isEqualTo("Classic Fluffy Pancakes");

        List<Map<String, Object>> quickest = all("sort", "QUICKEST");
        assertThat(quickest).extracting(r -> num(r, "totalMinutes")).isSorted();
        List<Map<String, Object>> cheapest = all("sort", "CHEAPEST");
        assertThat(cheapest).extracting(r -> num(r, "cost")).isSorted();
    }

    // ---- Validation & options ----------------------------------------------------------

    @Test
    void invalidParametersAreRejected() throws Exception {
        mockMvc.perform(get("/api/recipes").param("sort", "RANDOM")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/recipes").param("maxDifficulty", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("maxDifficulty"));
        mockMvc.perform(get("/api/recipes").param("diet", "CARNIVORE")).andExpect(status().isBadRequest());
    }

    @Test
    void filterOptionsListChoicesAndRanges() throws Exception {
        mockMvc.perform(get("/api/meta/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diets.length()").value(6))
                .andExpect(jsonPath("$.cuisines.length()").value(13))
                .andExpect(jsonPath("$.ingredients[0]").value("salt")) // most used
                .andExpect(jsonPath("$.equipment").isNotEmpty())
                .andExpect(jsonPath("$.ranges.maxCost").value(700.0))
                .andExpect(jsonPath("$.ranges.maxMinutes").value(135));
    }

    // ---- Helpers -------------------------------------------------------------------------

    private List<Map<String, Object>> all(String... params) throws Exception {
        var request = get("/api/recipes").param("size", "50");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content");
    }

    private List<String> titles(String... params) throws Exception {
        return all(params).stream().map(r -> (String) r.get("title")).toList();
    }

    @SuppressWarnings("unchecked")
    private static String diet(Map<String, Object> r) {
        return (String) ((Map<String, Object>) r.get("diet")).get("type");
    }

    private static double num(Map<String, Object> r, String key) {
        return ((Number) r.get(key)).doubleValue();
    }
}
