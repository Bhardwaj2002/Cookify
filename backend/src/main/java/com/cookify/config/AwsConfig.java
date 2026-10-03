package com.cookify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * AWS clients, created only when the matching feature is switched to AWS. Credentials use the
 * SDK's default chain, i.e. the EC2 instance role in production — no access keys in config.
 */
@Configuration
public class AwsConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public S3Client s3Client(@Value("${app.aws.region}") String region) {
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.notifications.channel", havingValue = "ses")
    public SesV2Client sesClient(@Value("${app.aws.region}") String region) {
        return SesV2Client.builder().region(Region.of(region)).build();
    }
}
