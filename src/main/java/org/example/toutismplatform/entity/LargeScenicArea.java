package org.example.toutismplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
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
    @JsonIgnoreProperties({"largeScenicAreas"}) // 关键修复：忽略反向引用，防止无限循环
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
    
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price;
    
    @Column(length = 500)
    private String tags;
    
    @Column(name = "is_public_facility", nullable = false, columnDefinition = "tinyint default 0")
    @JsonProperty("isPublicFacility")
    private boolean isPublicFacility;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "largeAreaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SmallScenicSpot> smallScenicSpots;
    
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
