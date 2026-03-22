package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
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
    private Integer visitingDuration = 60; // 默认 60 分钟
    
    @Column(length = 500)
    private String tags;
    
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
