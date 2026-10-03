package com.cookify.media;

/**
 * Where uploaded media lives. {@link LocalStorageService} writes to disk in development;
 * an S3 implementation replaces it in production (Step 12) without touching callers.
 */
public interface StorageService {

    /**
     * Saves bytes under {@code folder} with a random file name.
     *
     * @param folder      logical folder, e.g. {@code avatars/42}
     * @param bytes       file content
     * @param contentType MIME type, e.g. {@code image/jpeg}
     * @param extension   file extension without dot, e.g. {@code jpg}
     */
    StoredFile store(String folder, byte[] bytes, String contentType, String extension);

    /** Deletes a previously stored file. Missing files are ignored. */
    void delete(String key);
}
