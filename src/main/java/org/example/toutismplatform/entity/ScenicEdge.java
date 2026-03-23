package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "scenic_edge")
@Data
public class ScenicEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_area_id", nullable = false)
    private Long startAreaId;

    @Column(name = "end_area_id", nullable = false)
    private Long endAreaId;

    @Column(nullable = false)
    private Double distance = 0.0;

    @Column(name = "time_cost", nullable = false)
    private Integer timeCost = 0;

    @Column(name = "path_description", length = 500)
    private String pathDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (distance == null) {
            distance = 0.0;
        }
        if (timeCost == null) {
            timeCost = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
