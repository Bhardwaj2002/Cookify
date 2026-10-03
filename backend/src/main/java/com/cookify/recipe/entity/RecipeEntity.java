package com.cookify.recipe.entity;

import com.cookify.recipe.domain.Course;
import com.cookify.recipe.domain.Cuisine;
import com.cookify.recipe.domain.DietType;
import com.cookify.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Database row of a recipe. Business rules live in the domain {@code Recipe} hierarchy. */
@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false, length = 20)
    private DietType dietType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Cuisine cuisine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Course course;

    @Column(name = "prep_minutes", nullable = false)
    private int prepMinutes;

    @Column(name = "cook_minutes", nullable = false)
    private int cookMinutes;

    /** prep + cook, computed by the database (generated column) for filtering and sorting. */
    @Column(name = "total_minutes", insertable = false, updatable = false)
    private Integer totalMinutes;

    @Column(nullable = false)
    private int servings;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false)
    private int calories;

    private Integer protein;

    @Column(nullable = false)
    private short difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> steps = new ArrayList<>();

    @Column(nullable = false)
    private long views;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    /** Sum of all scores; avg_rating = rating_sum / rating_count (maintained atomically in SQL). */
    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<RecipeIngredientEntity> ingredients = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "recipe_equipment",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<EquipmentEntity> equipment = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<RecipeMediaEntity> media = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public int getTotalMinutes() {
        return prepMinutes + cookMinutes;
    }
}
