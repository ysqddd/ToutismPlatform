package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "permissions")
@Data
public class Permissions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "level", nullable = false, unique = true)
    private int level;

}
