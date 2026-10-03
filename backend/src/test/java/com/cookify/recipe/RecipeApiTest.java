package com.cookify.recipe;

import com.cookify.support.AuthApi;
import com.cookify.support.RecipeJson;
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
import org.springframework.test.web.servlet.ResultActions;

import static com.cookify.support.AuthApi.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 5: recipe create / read / update / delete, ownership, views and diet rules over HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String authorToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        AuthApi api = new AuthApi(mockMvc);
        authorToken = json(api.signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");
        otherToken = json(api.signup("other_cook", "other@example.com").andReturn(), "$.accessToken");
    }

    @Test
    void createReturnsFullRecipeWithDomainData() throws Exception {
        create(authorToken, RecipeJson.paneerTikka())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Paneer Tikka"))
                .andExpect(jsonPath("$.diet.type").value("VEG"))
                .andExpect(jsonPath("$.diet.label").value("Vegetarian"))
                .andExpect(jsonPath("$.totalMinutes").value(35))
                .andExpect(jsonPath("$.speed").value(3))
                .andExpect(jsonPath("$.cost").value(250.5))
                .andExpect(jsonPath("$.ingredients[1].name").value("hung curd"))
                .andExpect(jsonPath("$.ingredients[1].quantity").value("1/2 cup"))
                .andExpect(jsonPath("$.equipment.length()").value(2))
                .andExpect(jsonPath("$.steps[2]").value("Grill for 15 minutes."))
                .andExpect(jsonPath("$.author.username").value("chef_ayush"))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void ingredientsAndEquipmentAreSharedAcrossRecipes() throws Exception {
        create(authorToken, RecipeJson.paneerTikka()).andExpect(status().isCreated());
        create(otherToken, RecipeJson.recipe("Paneer Bhurji", "VEG", "[{\"name\":\"PANEER\"},{\"name\":\"Tomato\"}]"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ingredients WHERE name = 'paneer'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipment", Integer.class)).isEqualTo(2);
    }

    @Test
    void missingFieldsAreReported() throws Exception {
        create(authorToken, "{\"title\":\"\",\"ingredients\":[],\"steps\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field", hasItem("title")))
                .andExpect(jsonPath("$.error.fields[*].field", hasItem("dietType")))
                .andExpect(jsonPath("$.error.fields[*].field", hasItem("ingredients")))
                .andExpect(jsonPath("$.error.fields[*].field", hasItem("steps")));
    }

    @Test
    void dietSubclassRulesAreEnforced() throws Exception {
        create(authorToken, RecipeJson.recipe("Jain Pav Bhaji", "JAIN", "[{\"name\":\"Onion\"},{\"name\":\"Raw banana\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("ingredients"))
                .andExpect(jsonPath("$.error.fields[0].message").value("Jain recipes cannot contain onion (found \"onion\")"));

        create(authorToken, RecipeJson.recipe("Vegan Curry", "VEGAN", "[{\"name\":\"Coconut milk\"},{\"name\":\"Chickpeas\"}]"))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateIngredientIsRejected() throws Exception {
        create(authorToken, RecipeJson.recipe("Dal", "VEG", "[{\"name\":\"Toor dal\"},{\"name\":\"toor  DAL\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].message").value("Ingredient listed twice: toor dal"));
    }

    @Test
    void createRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyoneCanViewAndViewsAreCountedExceptForAuthor() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.views").value(1));
        mockMvc.perform(get("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(jsonPath("$.views").value(2))
                .andExpect(jsonPath("$.canEdit").value(false));
        mockMvc.perform(get("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(jsonPath("$.views").value(2))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void unknownRecipeIs404() throws Exception {
        mockMvc.perform(get("/api/recipes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void onlyAuthorCanUpdateOrDelete() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());

        update(otherToken, RecipeJson.paneerTikka())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_RECIPE_OWNER"));
        mockMvc.perform(delete("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditAnyRecipe() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE username = 'other_cook'");
        String adminToken = json(new AuthApi(mockMvc).login("other_cook", AuthApi.PASSWORD).andReturn(), "$.accessToken");

        update(adminToken, RecipeJson.recipe("Moderated title", "VEG", "[{\"name\":\"Paneer\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReplacesListsAndDietClass() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());

        update(authorToken, RecipeJson.recipe("Tofu Tikka", "VEGAN", "[{\"name\":\"Tofu\",\"quantity\":\"300 g\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tofu Tikka"))
                .andExpect(jsonPath("$.diet.label").value("Vegan"))
                .andExpect(jsonPath("$.ingredients.length()").value(1))
                .andExpect(jsonPath("$.ingredients[0].name").value("tofu"));
    }

    @Test
    void deleteRemovesRecipe() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());
        mockMvc.perform(delete("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/recipes/1")).andExpect(status().isNotFound());
    }

    @Test
    void listsShowNewestFirstAndProfileCountsRecipes() throws Exception {
        create(authorToken, RecipeJson.paneerTikka());
        create(otherToken, RecipeJson.recipe("Masala Chai", "VEG", "[{\"name\":\"Tea leaves\"}]"));
        create(authorToken, RecipeJson.recipe("Aloo Paratha", "VEG", "[{\"name\":\"Potato\"}]"));

        mockMvc.perform(get("/api/recipes").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Aloo Paratha"))
                .andExpect(jsonPath("$.content[0].diet.icon").value("🥦"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/users/chef_ayush/recipes"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].title").value("Paneer Tikka"));

        mockMvc.perform(get("/api/users/chef_ayush"))
                .andExpect(jsonPath("$.recipeCount").value(2));
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/recipes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(String token, String body) throws Exception {
        return mockMvc.perform(put("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
