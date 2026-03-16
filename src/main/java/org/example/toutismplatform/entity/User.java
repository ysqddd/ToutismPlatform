package org.example.toutismplatform.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String role = "USER";
    
    @Column(nullable = false, columnDefinition = "tinyint(4) default 0")
    private int isAdmin = 0; // 0: 普通用户, 1: 超级管理员, 2: 景区管理员, 3: 订单管理员
}