package com.cookify.notification;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal template engine for emails: replaces {@code {{name}}} placeholders with HTML-escaped
 * values, then wraps the result in the shared {@code layout.html}.
 */
@Component
public class EmailRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");
    private static final String BASE = "templates/email/";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public RenderedEmail render(EmailTemplate template, Map<String, ?> data) {
        String body = fill(load(template.fileName()), data, true);
        String html = fill(load("layout"), Map.of("subject", template.subject(), "content", body), false);
        return new RenderedEmail(template.subject(), html, toPlainText(body));
    }

    private String fill(String source, Map<String, ?> data, boolean escape) {
        Matcher m = PLACEHOLDER.matcher(source);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            Object value = data.get(m.group(1));
            String text = value == null ? "" : value.toString();
            m.appendReplacement(out, Matcher.quoteReplacement(escape ? HtmlUtils.htmlEscape(text) : text));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String load(String name) {
        return cache.computeIfAbsent(name, n -> {
            try {
                return new ClassPathResource(BASE + n + ".html").getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Missing email template " + n, e);
            }
        });
    }

    private static String toPlainText(String html) {
        return HtmlUtils.htmlUnescape(html.replaceAll("(?s)<[^>]+>", " ").replaceAll("[ \\t]+", " ")
                .replaceAll("\\s*\\n\\s*", "\n").trim());
    }

    public record RenderedEmail(String subject, String html, String text) {
    }
}
