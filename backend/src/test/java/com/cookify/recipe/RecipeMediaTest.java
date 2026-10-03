package com.cookify.recipe;

import com.cookify.media.LocalStorageService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.cookify.support.AuthApi.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 5: photo / video attachments on recipes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeMediaTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LocalStorageService storage;

    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        AuthApi api = new AuthApi(mockMvc);
        token = json(api.signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");
        otherToken = json(api.signup("other_cook", "other@example.com").andReturn(), "$.accessToken");
        mockMvc.perform(post("/api/recipes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(RecipeJson.paneerTikka())).andExpect(status().isCreated());
    }

    @Test
    void imageAndVideoAreAttachedInOrder() throws Exception {
        String imageUrl = json(upload(token, "photo.png", png()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IMAGE")).andReturn(), "$.url");
        upload(token, "clip.mp4", mp4()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VIDEO"))
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.endsWith(".mp4")));

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(jsonPath("$.media.length()").value(2))
                .andExpect(jsonPath("$.media[0].type").value("IMAGE"))
                .andExpect(jsonPath("$.media[1].type").value("VIDEO"));
        mockMvc.perform(get("/api/recipes"))
                .andExpect(jsonPath("$.content[0].coverUrl").value(imageUrl));
        assertThat(Files.exists(fileFor(imageUrl))).isTrue();
    }

    @Test
    void nonMediaFilesAreRejected() throws Exception {
        upload(token, "notes.mp4", "just text".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MEDIA"));
    }

    @Test
    void onlyAuthorCanAttachMedia() throws Exception {
        upload(otherToken, "photo.png", png())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_RECIPE_OWNER"));
    }

    @Test
    void atMostTenItems() throws Exception {
        for (int i = 0; i < 10; i++) {
            upload(token, "p.png", png()).andExpect(status().isCreated());
        }
        upload(token, "p.png", png())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_MEDIA"));
    }

    @Test
    void removingMediaDeletesFile() throws Exception {
        String url = json(upload(token, "photo.png", png()).andReturn(), "$.url");
        Integer mediaId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/recipes/1")).andReturn().getResponse().getContentAsString(), "$.media[0].id");

        mockMvc.perform(delete("/api/recipes/1/media/" + mediaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(Files.exists(fileFor(url))).isFalse();
        mockMvc.perform(get("/api/recipes/1")).andExpect(jsonPath("$.media.length()").value(0));
    }

    @Test
    void deletingRecipeDeletesItsFiles() throws Exception {
        String url = json(upload(token, "photo.png", png()).andReturn(), "$.url");
        mockMvc.perform(delete("/api/recipes/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(Files.exists(fileFor(url))).isFalse();
    }

    private ResultActions upload(String bearer, String name, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/recipes/1/media")
                .file(new MockMultipartFile("file", name, "application/octet-stream", bytes))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer));
    }

    private Path fileFor(String url) {
        return storage.root().resolve(url.substring(url.indexOf("/media/") + "/media/".length()));
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    /** Minimal ISO-BMFF header: size + "ftyp" + brand "isom". Enough for type detection. */
    private static byte[] mp4() {
        byte[] b = new byte[64];
        byte[] head = {0, 0, 0, 0x20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        System.arraycopy(head, 0, b, 0, head.length);
        return b;
    }
}
