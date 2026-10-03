package com.cookify.user.service;

import com.cookify.common.exception.BadRequestException;
import com.cookify.media.ImageType;
import com.cookify.media.StorageService;
import com.cookify.media.StoredFile;
import com.cookify.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Profile picture upload: validates the image, stores it, and removes the previous one. */
@Service
@RequiredArgsConstructor
public class AvatarService {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private final UserService userService;
    private final StorageService storage;

    @Transactional
    public User upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("FILE_REQUIRED", "Please choose an image to upload");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("FILE_TOO_LARGE", "Profile pictures must be 5 MB or smaller");
        }
        byte[] bytes = read(file);
        ImageType type = ImageType.detect(bytes)
                .orElseThrow(() -> new BadRequestException("INVALID_IMAGE", "Only JPEG, PNG, GIF or WebP images are allowed"));

        User user = userService.getById(userId);
        String previousKey = user.getAvatarKey();

        StoredFile stored = storage.store("avatars/" + userId, bytes, type.contentType(), type.extension());
        user.setAvatarUrl(stored.url());
        user.setAvatarKey(stored.key());

        storage.delete(previousKey);
        return user;
    }

    @Transactional
    public User remove(Long userId) {
        User user = userService.getById(userId);
        storage.delete(user.getAvatarKey());
        user.setAvatarUrl(null);
        user.setAvatarKey(null);
        return user;
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
