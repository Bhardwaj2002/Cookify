package com.cookify.security;

import com.cookify.security.ratelimit.RateLimiter;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.cookify.support.AuthApi.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 13: rate limits on abuse-prone endpoints and security headers. */
@SpringBootTest(properties = "app.rate-limit.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitAndHeadersTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
    }

    @Test
    void tenRapidLoginAttemptsThenTooManyRequests() throws Exception {
        for (int i = 0; i < 10; i++) {
            login("10.0.0.1").andExpect(status().isUnauthorized());
        }
        login("10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

        // Another client is not affected.
        login("10.0.0.2").andExpect(status().isUnauthorized());
    }

    @Test
    void commentFloodingIsLimitedPerUser() throws Exception {
        String token = json(new AuthApi(mockMvc).signup("chatty", "chatty@example.com").andReturn(), "$.accessToken");
        mockMvc.perform(post("/api/recipes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka())).andExpect(status().isCreated());

        for (int i = 0; i < 20; i++) {
            comment(token, "Tip " + i).andExpect(status().isCreated());
        }
        comment(token, "One too many").andExpect(status().isTooManyRequests());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", org.hamcrest.Matchers.containsString("camera=()")));
    }

    @Test
    void slidingWindowFreesUpAfterTheWindow() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, Duration.ofSeconds(60))).isZero();
        }
        assertThat(limiter.tryAcquire("k", 3, Duration.ofSeconds(60))).isEqualTo(60);
        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("k", 3, Duration.ofSeconds(60))).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions login(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(r -> {
            r.setRemoteAddr(ip);
            return r;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"nobody\",\"password\":\"WrongPass123\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions comment(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/recipes/1/comments").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"));
    }

    /** Test clock that can be moved forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-03T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
