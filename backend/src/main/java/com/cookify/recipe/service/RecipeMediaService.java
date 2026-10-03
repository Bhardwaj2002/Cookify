package com.cookify.recipe.service;

import com.cookify.common.exception.BadRequestException;
import com.cookify.media.ImageType;
import com.cookify.media.StorageService;
import com.cookify.media.StoredFile;
import com.cookify.media.VideoType;
import com.cookify.recipe.dto.MediaResponse;
import com.cookify.recipe.entity.MediaKind;
import com.cookify.recipe.entity.RecipeEntity;
import com.cookify.recipe.entity.RecipeMediaEntity;
import com.cookify.recipe.repository.RecipeMediaRepository;
import com.cookify.security.jwt.AuthUser;
import com.cookify.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/** Photos and videos attached to a recipe. */
@Service
@RequiredArgsConstructor
public class RecipeMediaService {

    static final int MAX_ITEMS = 10;
    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;

    private final RecipeService recipeService;
    private final RecipeMediaRepository mediaRepository;
    private final StorageService storage;

    @Transactional
    public MediaResponse add(Long recipeId, AuthUser caller, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("FILE_REQUIRED", "Please choose a photo or video");
        }
        return add(recipeId, caller, read(file));
    }

    /** Same checks as an upload; used for files that don't arrive as multipart (e.g. demo photos). */
    @Transactional
    public MediaResponse add(Long recipeId, AuthUser caller, byte[] bytes) {
        RecipeEntity recipe = recipeService.getEditable(recipeId, caller);
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("FILE_REQUIRED", "Please choose a photo or video");
        }
        if (recipe.getMedia().size() >= MAX_ITEMS) {
            throw new BadRequestException("TOO_MANY_MEDIA", "A recipe can have at most " + MAX_ITEMS + " photos/videos");
        }

        Optional<ImageType> image = ImageType.detect(bytes);
        Optional<VideoType> video = image.isPresent() ? Optional.empty() : VideoType.detect(bytes);
        if (image.isEmpty() && video.isEmpty()) {
            throw new BadRequestException("INVALID_MEDIA", "Only JPEG, PNG, GIF, WebP images or MP4, MOV, WebM videos are allowed");
        }
        MediaKind kind = image.isPresent() ? MediaKind.IMAGE : MediaKind.VIDEO;
        long limit = kind == MediaKind.IMAGE ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES;
        if (bytes.length > limit) {
            throw new BadRequestException("FILE_TOO_LARGE",
                    kind == MediaKind.IMAGE ? "Images must be 10 MB or smaller" : "Videos must be 50 MB or smaller");
        }

        String contentType = image.map(ImageType::contentType).orElseGet(() -> video.get().contentType());
        String extension = image.map(ImageType::extension).orElseGet(() -> video.get().extension());
        StoredFile stored = storage.store("recipes/" + recipeId, bytes, contentType, extension);

        int position = recipe.getMedia().stream().mapToInt(RecipeMediaEntity::getPosition).max().orElse(-1) + 1;
        RecipeMediaEntity media = new RecipeMediaEntity(recipe, kind, stored.key(), stored.url(), position);
        recipe.getMedia().add(media);
        return MediaResponse.from(mediaRepository.saveAndFlush(media));
    }

    @Transactional
    public void remove(Long recipeId, Long mediaId, AuthUser caller) {
        RecipeEntity recipe = recipeService.getEditable(recipeId, caller);
        RecipeMediaEntity media = mediaRepository.findByIdAndRecipeId(mediaId, recipeId)
                .orElseThrow(() -> new NotFoundException("MEDIA_NOT_FOUND", "Media not found"));
        recipe.getMedia().remove(media);
        String key = media.getStorageKey();
        RecipeService.afterCommit(() -> storage.delete(key));
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
