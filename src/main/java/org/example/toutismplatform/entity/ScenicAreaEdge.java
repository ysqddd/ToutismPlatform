package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
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
    private java.math.BigDecimal distance;
    
    @Column(nullable = false)
    private Integer duration;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
