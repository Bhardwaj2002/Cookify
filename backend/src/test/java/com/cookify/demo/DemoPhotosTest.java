package com.cookify.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every demo recipe gets its bundled photo as cover, also when the data was seeded without photos. */
@SpringBootTest(properties = {"app.demo-data.enabled=true", "app.demo-data.photos=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoPhotosTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DemoDataLoader loader;

    @Test
    void everyDemoRecipeHasExactlyOnePhotoAfterRepeatedLoads() throws Exception {
        jdbc.execute("TRUNCATE users, ingredients, equipment RESTART IDENTITY CASCADE");
        loader.run(null);
        jdbc.execute("DELETE FROM recipe_media WHERE recipe_id IN (SELECT id FROM recipes ORDER BY id LIMIT 5)");

        loader.run(null); // "already present" path re-adds only the missing photos
        loader.run(null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipes", Long.class)).isEqualTo(39);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipe_media WHERE media_type = 'IMAGE'", Long.class)).isEqualTo(39);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM recipes r WHERE (SELECT count(*) FROM recipe_media m WHERE m.recipe_id = r.id) <> 1",
                Long.class)).isZero();

        mockMvc.perform(get("/api/recipes").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].coverUrl", everyItem(startsWith("http://localhost:8080/media/recipes/"))));
    }

    @Test
    void slugMatchesPhotoFileNames() {
        assertThat(DemoDataLoader.slug("Thai Green Curry with Tofu")).isEqualTo("thai-green-curry-with-tofu");
        assertThat(DemoDataLoader.slug("Spaghetti Aglio e Olio")).isEqualTo("spaghetti-aglio-e-olio");
    }
}
