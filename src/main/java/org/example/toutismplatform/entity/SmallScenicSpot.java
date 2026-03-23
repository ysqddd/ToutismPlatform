package org.example.toutismplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "small_scenic_spot")
@Data
public class SmallScenicSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "large_area_id", nullable = false)
    private Long largeAreaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "large_area_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"smallScenicSpots", "products"})
    private LargeScenicArea largeScenicArea;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_spot_type", nullable = false, columnDefinition = "int default 0")
    private Integer isSpotType = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "visiting_duration")
    private Integer visitingDuration = 60;

    @Column(length = 500)
    private String tags;

    @Column(name = "intensity_level", nullable = false)
    private Integer intensityLevel = 2;

    @Column(name = "queue_level", nullable = false)
    private Integer queueLevel = 1;

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

    @Column(name = "rest_convenience_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal restConvenienceScore = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isSpotType == null) {
            isSpotType = 0;
        }
        if (visitingDuration == null) {
            visitingDuration = 60;
        }
        if (intensityLevel == null) {
            intensityLevel = 2;
        }
        if (queueLevel == null) {
            queueLevel = 1;
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
        if (restConvenienceScore == null) {
            restConvenienceScore = BigDecimal.ZERO;
        }
    }
}
