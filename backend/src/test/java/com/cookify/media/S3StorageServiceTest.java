package com.cookify.media;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Step 12: S3 storage writes under media/ and returns CloudFront URLs (S3 client mocked). */
class S3StorageServiceTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3StorageService storage = new S3StorageService(s3, "cookify-media", "media/",
            new StorageProperties(null, "https://d123.cloudfront.net/media/"));

    @Test
    void storesUnderMediaPrefixWithTypeAndCaching() {
        StoredFile file = storage.store("recipes/7", new byte[]{1, 2, 3}, "image/png", "png");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("cookify-media");
        assertThat(request.getValue().key()).matches("media/recipes/7/[0-9a-f-]{36}\\.png");
        assertThat(request.getValue().contentType()).isEqualTo("image/png");
        assertThat(request.getValue().cacheControl()).contains("immutable");

        assertThat(file.key()).isEqualTo(request.getValue().key());
        assertThat(file.url()).isEqualTo("https://d123.cloudfront.net/" + file.key());
    }

    @Test
    void deletesByKeyAndIgnoresFailures() {
        storage.delete("media/avatars/1/x.jpg");
        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo("media/avatars/1/x.jpg");

        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder().message("boom").build());
        assertThatCode(() -> storage.delete("media/x.jpg")).doesNotThrowAnyException();
        assertThatCode(() -> storage.delete(null)).doesNotThrowAnyException();
    }
}
