package com.cookify.notification;

import com.cookify.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Step 12: SES notifier sends rendered HTML + text from the verified address (SES client mocked). */
class SesEmailNotifierTest {

    private final SesV2Client ses = mock(SesV2Client.class);
    private final SesEmailNotifier notifier = new SesEmailNotifier(new EmailRenderer(), ses, "Cookify <no-reply@cookify.dev>");

    @Test
    void sendsHtmlAndTextEmail() {
        notifier.send(user(), EmailTemplate.LOGIN_OTP, Map.of("code", "482913", "minutes", 5));

        ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses).sendEmail(request.capture());
        SendEmailRequest sent = request.getValue();
        assertThat(sent.fromEmailAddress()).isEqualTo("Cookify <no-reply@cookify.dev>");
        assertThat(sent.destination().toAddresses()).containsExactly("chef@example.com");
        assertThat(sent.content().simple().subject().data()).isEqualTo("Your Cookify login code");
        assertThat(sent.content().simple().body().html().data()).contains("482913", "COOKify");
        assertThat(sent.content().simple().body().text().data()).contains("482913").doesNotContain("<p>");
    }

    @Test
    void deliveryFailureIsLoggedNotThrown() {
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SesV2Exception.builder().message("Email address is not verified").build());
        assertThatCode(() -> notifier.send(user(), EmailTemplate.LOGIN_OTP, Map.of("code", "1", "minutes", 5)))
                .doesNotThrowAnyException();
    }

    private static User user() {
        User u = new User();
        u.setUsername("chef");
        u.setEmail("chef@example.com");
        return u;
    }
}
