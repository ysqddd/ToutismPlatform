package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.User;
import org.example.toutismplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取所有用户（保留原接口，给后台管理使用）
     */
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userRepository.findAll()
                .stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 根据 ID 获取用户（保留原接口，自动过滤敏感字段）
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(this::toProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 当前登录用户查看自己的资料
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        Optional<User> currentUser = findCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("未登录或无法识别当前用户"));
        }
        return ResponseEntity.ok(toProfileResponse(currentUser.get()));
    }

    /**
     * 当前登录用户修改自己的资料
     * 这里只允许修改用户名、邮箱、密码；不再处理绑定手机、实名认证等内容。
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody UserProfileUpdateRequest request,
                                             Authentication authentication) {
        Optional<User> currentUser = findCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiMessage("未登录或无法识别当前用户"));
        }

        User user = currentUser.get();

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toProfileResponse(saved));
    }

    /**
     * 按 ID 修改用户（保留原接口，兼容旧后台管理逻辑）
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponse> updateUser(@PathVariable Long id,
                                                          @RequestBody UserProfileUpdateRequest request) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                        existingUser.setUsername(request.getUsername().trim());
                    }
                    if (request.getEmail() != null) {
                        existingUser.setEmail(request.getEmail().trim());
                    }
                    if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                    }
                    return ResponseEntity.ok(toProfileResponse(userRepository.save(existingUser)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private Optional<User> findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principalObject = authentication.getPrincipal();
        if (principalObject != null && !Objects.equals(principalObject, "anonymousUser")) {
            try {
                Object idValue = principalObject.getClass().getMethod("getId").invoke(principalObject);
                if (idValue instanceof Number) {
                    return userRepository.findById(((Number) idValue).longValue());
                }
            } catch (Exception ignored) {
            }
        }

        String principal = authentication.getName();
        if (principal == null || principal.trim().isEmpty() || Objects.equals(principal, "anonymousUser")) {
            return Optional.empty();
        }

        try {
            Long id = Long.parseLong(principal);
            Optional<User> byId = userRepository.findById(id);
            if (byId.isPresent()) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }

        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> principal.equals(user.getUsername()))
                .findFirst();
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        try {
            Object role = user.getClass().getMethod("getRole").invoke(user);
            response.setRole(role == null ? null : String.valueOf(role));
        } catch (Exception ignored) {
            response.setRole(null);
        }
        return response;
    }

    public static class UserProfileUpdateRequest {
        private String username;
        private String email;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String role;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class ApiMessage {
        private String message;

        public ApiMessage() {
        }

        public ApiMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
