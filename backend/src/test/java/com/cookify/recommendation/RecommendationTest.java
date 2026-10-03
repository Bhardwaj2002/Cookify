package com.cookify.recommendation;

import com.cookify.demo.DemoDataLoader;
import com.cookify.support.AuthApi;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static com.cookify.support.AuthApi.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 10: "For You" recommendations on the demo dataset. */
@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DemoDataLoader loader;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        loader.run(null);
        token = json(new AuthApi(mockMvc).signup("hungry", "hungry@example.com").andReturn(), "$.accessToken");
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/users/me/recommendations")).andExpect(status().isUnauthorized());
    }

    @Test
    void withoutPreferencesEverythingIsTrending() throws Exception {
        List<Map<String, Object>> feed = feed();
        assertThat(feed).hasSize(39);
        assertThat(title(feed.getFirst())).isEqualTo("Butter Chicken");
        assertThat(reasons(feed.getFirst())).containsExactly("TRENDING");
    }

    @Test
    void vegetarianNeverSeesNonVeg() throws Exception {
        setPreferences("{\"dietType\":\"VEG\"}");
        List<Map<String, Object>> feed = feed();
        assertThat(feed).hasSize(26).allSatisfy(r -> assertThat(diet(r)).isIn("VEG", "VEGAN", "JAIN"));
        assertThat(reasons(feed.getFirst())).contains("FITS_DIET");
    }

    @Test
    void jainSeesOnlyJainRecipes() throws Exception {
        setPreferences("{\"dietType\":\"JAIN\"}");
        assertThat(feed()).hasSize(4).allSatisfy(r -> assertThat(diet(r)).isEqualTo("JAIN"));
    }

    @Test
    void followedCooksRankHigher() throws Exception {
        perform(put("/api/users/chef_kabir/follow")).andExpect(status().isOk());
        List<Map<String, Object>> feed = feed();
        Map<String, Object> top = feed.getFirst();
        assertThat(author(top)).isEqualTo("chef_kabir");
        assertThat(reasons(top)).contains("FOLLOWED_AUTHOR");
    }

    @Test
    void ratedRecipesDisappearAndLikedCuisineIsBoosted() throws Exception {
        Integer dosa = JsonPath.read(mockMvc.perform(get("/api/recipes").param("q", "dosa")).andReturn()
                .getResponse().getContentAsString(), "$.content[0].id");
        perform(put("/api/recipes/" + dosa + "/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"))
                .andExpect(status().isOk());

        List<Map<String, Object>> feed = feed();
        assertThat(feed).hasSize(38).noneMatch(r -> title(r).equals("Masala Dosa"));
        // Loving an Indian dish boosts other Indian recipes.
        assertThat(feed.stream().filter(r -> reasons(r).contains("LIKED_CUISINE"))).isNotEmpty()
                .allSatisfy(r -> assertThat(((Map<?, ?>) r.get("recipe")).get("cuisine")).isEqualTo("INDIAN"));
    }

    @Test
    void ownRecipesAreExcluded() throws Exception {
        String meera = json(new AuthApi(mockMvc).login("chef_meera", "Cookify2026!").andReturn(), "$.accessToken");
        String body = mockMvc.perform(get("/api/users/me/recommendations").param("size", "50")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + meera)).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> feed = JsonPath.read(body, "$.content");
        assertThat(feed).noneMatch(r -> author(r).equals("chef_meera"));
    }

    @Test
    void nutritionTargetsAddReasons() throws Exception {
        setPreferences("{\"dietType\":\"VEG\",\"maxCalories\":250,\"minProtein\":7}");
        List<Map<String, Object>> feed = feed();
        assertThat(feed.stream().filter(r -> reasons(r).contains("FITS_CALORIES")))
                .isNotEmpty()
                .allSatisfy(r -> assertThat(((Number) ((Map<?, ?>) r.get("recipe")).get("calories")).intValue()).isLessThanOrEqualTo(250));
    }

    // ---- Helpers -------------------------------------------------------------------------

    private void setPreferences(String json) throws Exception {
        perform(put("/api/users/me/preferences").contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
    }

    private List<Map<String, Object>> feed() throws Exception {
        String body = perform(get("/api/users/me/recommendations").param("size", "50"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.content");
    }

    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> recipe(Map<String, Object> r) {
        return (Map<String, Object>) r.get("recipe");
    }

    private static String title(Map<String, Object> r) {
        return (String) recipe(r).get("title");
    }

    @SuppressWarnings("unchecked")
    private static String diet(Map<String, Object> r) {
        return (String) ((Map<String, Object>) recipe(r).get("diet")).get("type");
    }

    @SuppressWarnings("unchecked")
    private static String author(Map<String, Object> r) {
        return (String) ((Map<String, Object>) recipe(r).get("author")).get("username");
    }

    @SuppressWarnings("unchecked")
    private static List<String> reasons(Map<String, Object> r) {
        return (List<String>) r.get("reasons");
    }
}
