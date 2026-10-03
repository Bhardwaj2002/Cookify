package com.cookify.support;

import com.cookify.notification.EmailRenderer;
import com.cookify.notification.EmailTemplate;
import com.cookify.notification.Notifier;
import com.cookify.user.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test notifier: records every message (and still renders it) so tests can read OTP codes and links. */
@Component
@Primary
@Profile("test")
public class RecordingNotifier extends Notifier {

    public record Sent(String to, EmailTemplate template, Map<String, ?> data) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    public RecordingNotifier(EmailRenderer renderer) {
        super(renderer);
    }

    @Override
    public void send(User to, EmailTemplate template, Map<String, ?> data) {
        sent.add(new Sent(to.getEmail(), template, Map.copyOf(data)));
        super.send(to, template, data);
    }

    @Override
    protected void deliver(String toAddress, EmailRenderer.RenderedEmail email) {
        // rendered but not delivered
    }

    public List<Sent> sentTo(String email, EmailTemplate template) {
        return sent.stream().filter(s -> s.to().equals(email) && s.template() == template).toList();
    }

    public Sent last(String email, EmailTemplate template) {
        List<Sent> matches = sentTo(email, template);
        if (matches.isEmpty()) {
            throw new AssertionError("No " + template + " email sent to " + email);
        }
        return matches.getLast();
    }

    public void clear() {
        sent.clear();
    }
}
