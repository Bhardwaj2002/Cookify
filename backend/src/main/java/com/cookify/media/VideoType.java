package com.cookify.media;

import java.util.Optional;

/** Video formats accepted for recipe uploads, detected from magic bytes like {@link ImageType}. */
public enum VideoType {

    MP4("video/mp4", "mp4"),
    MOV("video/quicktime", "mov"),
    WEBM("video/webm", "webm");

    private final String contentType;
    private final String extension;

    VideoType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<VideoType> detect(byte[] b) {
        // ISO base media (MP4 / MOV): bytes 4-7 are "ftyp", followed by the brand.
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            boolean quickTime = b[8] == 'q' && b[9] == 't';
            return Optional.of(quickTime ? MOV : MP4);
        }
        // Matroska / WebM EBML header.
        if (b.length >= 4 && (b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) {
            return Optional.of(WEBM);
        }
        return Optional.empty();
    }
}
