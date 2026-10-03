package com.cookify.media;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

    @Test
    void detectsFormatsByMagicBytes() {
        assertThat(ImageType.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0))).contains(ImageType.JPEG);
        assertThat(ImageType.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0))).contains(ImageType.PNG);
        assertThat(ImageType.detect("GIF89a...".getBytes(StandardCharsets.US_ASCII))).contains(ImageType.GIF);
        assertThat(ImageType.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII))).contains(ImageType.WEBP);
    }

    @Test
    void rejectsNonImages() {
        assertThat(ImageType.detect("<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(ImageType.detect("%PDF-1.7".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(ImageType.detect(new byte[0])).isEmpty();
    }

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }
}
