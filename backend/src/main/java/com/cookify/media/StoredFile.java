package com.cookify.media;

/**
 * A file saved by a {@link StorageService}.
 *
 * @param key storage key used to delete it later (e.g. {@code avatars/7/3f2c….jpg})
 * @param url public URL the browser loads it from
 */
public record StoredFile(String key, String url) {
}
