package com.cookify.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** A photo or video attached to a recipe. The first image is used as the cover. */
@Entity
@Table(name = "recipe_media")
@Getter
@NoArgsConstructor
public class RecipeMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private RecipeEntity recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaKind kind;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RecipeMediaEntity(RecipeEntity recipe, MediaKind kind, String storageKey, String url, int position) {
        this.recipe = recipe;
        this.kind = kind;
        this.storageKey = storageKey;
        this.url = url;
        this.position = position;
    }
}
