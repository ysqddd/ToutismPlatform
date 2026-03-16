package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "non_login_employee")
@Data
public class NonLoginEmployee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "real_name", nullable = false)
    private String realName;
    
    private String email;
    
    private String phone;
    
    private String department;
    
    private String position;
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE: 在职，INACTIVE: 离职
    
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