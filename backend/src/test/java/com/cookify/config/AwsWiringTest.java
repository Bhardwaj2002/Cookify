package com.cookify.config;

import com.cookify.media.LocalStorageService;
import com.cookify.media.S3StorageService;
import com.cookify.media.StorageService;
import com.cookify.notification.ConsoleNotifier;
import com.cookify.notification.SesEmailNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 12: with the production switches (storage=s3, notifications=ses) the app starts with the AWS
 * implementations and without the local ones. No AWS call is made at startup, so no credentials needed.
 */
@SpringBootTest(properties = {
        "app.storage.type=s3",
        "app.storage.s3.bucket=cookify-media-test",
        "app.notifications.channel=ses",
        "app.notifications.ses.from=no-reply@cookify.dev",
        "app.aws.region=ap-south-1"
})
@ActiveProfiles("test")
class AwsWiringTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private StorageService storage;

    @Test
    void awsImplementationsReplaceLocalOnes() {
        assertThat(storage).isInstanceOf(S3StorageService.class);
        assertThat(context.getBeansOfType(SesEmailNotifier.class)).hasSize(1);
        assertThat(context.getBeansOfType(ConsoleNotifier.class)).isEmpty();
        assertThat(context.getBeansOfType(LocalStorageService.class)).isEmpty();
    }
}
