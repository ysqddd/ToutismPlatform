package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scenic_edge")
@Data
public class ScenicEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "large_area_id", nullable = false)
    private Long largeAreaId;

    @Column(name = "start_spot_id", nullable = false)
    private Long startSpotId;

    @Column(name = "end_spot_id", nullable = false)
    private Long endSpotId;

    @Column(nullable = false)
    private Double distance = 0.0;

    @Column(name = "time_cost", nullable = false)
    private Integer timeCost = 0;

    @Column(name = "path_description", length = 500)
    private String pathDescription;

    @Column(name = "transport_mode", nullable = false, length = 50)
    private String transportMode = "WALK";

    @Column(name = "intensity_level", nullable = false)
    private Integer intensityLevel = 2;

    @Column(name = "scenic_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal scenicScore = BigDecimal.ZERO;

    @Column(name = "comfort_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal comfortScore = BigDecimal.ZERO;

    @Column(name = "elderly_friendly_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal elderlyFriendlyScore = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        initDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void initDefaults() {
        if (distance == null) {
            distance = 0.0;
        }
        if (timeCost == null) {
            timeCost = 0;
        }
        if (transportMode == null || transportMode.trim().isEmpty()) {
            transportMode = "WALK";
        }
        if (intensityLevel == null) {
            intensityLevel = 2;
        }
        if (scenicScore == null) {
            scenicScore = BigDecimal.ZERO;
        }
        if (comfortScore == null) {
            comfortScore = BigDecimal.ZERO;
        }
        if (elderlyFriendlyScore == null) {
            elderlyFriendlyScore = BigDecimal.ZERO;
        }
    }
}
