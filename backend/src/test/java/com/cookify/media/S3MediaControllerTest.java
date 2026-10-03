package com.cookify.media;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Media streamed from the private bucket when there is no CloudFront in front (S3 client mocked). */
class S3MediaControllerTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3MediaController controller = new S3MediaController(s3, "cookify-media", "media/");

    private static ResponseInputStream<GetObjectResponse> object(GetObjectResponse meta, byte[] body) {
        return new ResponseInputStream<>(meta, AbortableInputStream.create(new ByteArrayInputStream(body)));
    }

    @Test
    void streamsObjectWithTypeLengthAndCaching() throws Exception {
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(object(GetObjectResponse.builder()
                .contentType("image/png").contentLength(3L).cacheControl("public, max-age=31536000, immutable").build(),
                new byte[]{1, 2, 3}));

        ResponseEntity<InputStreamResource> response = controller.get("/recipes/7/a.png", null);

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("cookify-media");
        assertThat(request.getValue().key()).isEqualTo("media/recipes/7/a.png");
        assertThat(request.getValue().range()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("image/png");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(3);
        assertThat(response.getHeaders().getCacheControl()).contains("immutable");
        assertThat(response.getBody().getInputStream().readAllBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void forwardsRangeAndAnswersPartialContent() {
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(object(GetObjectResponse.builder()
                .contentType("video/mp4").contentLength(2L).contentRange("bytes 0-1/10").build(), new byte[]{9, 9}));

        ResponseEntity<InputStreamResource> response = controller.get("/recipes/7/v.mp4", "bytes=0-1");

        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(request.capture());
        assertThat(request.getValue().range()).isEqualTo("bytes=0-1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-1/10");
    }

    @Test
    void missingObjectIs404() {
        when(s3.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThat(controller.get("/recipes/7/gone.png", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsEmptyAndDotDotPaths() {
        assertThat(controller.get("/", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.get("/../secret", null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(s3);
    }
}
