package com.cookify.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Streams media from the private S3 bucket at {@code /media/**}. Used when the app is not behind
 * CloudFront ({@code app.storage.s3.serve-via-app=true}); with CloudFront, {@code /media/*} goes
 * straight to the bucket and this controller is not created. Supports HTTP range requests so
 * recipe videos can be seeked.
 */
@RestController
@ConditionalOnProperty(name = "app.storage.s3.serve-via-app", havingValue = "true")
public class S3MediaController {

    private final S3Client s3;
    private final String bucket;
    private final String keyPrefix;

    public S3MediaController(S3Client s3,
                             @Value("${app.storage.s3.bucket}") String bucket,
                             @Value("${app.storage.s3.key-prefix:media/}") String keyPrefix) {
        this.s3 = s3;
        this.bucket = bucket;
        this.keyPrefix = keyPrefix;
    }

    @GetMapping("/media/{*path}")
    public ResponseEntity<InputStreamResource> get(@PathVariable String path,
                                                   @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        String relative = path.startsWith("/") ? path.substring(1) : path;
        if (relative.isEmpty() || relative.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        GetObjectRequest.Builder request = GetObjectRequest.builder().bucket(bucket).key(keyPrefix + relative);
        boolean partial = range != null && range.startsWith("bytes=");
        if (partial) {
            request.range(range);
        }
        ResponseInputStream<GetObjectResponse> object;
        try {
            object = s3.getObject(request.build());
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        } catch (S3Exception e) {
            if (e.statusCode() == 416) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
            throw e;
        }
        GetObjectResponse meta = object.response();
        ResponseEntity.BodyBuilder response = ResponseEntity
                .status(meta.contentRange() != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, meta.cacheControl() != null ? meta.cacheControl() : S3StorageService.CACHE_CONTROL)
                .contentType(meta.contentType() != null ? MediaType.parseMediaType(meta.contentType()) : MediaType.APPLICATION_OCTET_STREAM);
        if (meta.contentLength() != null) {
            response.contentLength(meta.contentLength());
        }
        if (meta.contentRange() != null) {
            response.header(HttpHeaders.CONTENT_RANGE, meta.contentRange());
        }
        if (meta.eTag() != null) {
            response.eTag(meta.eTag());
        }
        return response.body(new InputStreamResource(object));
    }
}
