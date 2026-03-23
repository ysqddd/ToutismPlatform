package org.example.toutismplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "large_scenic_area")
@Data
public class LargeScenicArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(mappedBy = "largeScenicAreas", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"largeScenicAreas"})
    private List<Product> products;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "opening_hours", length = 100)
    private String openingHours;

    @Column(name = "recommended_visit_duration", nullable = false)
    private Integer recommendedVisitDuration = 120;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 500)
    private String tags;

    @Column(name = "is_area_type", nullable = false, columnDefinition = "int default 0")
    @JsonProperty("isAreaType")
    private Integer isAreaType = 0;

    @Column(name = "intensity_level", nullable = false)
    private Integer intensityLevel = 2;

    @Column(name = "crowd_level", nullable = false)
    private Integer crowdLevel = 2;

    @Column(name = "family_friendly_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal familyFriendlyScore = BigDecimal.ZERO;

    @Column(name = "elderly_friendly_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal elderlyFriendlyScore = BigDecimal.ZERO;

    @Column(name = "nature_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal natureScore = BigDecimal.ZERO;

    @Column(name = "culture_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal cultureScore = BigDecimal.ZERO;

    @Column(name = "photography_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal photographyScore = BigDecimal.ZERO;

    @Column(name = "leisure_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal leisureScore = BigDecimal.ZERO;

    @Column(name = "food_convenience_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal foodConvenienceScore = BigDecimal.ZERO;

    @Column(name = "restroom_convenience_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal restroomConvenienceScore = BigDecimal.ZERO;

    @Column(name = "popularity_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal popularityScore = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "largeScenicArea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"largeScenicArea"})
    private List<SmallScenicSpot> smallScenicSpots;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (recommendedVisitDuration == null) {
            recommendedVisitDuration = 120;
        }
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        if (isAreaType == null) {
            isAreaType = 0;
        }
        if (intensityLevel == null) {
            intensityLevel = 2;
        }
        if (crowdLevel == null) {
            crowdLevel = 2;
        }
        initScoresIfNull();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        initScoresIfNull();
    }

    private void initScoresIfNull() {
        if (familyFriendlyScore == null) {
            familyFriendlyScore = BigDecimal.ZERO;
        }
        if (elderlyFriendlyScore == null) {
            elderlyFriendlyScore = BigDecimal.ZERO;
        }
        if (natureScore == null) {
            natureScore = BigDecimal.ZERO;
        }
        if (cultureScore == null) {
            cultureScore = BigDecimal.ZERO;
        }
        if (photographyScore == null) {
            photographyScore = BigDecimal.ZERO;
        }
        if (leisureScore == null) {
            leisureScore = BigDecimal.ZERO;
        }
        if (foodConvenienceScore == null) {
            foodConvenienceScore = BigDecimal.ZERO;
        }
        if (restroomConvenienceScore == null) {
            restroomConvenienceScore = BigDecimal.ZERO;
        }
        if (popularityScore == null) {
            popularityScore = BigDecimal.ZERO;
        }
    }
}
