package com.cookify.notification;

import com.cookify.user.entity.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends templated messages to users. Template-method pattern: rendering is shared, and each
 * subclass only decides how to deliver ({@link ConsoleNotifier} in dev, SES email in production).
 */
public abstract class Notifier {

    private final EmailRenderer renderer;

    protected Notifier(EmailRenderer renderer) {
        this.renderer = renderer;
    }

    public void send(User to, EmailTemplate template, Map<String, ?> data) {
        Map<String, Object> model = new HashMap<>(data);
        model.putIfAbsent("username", to.getUsername());
        deliver(to.getEmail(), renderer.render(template, model));
    }

    protected abstract void deliver(String toAddress, EmailRenderer.RenderedEmail email);
}
