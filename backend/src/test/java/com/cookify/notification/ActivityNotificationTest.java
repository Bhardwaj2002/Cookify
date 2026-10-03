package com.cookify.notification;

import com.cookify.support.AuthApi;
import com.cookify.support.RecipeJson;
import com.cookify.support.RecordingNotifier;
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

import java.util.function.Supplier;

import static com.cookify.support.AuthApi.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 9: activity emails, sent asynchronously after commit. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityNotificationTest {

    private static final String CHEF = "ayush@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordingNotifier notifier;

    private String chef;
    private String alice;
    private String bob;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        notifier.clear();
        AuthApi api = new AuthApi(mockMvc);
        chef = json(api.signup("chef_ayush", CHEF).andReturn(), "$.accessToken");
        alice = json(api.signup("alice", "alice@example.com").andReturn(), "$.accessToken");
        bob = json(api.signup("bob", "bob@example.com").andReturn(), "$.accessToken");
    }

    @Test
    void subscribersAreEmailedAboutNewRecipes() throws Exception {
        perform(put("/api/users/chef_ayush/follow"), alice).andExpect(status().isOk());
        perform(put("/api/users/chef_ayush/follow"), bob).andExpect(status().isOk());
        createRecipe();

        await(() -> notifier.sentTo("alice@example.com", EmailTemplate.NEW_RECIPE).size() == 1
                && notifier.sentTo("bob@example.com", EmailTemplate.NEW_RECIPE).size() == 1);
        var mail = notifier.last("alice@example.com", EmailTemplate.NEW_RECIPE);
        assertThat(mail.data().get("title")).isEqualTo("Paneer Tikka");
        assertThat((String) mail.data().get("link")).isEqualTo("http://localhost:8080/pages/recipe.html?id=1");
        // The author does not get their own announcement.
        assertThat(notifier.sentTo(CHEF, EmailTemplate.NEW_RECIPE)).isEmpty();
    }

    @Test
    void ownerIsEmailedAboutNewSubscriber() throws Exception {
        perform(put("/api/users/chef_ayush/follow"), alice);
        perform(put("/api/users/chef_ayush/follow"), alice); // repeat: no second email

        await(() -> notifier.sentTo(CHEF, EmailTemplate.NEW_FOLLOWER).size() == 1);
        assertThat(notifier.last(CHEF, EmailTemplate.NEW_FOLLOWER).data().get("count")).isEqualTo(1L);
        Thread.sleep(200);
        assertThat(notifier.sentTo(CHEF, EmailTemplate.NEW_FOLLOWER)).hasSize(1);
    }

    @Test
    void ownerIsEmailedAboutCommentsButNotTheirOwn() throws Exception {
        createRecipe();
        comment(alice, "Fantastic!").andExpect(status().isCreated());
        comment(chef, "Thanks Alice!").andExpect(status().isCreated());

        await(() -> notifier.sentTo(CHEF, EmailTemplate.NEW_COMMENT).size() == 1);
        assertThat(notifier.last(CHEF, EmailTemplate.NEW_COMMENT).data().get("comment")).isEqualTo("Fantastic!");
        Thread.sleep(200);
        assertThat(notifier.sentTo(CHEF, EmailTemplate.NEW_COMMENT)).hasSize(1);
    }

    @Test
    void ownerIsEmailedAboutFirstRatingOnly() throws Exception {
        createRecipe();
        rate(alice, 4);
        rate(alice, 5); // changed rating: no new email

        await(() -> notifier.sentTo(CHEF, EmailTemplate.NEW_RATING).size() == 1);
        assertThat(notifier.last(CHEF, EmailTemplate.NEW_RATING).data().get("stars")).isEqualTo("★★★★☆");
        Thread.sleep(200);
        assertThat(notifier.sentTo(CHEF, EmailTemplate.NEW_RATING)).hasSize(1);
    }

    @Test
    void blockedCommentSendsNoActivityEmail() throws Exception {
        createRecipe();
        comment(alice, "idiot").andExpect(status().isBadRequest());
        Thread.sleep(300);
        assertThat(notifier.sentTo(CHEF, EmailTemplate.NEW_COMMENT)).isEmpty();
    }

    // ---- Helpers --------------------------------------------------------------------------

    private void createRecipe() throws Exception {
        perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka()), chef)
                .andExpect(status().isCreated());
    }

    private ResultActions comment(String token, String body) throws Exception {
        return perform(post("/api/recipes/1/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"), token);
    }

    private void rate(String token, int score) throws Exception {
        perform(put("/api/recipes/1/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":" + score + "}"), token)
                .andExpect(status().isOk());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /** Emails are sent on a background thread: poll for up to 5 seconds. */
    private static void await(Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.get()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Expected email was not sent within 5 seconds");
            }
            Thread.sleep(50);
        }
    }
}
