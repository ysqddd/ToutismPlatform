package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scenic_area_edge")
@Data
public class ScenicAreaEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_area_id", nullable = false)
    private Long fromAreaId;

    @Column(name = "to_area_id", nullable = false)
    private Long toAreaId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer duration = 0;

    @Column(name = "cost_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

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

    @Column(length = 500)
    private String description;

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
        initDefaults();
    }

    private void initDefaults() {
        if (distance == null) {
            distance = BigDecimal.ZERO;
        }
        if (duration == null) {
            duration = 0;
        }
        if (costAmount == null) {
            costAmount = BigDecimal.ZERO;
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
