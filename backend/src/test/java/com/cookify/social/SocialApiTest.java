package com.cookify.social;

import com.cookify.support.AuthApi;
import com.cookify.support.RecipeJson;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.cookify.support.AuthApi.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 7: ratings, comments, saved recipes and subscriptions. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String author;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        AuthApi api = new AuthApi(mockMvc);
        author = json(api.signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");
        alice = json(api.signup("alice", "alice@example.com").andReturn(), "$.accessToken");
        bob = json(api.signup("bob", "bob@example.com").andReturn(), "$.accessToken");
        perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka()), author)
                .andExpect(status().isCreated());
    }

    // ---- Ratings -------------------------------------------------------------------

    @Test
    void ratingsAverageAndCanBeChangedOrRemoved() throws Exception {
        rate(alice, 5).andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(5.0))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.myRating").value(5));
        rate(bob, 2).andExpect(jsonPath("$.avgRating").value(3.5)).andExpect(jsonPath("$.ratingCount").value(2));

        // Re-rating replaces the old score instead of adding a second rating.
        rate(bob, 4).andExpect(jsonPath("$.avgRating").value(4.5)).andExpect(jsonPath("$.ratingCount").value(2));

        perform(delete("/api/recipes/1/rating"), alice)
                .andExpect(jsonPath("$.avgRating").value(4.0))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.myRating").doesNotExist());

        perform(get("/api/recipes/1"), bob)
                .andExpect(jsonPath("$.avgRating").value(4.0))
                .andExpect(jsonPath("$.viewer.myRating").value(4));
    }

    @Test
    void ratingKeepsExistingTotals() throws Exception {
        // e.g. demo recipes that start with 4.6 from 100 ratings
        jdbc.update("UPDATE recipes SET avg_rating = 4.6, rating_count = 100, rating_sum = 460 WHERE id = 1");
        rate(alice, 1).andExpect(jsonPath("$.avgRating").value(4.56)).andExpect(jsonPath("$.ratingCount").value(101));
    }

    @Test
    void ratingRules() throws Exception {
        rate(author, 5).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("CANNOT_RATE_OWN"));
        rate(alice, 6).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fields[0].field").value("score"));
        rate(alice, 0).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/recipes/1/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"))
                .andExpect(status().isUnauthorized());
        perform(put("/api/recipes/99/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"), alice)
                .andExpect(status().isNotFound());
    }

    // ---- Comments ------------------------------------------------------------------

    @Test
    void commentsAreListedNewestFirstAndCounted() throws Exception {
        comment(alice, "Loved it!").andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.username").value("alice"))
                .andExpect(jsonPath("$.canDelete").value(true));
        comment(bob, "  Added extra chilli.  ").andExpect(jsonPath("$.body").value("Added extra chilli."));

        mockMvc.perform(get("/api/recipes/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].body").value("Added extra chilli."))
                .andExpect(jsonPath("$.content[0].canDelete").value(false));
        mockMvc.perform(get("/api/recipes/1")).andExpect(jsonPath("$.commentCount").value(2));
        mockMvc.perform(get("/api/recipes")).andExpect(jsonPath("$.content[0].commentCount").value(2));
    }

    @Test
    void commentValidation() throws Exception {
        comment(alice, "   ").andExpect(status().isBadRequest());
        comment(alice, "x".repeat(1001)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recipes/1/comments").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void commentDeletePermissions() throws Exception {
        Integer aliceComment = JsonPath.read(comment(alice, "First!").andReturn().getResponse().getContentAsString(), "$.id");
        Integer bobComment = JsonPath.read(comment(bob, "Second").andReturn().getResponse().getContentAsString(), "$.id");

        perform(delete("/api/comments/" + aliceComment), bob)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_COMMENT_OWNER"));
        perform(delete("/api/comments/" + aliceComment), alice).andExpect(status().isNoContent());
        // The recipe's author can remove comments on their recipe.
        perform(get("/api/recipes/1/comments"), author).andExpect(jsonPath("$.content[0].canDelete").value(true));
        perform(delete("/api/comments/" + bobComment), author).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recipes/1")).andExpect(jsonPath("$.commentCount").value(0));
    }

    // ---- Saved -------------------------------------------------------------------------

    @Test
    void saveIsIdempotentAndListed() throws Exception {
        perform(put("/api/recipes/1/save"), alice).andExpect(status().isNoContent());
        perform(put("/api/recipes/1/save"), alice).andExpect(status().isNoContent());

        perform(get("/api/users/me/saved"), alice)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Paneer Tikka"));
        perform(get("/api/recipes/1"), alice).andExpect(jsonPath("$.viewer.saved").value(true));
        perform(get("/api/recipes/1"), bob).andExpect(jsonPath("$.viewer.saved").value(false));

        perform(delete("/api/recipes/1/save"), alice).andExpect(status().isNoContent());
        perform(get("/api/users/me/saved"), alice).andExpect(jsonPath("$.totalElements").value(0));
    }

    // ---- Follow ------------------------------------------------------------------------

    @Test
    void subscribeUpdatesCountsAndViewerFlags() throws Exception {
        perform(put("/api/users/chef_ayush/follow"), alice)
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));
        perform(put("/api/users/chef_ayush/follow"), alice).andExpect(jsonPath("$.followerCount").value(1));
        perform(put("/api/users/chef_ayush/follow"), bob).andExpect(jsonPath("$.followerCount").value(2));

        perform(get("/api/users/chef_ayush"), alice)
                .andExpect(jsonPath("$.followerCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(get("/api/users/alice")).andExpect(jsonPath("$.followingCount").value(1))
                .andExpect(jsonPath("$.following").value(false));
        perform(get("/api/recipes/1"), alice).andExpect(jsonPath("$.viewer.followingAuthor").value(true));

        perform(delete("/api/users/chef_ayush/follow"), alice)
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    void followRules() throws Exception {
        perform(put("/api/users/chef_ayush/follow"), author)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_FOLLOW_SELF"));
        perform(put("/api/users/nobody/follow"), alice).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/users/chef_ayush/follow")).andExpect(status().isUnauthorized());
    }

    // ---- Helpers -------------------------------------------------------------------------

    private ResultActions rate(String token, int score) throws Exception {
        return perform(put("/api/recipes/1/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":" + score + "}"), token);
    }

    private ResultActions comment(String token, String body) throws Exception {
        return perform(post("/api/recipes/1/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"), token);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
