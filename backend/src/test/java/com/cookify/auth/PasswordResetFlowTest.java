package com.cookify.auth;

import com.cookify.notification.EmailTemplate;
import com.cookify.support.AuthApi;
import com.cookify.support.RecordingNotifier;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;

import static com.cookify.support.AuthApi.PASSWORD;
import static com.cookify.support.AuthApi.refreshCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 3: "Forgot password?" emails a one-time link; resetting signs the user out everywhere. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowTest {

    private static final String EMAIL = "ayush@example.com";
    private static final String NEW_PASSWORD = "BrandN3w!Pass";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordingNotifier notifier;

    private AuthApi api;
    private Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        notifier.clear();
        api = new AuthApi(mockMvc);
        sessionCookie = refreshCookie(api.signup("chef_ayush", EMAIL).andReturn());
    }

    @Test
    void unknownAccountGetsSameResponseAndNoEmail() throws Exception {
        forgot("nobody@example.com").andExpect(status().isAccepted());
        assertThat(notifier.sentTo("nobody@example.com", EmailTemplate.PASSWORD_RESET)).isEmpty();
    }

    @Test
    void resetLinkIsEmailedForUsernameEmailOrPhone() throws Exception {
        forgot("chef_ayush").andExpect(status().isAccepted());
        String link = (String) notifier.last(EMAIL, EmailTemplate.PASSWORD_RESET).data().get("link");
        assertThat(link).startsWith("http://localhost:8080/pages/reset-password.html?token=");
    }

    @Test
    void resetChangesPasswordAndRevokesSessions() throws Exception {
        forgot(EMAIL).andExpect(status().isAccepted());

        reset(latestToken(), NEW_PASSWORD).andExpect(status().isNoContent());

        api.login("chef_ayush", PASSWORD).andExpect(status().isUnauthorized());
        api.login("chef_ayush", NEW_PASSWORD).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetTokenWorksOnlyOnce() throws Exception {
        forgot(EMAIL);
        String token = latestToken();
        reset(token, NEW_PASSWORD).andExpect(status().isNoContent());

        reset(token, "Another1Pass!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        forgot(EMAIL);
        jdbc.update("UPDATE password_reset_tokens SET expires_at = now() - interval '1 minute'");

        reset(latestToken(), NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void newRequestInvalidatesOlderLink() throws Exception {
        forgot(EMAIL);
        String first = latestToken();
        forgot(EMAIL);

        reset(first, NEW_PASSWORD).andExpect(status().isBadRequest());
        reset(latestToken(), NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void newPasswordMustFollowPasswordRules() throws Exception {
        forgot(EMAIL);
        reset(latestToken(), "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("password"));
    }

    private ResultActions forgot(String identifier) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"%s\"}".formatted(identifier)));
    }

    private ResultActions reset(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"password\":\"%s\",\"confirmPassword\":\"%s\"}".formatted(token, password, password)));
    }

    private String latestToken() {
        String link = (String) notifier.last(EMAIL, EmailTemplate.PASSWORD_RESET).data().get("link");
        return UriComponentsBuilder.fromUriString(link).build().getQueryParams().getFirst("token");
    }
}
