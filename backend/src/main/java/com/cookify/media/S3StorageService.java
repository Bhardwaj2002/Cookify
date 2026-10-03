package com.cookify.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

/**
 * Production storage: files go to a private S3 bucket and are served through CloudFront.
 * CloudFront forwards {@code /media/*} to the bucket unchanged, so objects are stored under the
 * {@code media/} prefix and the public URL is {@code <APP_URL>/media/<folder>/<uuid>.<ext>}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    /** File names are random and never reused, so browsers and CloudFront may cache them for a year. */
    static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client s3;
    private final String bucket;
    private final String keyPrefix;
    private final String publicBaseUrl;

    public S3StorageService(S3Client s3,
                            @Value("${app.storage.s3.bucket}") String bucket,
                            @Value("${app.storage.s3.key-prefix:media/}") String keyPrefix,
                            StorageProperties props) {
        this.s3 = s3;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
        this.publicBaseUrl = props.publicBaseUrl().replaceAll("/+$", "");
    }

    @Override
    public StoredFile store(String folder, byte[] bytes, String contentType, String extension) {
        String relative = folder + "/" + UUID.randomUUID() + "." + extension;
        String key = keyPrefix + relative;
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl(CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(bytes));
        return new StoredFile(key, publicBaseUrl + "/" + relative);
    }

    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("Could not delete s3://{}/{}", bucket, key, e);
        }
    }
}
