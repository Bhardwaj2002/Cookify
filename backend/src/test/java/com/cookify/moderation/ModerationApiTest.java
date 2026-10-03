package com.cookify.moderation;

import com.cookify.notification.EmailTemplate;
import com.cookify.support.AuthApi;
import com.cookify.support.RecipeJson;
import com.cookify.support.RecordingNotifier;
import jakarta.servlet.http.Cookie;
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

import static com.cookify.support.AuthApi.PASSWORD;
import static com.cookify.support.AuthApi.json;
import static com.cookify.support.AuthApi.refreshCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 8: warnings for abusive content, automatic ban at 3, content removal, admin tools. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModerationApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordingNotifier notifier;

    private AuthApi api;
    private String chef;
    private String troll;
    private Cookie trollSession;
    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        notifier.clear();
        api = new AuthApi(mockMvc);
        chef = json(api.signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");   // id 1
        var trollSignup = api.signup("troll", "troll@example.com").andReturn();                      // id 2
        troll = json(trollSignup, "$.accessToken");
        trollSession = refreshCookie(trollSignup);
        api.signup("boss", "boss@example.com");                                                      // id 3
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE username = 'boss'");
        admin = json(api.login("boss", PASSWORD).andReturn(), "$.accessToken");

        // Chef's recipe (id 1); the troll also has a recipe (id 2), rates and follows before misbehaving.
        perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka()), chef);
        perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                .content(RecipeJson.recipe("Troll Toast", "VEG", "[{\"name\":\"Bread\"}]")), troll);
        perform(put("/api/recipes/1/rating").contentType(MediaType.APPLICATION_JSON).content("{\"score\":1}"), troll);
        perform(put("/api/users/chef_ayush/follow"), troll);
        comment(troll, "Nice one").andExpect(status().isCreated());
    }

    @Test
    void abusiveCommentIsBlockedAndWarned() throws Exception {
        comment(troll, "this is sh1t")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CONTENT_BLOCKED"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("warning 1 of 3")));

        assertThat(jdbc.queryForObject("SELECT warnings_count FROM users WHERE id = 2", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT content_snippet FROM warnings WHERE user_id = 2", String.class)).isEqualTo("this is sh1t");
        assertThat(notifier.last("troll@example.com", EmailTemplate.ACCOUNT_WARNING).data().get("count")).isEqualTo(1);
        // The blocked comment was not saved.
        mockMvc.perform(get("/api/recipes/1/comments")).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void abusiveRecipeTextIsBlockedToo() throws Exception {
        perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON)
                .content(RecipeJson.recipe("Recipe for idiots", "VEG", "[{\"name\":\"Bread\"}]")), troll)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CONTENT_BLOCKED"));
        assertThat(jdbc.queryForObject("SELECT reason FROM warnings WHERE user_id = 2", String.class)).isEqualTo("ABUSIVE_RECIPE");
    }

    @Test
    void thirdWarningBansAndRemovesContent() throws Exception {
        comment(troll, "idiot").andExpect(status().isBadRequest());
        comment(troll, "moron").andExpect(status().isBadRequest());
        comment(troll, "loser")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_BANNED"));

        // Banned for 30 days with a reason; email sent.
        assertThat(jdbc.queryForObject("SELECT banned_until > now() + interval '29 days' FROM users WHERE id = 2", Boolean.class)).isTrue();
        assertThat(notifier.sentTo("troll@example.com", EmailTemplate.ACCOUNT_BANNED)).hasSize(1);

        // Content removed: their recipe, comment, rating (average restored) and follow.
        mockMvc.perform(get("/api/recipes/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.avgRating").value(0))
                .andExpect(jsonPath("$.commentCount").value(0));
        mockMvc.perform(get("/api/users/chef_ayush")).andExpect(jsonPath("$.followerCount").value(0));

        // Still-valid access token and refresh cookie no longer work; login is refused.
        perform(get("/api/users/me"), troll).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCOUNT_BANNED"));
        mockMvc.perform(post("/api/auth/refresh").cookie(trollSession)).andExpect(status().isUnauthorized());
        api.login("troll", PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCOUNT_BANNED"));
        mockMvc.perform(get("/api/users/troll")).andExpect(status().isNotFound());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        perform(get("/api/admin/users"), chef).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        perform(get("/api/admin/users"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void adminCanListWarnedUsersAndSeeWarnings() throws Exception {
        comment(troll, "idiot");
        perform(get("/api/admin/users").param("status", "WARNED"), admin)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("troll"))
                .andExpect(jsonPath("$.content[0].warningsCount").value(1));
        perform(get("/api/admin/users/2/warnings"), admin)
                .andExpect(jsonPath("$[0].reason").value("ABUSIVE_COMMENT"))
                .andExpect(jsonPath("$[0].contentSnippet").value("idiot"));
        perform(get("/api/admin/users").param("q", "TROL"), admin).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminCanBanPermanentlyAndUnban() throws Exception {
        perform(post("/api/admin/users/2/ban").contentType(MediaType.APPLICATION_JSON)
                .content("{\"permanent\":true,\"reason\":\"Spam\"}"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true))
                .andExpect(jsonPath("$.permanentlyBanned").value(true))
                .andExpect(jsonPath("$.banReason").value("Spam"));
        perform(get("/api/admin/users").param("status", "BANNED"), admin).andExpect(jsonPath("$.totalElements").value(1));
        api.login("troll", PASSWORD).andExpect(status().isForbidden());

        perform(post("/api/admin/users/2/unban"), admin)
                .andExpect(jsonPath("$.banned").value(false))
                .andExpect(jsonPath("$.warningsCount").value(0));
        api.login("troll", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void banRequestValidationAndAdminProtection() throws Exception {
        perform(post("/api/admin/users/2/ban").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("durationChosen"));
        perform(post("/api/admin/users/3/ban").contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":7,\"reason\":\"x\"}"), admin)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CANNOT_BAN_ADMIN"));
    }

    private ResultActions comment(String token, String body) throws Exception {
        return perform(post("/api/recipes/1/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"), token);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
