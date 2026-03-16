package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permission", uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
})
@Data
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_role"))
    private Role role;

    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
    private Permission permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}