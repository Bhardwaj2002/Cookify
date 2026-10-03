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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.cookify.support.AuthApi.PASSWORD;
import static com.cookify.support.AuthApi.deviceCookie;
import static com.cookify.support.AuthApi.json;
import static com.cookify.support.AuthApi.refreshCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 3: email 2FA codes, attempt limits, expiry and "remember this device". */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TwoFactorFlowTest {

    private static final String EMAIL = "ayush@example.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordingNotifier notifier;

    private AuthApi api;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        notifier.clear();
        api = new AuthApi(mockMvc);
        accessToken = json(api.signup("chef_ayush", EMAIL).andReturn(), "$.accessToken");
    }

    // ---- Enabling 2FA ----------------------------------------------------

    @Test
    void enablingTwoFactorRequiresCorrectPassword() throws Exception {
        api.setTwoFactor(accessToken, true, "WrongPass123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));

        api.setTwoFactor(accessToken, true, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorEnabled").value(true));
    }

    @Test
    void withoutTwoFactorLoginReturnsTokensDirectly() throws Exception {
        api.login("chef_ayush", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        assertThat(notifier.sentTo(EMAIL, EmailTemplate.LOGIN_OTP)).isEmpty();
    }

    // ---- Challenge + code ------------------------------------------------

    @Test
    void loginWithTwoFactorEmailsCodeAndReturnsChallenge() throws Exception {
        enableTwoFactor();

        MvcResult result = api.login("chef_ayush", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.challengeId").isNotEmpty())
                .andExpect(jsonPath("$.maskedEmail").value("a***h@example.com"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        assertThat(refreshCookie(result)).as("no session before the code is verified").isNull();
        assertThat(latestCode()).matches("\\d{6}");
    }

    @Test
    void correctCodeCompletesLoginOnlyOnce() throws Exception {
        enableTwoFactor();
        String challengeId = startChallenge();
        String code = latestCode();

        MvcResult result = api.verifyOtp(challengeId, code, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("chef_ayush"))
                .andReturn();
        assertThat(refreshCookie(result)).isNotNull();
        assertThat(deviceCookie(result)).as("device not remembered").isNull();

        api.verifyOtp(challengeId, code, false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));
    }

    @Test
    void wrongCodesAreLimitedToFiveAttempts() throws Exception {
        enableTwoFactor();
        String challengeId = startChallenge();
        String code = latestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";

        api.verifyOtp(challengeId, wrong, false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_OTP"))
                .andExpect(jsonPath("$.error.message").value("Incorrect code. 4 attempt(s) left."));
        for (int i = 0; i < 3; i++) {
            api.verifyOtp(challengeId, wrong, false).andExpect(status().isUnauthorized());
        }
        api.verifyOtp(challengeId, wrong, false)
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));

        // Even the right code no longer works once the challenge is burned.
        api.verifyOtp(challengeId, code, false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));
    }

    @Test
    void expiredCodeIsRejected() throws Exception {
        enableTwoFactor();
        String challengeId = startChallenge();
        jdbc.update("UPDATE otp_codes SET expires_at = now() - interval '1 minute'");

        api.verifyOtp(challengeId, latestCode(), false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));
    }

    @Test
    void newLoginInvalidatesPreviousChallenge() throws Exception {
        enableTwoFactor();
        String first = startChallenge();
        String firstCode = latestCode();
        startChallenge();

        api.verifyOtp(first, firstCode, false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));
    }

    @Test
    void malformedCodeIsRejectedByValidation() throws Exception {
        api.verifyOtp("anything", "12ab", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("code"));
    }

    // ---- Remember this device -------------------------------------------

    @Test
    void rememberedDeviceSkipsCodeUntilForgotten() throws Exception {
        enableTwoFactor();
        MvcResult verified = api.verifyOtp(startChallenge(), latestCode(), true)
                .andExpect(status().isOk())
                .andReturn();
        Cookie device = deviceCookie(verified);
        assertThat(device).isNotNull();
        assertThat(device.isHttpOnly()).isTrue();

        // Same browser: straight in, no new code.
        int codesBefore = notifier.sentTo(EMAIL, EmailTemplate.LOGIN_OTP).size();
        api.login("chef_ayush", PASSWORD, device)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        assertThat(notifier.sentTo(EMAIL, EmailTemplate.LOGIN_OTP)).hasSize(codesBefore);

        // Device appears in settings and can be forgotten.
        MvcResult list = mockMvc.perform(get("/api/users/me/devices").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();
        Integer deviceId = com.jayway.jsonpath.JsonPath.read(list.getResponse().getContentAsString(), "$[0].id");
        mockMvc.perform(delete("/api/users/me/devices/" + deviceId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNoContent());

        api.login("chef_ayush", PASSWORD, device)
                .andExpect(jsonPath("$.twoFactorRequired").value(true));
    }

    @Test
    void anotherUsersDeviceCookieDoesNotSkipCode() throws Exception {
        enableTwoFactor();
        Cookie myDevice = deviceCookie(api.verifyOtp(startChallenge(), latestCode(), true).andReturn());

        String otherToken = json(api.signup("other_chef", "other@example.com").andReturn(), "$.accessToken");
        api.setTwoFactor(otherToken, true, PASSWORD).andExpect(status().isOk());

        api.login("other_chef", PASSWORD, myDevice)
                .andExpect(jsonPath("$.twoFactorRequired").value(true));
    }

    @Test
    void disablingTwoFactorRestoresDirectLogin() throws Exception {
        enableTwoFactor();
        api.setTwoFactor(accessToken, false, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));

        api.login("chef_ayush", PASSWORD)
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ---- Helpers ---------------------------------------------------------

    private void enableTwoFactor() throws Exception {
        api.setTwoFactor(accessToken, true, PASSWORD).andExpect(status().isOk());
    }

    private String startChallenge() throws Exception {
        return json(api.login("chef_ayush", PASSWORD).andExpect(status().isOk()).andReturn(), "$.challengeId");
    }

    private String latestCode() {
        return (String) notifier.last(EMAIL, EmailTemplate.LOGIN_OTP).data().get("code");
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }
}
