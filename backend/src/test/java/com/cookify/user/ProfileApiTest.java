package com.cookify.user;

import com.cookify.support.AuthApi;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 4: edit profile, dietary preferences and public profiles. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        AuthApi api = new AuthApi(mockMvc);
        token = json(api.signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");
        api.signup("taken_name", "taken@example.com");
        jdbc.update("UPDATE users SET phone = '+911111111111' WHERE username = 'taken_name'");
    }

    // ---- Profile ---------------------------------------------------------

    @Test
    void updateProfileSavesAllFields() throws Exception {
        updateProfile("""
                {"username":"Chef_Ayush","firstName":"Ayush","lastName":"Singh","phone":"+919876543210",
                 "age":27,"gender":"MALE","bio":"Home cook. Paneer fan.","preferredLang":"hi"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Chef_Ayush"))
                .andExpect(jsonPath("$.firstName").value("Ayush"))
                .andExpect(jsonPath("$.age").value(27))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.bio").value("Home cook. Paneer fan."))
                .andExpect(jsonPath("$.preferredLang").value("hi"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.phone").value("+919876543210"));
    }

    @Test
    void blankOptionalFieldsAreCleared() throws Exception {
        updateProfile("""
                {"username":"chef_ayush","firstName":"  ","bio":"","preferredLang":"en"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void invalidProfileValuesAreRejected() throws Exception {
        updateProfile("""
                {"username":"x","age":12,"bio":"%s","preferredLang":"de","gender":"MALE"}""".formatted("a".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field", containsInAnyOrder("username", "age", "bio", "preferredLang")));
    }

    @Test
    void unknownGenderIsRejected() throws Exception {
        updateProfile("""
                {"username":"chef_ayush","gender":"ROBOT","preferredLang":"en"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotTakeAnotherUsersUsernameOrPhone() throws Exception {
        updateProfile("""
                {"username":"TAKEN_NAME","preferredLang":"en"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_TAKEN"));

        updateProfile("""
                {"username":"chef_ayush","phone":"+911111111111","preferredLang":"en"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PHONE_TAKEN"));
    }

    // ---- Preferences -----------------------------------------------------

    @Test
    void preferencesStartEmptyAndCanBeSaved() throws Exception {
        mockMvc.perform(get("/api/users/me/preferences").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietType").doesNotExist());

        updatePreferences("""
                {"dietType":"JAIN","maxCalories":600,"minProtein":20}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietType").value("JAIN"));

        updatePreferences("""
                {"dietType":"VEGAN","maxCalories":null,"minProtein":15}""")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/preferences").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.dietType").value("VEGAN"))
                .andExpect(jsonPath("$.maxCalories").doesNotExist())
                .andExpect(jsonPath("$.minProtein").value(15));
    }

    @Test
    void preferenceRangesAreValidated() throws Exception {
        updatePreferences("""
                {"dietType":"VEG","maxCalories":10,"minProtein":-1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[*].field", containsInAnyOrder("maxCalories", "minProtein")));
    }

    // ---- Public profile --------------------------------------------------

    @Test
    void publicProfileIsVisibleWithoutLoginAndHidesPrivateData() throws Exception {
        updateProfile("""
                {"username":"chef_ayush","firstName":"Ayush","phone":"+919876543210","age":27,"bio":"Hi!","preferredLang":"en"}""");

        mockMvc.perform(get("/api/users/CHEF_AYUSH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chef_ayush"))
                .andExpect(jsonPath("$.firstName").value("Ayush"))
                .andExpect(jsonPath("$.bio").value("Hi!"))
                .andExpect(jsonPath("$.age").value(27))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.twoFactorEnabled").doesNotExist());
    }

    @Test
    void unknownOrBannedUserIsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/nobody_here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));

        jdbc.update("UPDATE users SET permanently_banned = TRUE WHERE username = 'taken_name'");
        mockMvc.perform(get("/api/users/taken_name")).andExpect(status().isNotFound());
    }

    @Test
    void ownAccountEndpointsStillRequireLogin() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/preferences")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions updateProfile(String json) throws Exception {
        return mockMvc.perform(put("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions updatePreferences(String json) throws Exception {
        return mockMvc.perform(put("/api/users/me/preferences").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
