package com.cookify.user;

import com.cookify.media.LocalStorageService;
import com.cookify.support.AuthApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Step 4: profile picture upload, validation, replacement and removal. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarUploadTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LocalStorageService storage;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        token = json(new AuthApi(mockMvc).signup("chef_ayush", "ayush@example.com").andReturn(), "$.accessToken");
    }

    @Test
    void uploadedAvatarIsStoredAndServed() throws Exception {
        String url = json(upload("me.png", "image/png", png()).andExpect(status().isOk()).andReturn(), "$.avatarUrl");

        assertThat(url).startsWith("http://localhost:8080/media/avatars/1/").endsWith(".png");
        String path = url.substring("http://localhost:8080".length());
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        assertThat(Files.exists(fileFor(url))).isTrue();
    }

    @Test
    void fileTypeComesFromContentNotFromName() throws Exception {
        // A text file pretending to be a PNG is rejected...
        upload("evil.png", "image/png", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));

        // ...while a real JPEG with a misleading name is accepted and stored as .jpg.
        String url = json(upload("photo.txt", "text/plain", jpeg()).andExpect(status().isOk()).andReturn(), "$.avatarUrl");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    void oversizedFileIsRejected() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(png(), 0, big, 0, 8);
        upload("big.png", "image/png", big)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void replacingAvatarDeletesPreviousFile() throws Exception {
        String first = json(upload("a.png", "image/png", png()).andReturn(), "$.avatarUrl");
        String second = json(upload("b.png", "image/png", png()).andReturn(), "$.avatarUrl");

        assertThat(second).isNotEqualTo(first);
        assertThat(Files.exists(fileFor(first))).isFalse();
        assertThat(Files.exists(fileFor(second))).isTrue();
    }

    @Test
    void removingAvatarClearsUrlAndFile() throws Exception {
        String url = json(upload("a.png", "image/png", png()).andReturn(), "$.avatarUrl");

        mockMvc.perform(delete("/api/users/me/avatar").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
        assertThat(Files.exists(fileFor(url))).isFalse();
    }

    @Test
    void uploadRequiresLogin() throws Exception {
        mockMvc.perform(multipart("/api/users/me/avatar").file(new MockMultipartFile("file", "a.png", "image/png", png())))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions upload(String name, String contentType, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/users/me/avatar")
                .file(new MockMultipartFile("file", name, contentType, bytes))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private Path fileFor(String url) {
        return storage.root().resolve(url.substring(url.indexOf("/media/") + "/media/".length()));
    }

    private static byte[] png() throws Exception {
        return image("png");
    }

    private static byte[] jpeg() throws Exception {
        return image("jpg");
    }

    private static byte[] image(String format) throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }
}
