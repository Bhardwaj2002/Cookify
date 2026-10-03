package com.cookify.notification;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailRendererTest {

    private final EmailRenderer renderer = new EmailRenderer();

    @Test
    void fillsPlaceholdersInsideBrandedLayout() {
        EmailRenderer.RenderedEmail email = renderer.render(EmailTemplate.LOGIN_OTP,
                Map.of("username", "chef_ayush", "code", "123456", "minutes", 5));

        assertThat(email.subject()).isEqualTo("Your Cookify login code");
        assertThat(email.html()).contains("COOKify", "Hi chef_ayush", "123456", "5 minutes");
        assertThat(email.html()).doesNotContain("{{");
        assertThat(email.text()).contains("123456").doesNotContain("<p>");
    }

    @Test
    void escapesUserControlledValues() {
        EmailRenderer.RenderedEmail email = renderer.render(EmailTemplate.LOGIN_OTP,
                Map.of("username", "<script>alert(1)</script>", "code", "123456", "minutes", 5));

        assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
