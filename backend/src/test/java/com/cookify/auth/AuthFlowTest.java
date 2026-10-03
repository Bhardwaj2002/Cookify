package com.cookify.auth;

import com.cookify.auth.controller.AuthCookies;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 2: signup, login, /me, refresh rotation, logout and bans, end to end against the test database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String PASSWORD = "Sup3rSecret!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }

    // ---- Signup ----------------------------------------------------------

    @Test
    void signupCreatesAccountAndLogsIn() throws Exception {
        MvcResult result = signup("chef_ayush", "Ayush@Example.com", "+919876543210", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.username").value("chef_ayush"))
                .andExpect(jsonPath("$.user.email").value("ayush@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("HttpOnly"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Path=/api/auth"))))
                .andReturn();

        assertThat(refreshCookie(result)).isNotNull();
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = 'chef_ayush'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void duplicateUsernameOrEmailIsRejectedIgnoringCase() throws Exception {
        signup("chef_ayush", "ayush@example.com", null, PASSWORD).andExpect(status().isCreated());

        signup("CHEF_AYUSH", "other@example.com", null, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));

        signup("someone_else", "AYUSH@example.com", null, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        signup("chef1", "a@example.com", null, "short12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("password"));

        signup("chef2", "b@example.com", null, "has spaces 123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("password"));
    }

    @Test
    void passwordConfirmationMustMatch() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"chef","email":"c@example.com","password":"%s","confirmPassword":"different123"}
                        """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("passwordConfirmed"))
                .andExpect(jsonPath("$.error.fields[0].message").value("Passwords do not match"));
    }

    @Test
    void invalidUsernameAndEmailAreRejected() throws Exception {
        signup("a b", "not-an-email", null, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field", org.hamcrest.Matchers.containsInAnyOrder("username", "email")));
    }

    // ---- Login -----------------------------------------------------------

    @Test
    void loginWorksWithUsernameEmailOrPhone() throws Exception {
        signup("chef_ayush", "ayush@example.com", "+919876543210", PASSWORD).andExpect(status().isCreated());

        for (String identifier : new String[]{"chef_ayush", "Chef_Ayush", "AYUSH@example.com", "+919876543210"}) {
            login(identifier, PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("chef_ayush"));
        }
    }

    @Test
    void wrongPasswordAndUnknownUserGiveSameError() throws Exception {
        signup("chef_ayush", "ayush@example.com", null, PASSWORD).andExpect(status().isCreated());

        login("chef_ayush", "WrongPassword1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        login("nobody", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void bannedUserCannotLogIn() throws Exception {
        signup("troll", "troll@example.com", null, PASSWORD).andExpect(status().isCreated());
        jdbc.update("UPDATE users SET banned_until = now() + interval '7 days' WHERE username = 'troll'");

        login("troll", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_BANNED"));
    }

    // ---- /me and access tokens ------------------------------------------

    @Test
    void meRequiresValidAccessToken() throws Exception {
        String token = accessToken(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chef_ayush"))
                .andExpect(jsonPath("$.email").value("ayush@example.com"));
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        signup("chef_ayush", "ayush@example.com", null, PASSWORD).andExpect(status().isCreated());
        Instant past = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cookify").subject("1").issuedAt(past.minusSeconds(900)).expiresAt(past)
                .claim("username", "chef_ayush").claim("role", "USER").build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    // ---- Refresh & logout ------------------------------------------------

    @Test
    void refreshRotatesTokenAndIssuesNewAccessToken() throws Exception {
        Cookie original = refreshCookie(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());

        MvcResult refreshed = refresh(original)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie rotated = refreshCookie(refreshed);

        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(refreshed)))
                .andExpect(status().isOk());
    }

    @Test
    void reusingOldRefreshTokenRevokesAllSessions() throws Exception {
        Cookie original = refreshCookie(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());
        Cookie rotated = refreshCookie(refresh(original).andExpect(status().isOk()).andReturn());
        // Move the rotation outside the 30s grace window used for parallel tabs.
        jdbc.update("UPDATE refresh_tokens SET revoked_at = now() - interval '1 minute' WHERE revoke_reason = 'ROTATED'");

        // Replay of the already-used token (e.g. stolen) is rejected...
        refresh(original)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        // ...and the legitimate new token is revoked too, forcing a fresh login.
        refresh(rotated).andExpect(status().isUnauthorized());
    }

    @Test
    void parallelRefreshWithinGraceWindowSucceeds() throws Exception {
        Cookie original = refreshCookie(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());

        // Two tabs load at the same time and both send the same cookie.
        refresh(original).andExpect(status().isOk());
        MvcResult second = refresh(original).andExpect(status().isOk()).andReturn();

        // The session survives: the second tab's new token keeps working.
        refresh(refreshCookie(second)).andExpect(status().isOk());
    }

    @Test
    void loggedOutTokenGetsNoGraceWindow() throws Exception {
        Cookie cookie = refreshCookie(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());
        mockMvc.perform(post("/api/auth/logout").cookie(cookie)).andExpect(status().isNoContent());

        refresh(cookie).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieFails() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        Cookie cookie = refreshCookie(signup("chef_ayush", "ayush@example.com", null, PASSWORD).andReturn());

        MvcResult logout = mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(refreshCookie(logout).getMaxAge()).isZero();

        refresh(cookie).andExpect(status().isUnauthorized());
    }

    // ---- Helpers ---------------------------------------------------------

    private ResultActions signup(String username, String email, String phone, String password) throws Exception {
        String phoneJson = phone == null ? "null" : "\"" + phone + "\"";
        return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","email":"%s","phone":%s,"password":"%s","confirmPassword":"%s"}
                """.formatted(username, email, phoneJson, password, password)));
    }

    private ResultActions login(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"identifier":"%s","password":"%s"}
                """.formatted(identifier, password)));
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(cookie));
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        return result.getResponse().getCookie(AuthCookies.REFRESH);
    }
}
