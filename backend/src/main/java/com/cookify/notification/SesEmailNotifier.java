package com.cookify.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Production notifier: sends the rendered HTML (with a plain-text alternative) through Amazon SES.
 * Delivery failures are logged, never thrown, so a mail problem can't break a login or a comment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.channel", havingValue = "ses")
public class SesEmailNotifier extends Notifier {

    private final SesV2Client ses;
    private final String fromAddress;

    public SesEmailNotifier(EmailRenderer renderer, SesV2Client ses,
                            @Value("${app.notifications.ses.from}") String fromAddress) {
        super(renderer);
        this.ses = ses;
        this.fromAddress = fromAddress;
    }

    @Override
    protected void deliver(String toAddress, EmailRenderer.RenderedEmail email) {
        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(toAddress).build())
                .content(EmailContent.builder().simple(Message.builder()
                        .subject(utf8(email.subject()))
                        .body(Body.builder().html(utf8(email.html())).text(utf8(email.text())).build())
                        .build()).build())
                .build();
        try {
            ses.sendEmail(request);
        } catch (SesV2Exception e) {
            log.error("SES could not send '{}' to {}: {}", email.subject(), toAddress, e.getMessage());
        }
    }

    private static Content utf8(String value) {
        return Content.builder().data(value).charset("UTF-8").build();
    }
}
