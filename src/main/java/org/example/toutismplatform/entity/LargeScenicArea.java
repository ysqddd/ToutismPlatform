package org.example.toutismplatform.entity;

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
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
    
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
