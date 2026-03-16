package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.User;
import org.example.toutismplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 获取所有用户（仅管理员）
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
    
    // 根据ID获取用户（仅管理员）
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 修改用户信息（仅管理员）
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    if (user.getUsername() != null) {
                        existingUser.setUsername(user.getUsername());
                    }
                    if (user.getEmail() != null) {
                        existingUser.setEmail(user.getEmail());
                    }
                    if (user.getPassword() != null) {
                        existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                    }
                    if (user.getRole() != null) {
                        existingUser.setRole(user.getRole());
                    }
                    existingUser.setIsAdmin(user.getIsAdmin());
                    return ResponseEntity.ok(userRepository.save(existingUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 更新用户权限级别（仅超级管理员）
    @PutMapping("/{id}/admin-level")
    public ResponseEntity<User> updateAdminLevel(@PathVariable Long id, @RequestParam int adminLevel) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setIsAdmin(adminLevel);
                    return ResponseEntity.ok(userRepository.save(existingUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 删除用户（仅管理员）
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
