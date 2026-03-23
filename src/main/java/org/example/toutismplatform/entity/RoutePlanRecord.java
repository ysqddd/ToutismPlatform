package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_plan_record")
@Data
public class RoutePlanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "start_area_id")
    private Long startAreaId;

    @Column(name = "end_area_id")
    private Long endAreaId;

    @Column(name = "original_query", columnDefinition = "LONGTEXT", nullable = false)
    private String originalQuery;

    @Column(name = "extracted_preferences_json", columnDefinition = "JSON")
    private String extractedPreferencesJson;

    @Column(name = "candidate_area_ids_json", columnDefinition = "JSON")
    private String candidateAreaIdsJson;

    @Column(name = "route_result_json", columnDefinition = "JSON")
    private String routeResultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
