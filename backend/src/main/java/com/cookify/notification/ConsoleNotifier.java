package com.cookify.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development notifier: prints emails to the application log instead of sending them. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.channel", havingValue = "console", matchIfMissing = true)
public class ConsoleNotifier extends Notifier {

    public ConsoleNotifier(EmailRenderer renderer) {
        super(renderer);
    }

    @Override
    protected void deliver(String toAddress, EmailRenderer.RenderedEmail email) {
        log.info("""

                ======================= EMAIL (console) =======================
                To:      {}
                Subject: {}
                ---------------------------------------------------------------
                {}
                ===============================================================""",
                toAddress, email.subject(), email.text());
    }
}
