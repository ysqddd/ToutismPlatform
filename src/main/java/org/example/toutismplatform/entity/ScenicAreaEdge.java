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

    @Column(length = 500)
    private String description;

    @Column(name = "cost_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

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
    }
}
