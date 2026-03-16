package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Permission;
import org.example.toutismplatform.entity.Role;
import org.example.toutismplatform.entity.RolePermission;
import org.example.toutismplatform.repository.PermissionRepository;
import org.example.toutismplatform.repository.RolePermissionRepository;
import org.example.toutismplatform.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    // 获取所有权限
    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionRepository.findAll());
    }

    // 添加权限
    @PostMapping
    public ResponseEntity<Permission> addPermission(@RequestBody PermissionRequest request) {
        Permission permission = new Permission();
        permission.setCode(request.getCode());
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setModule(request.getModule());

        return ResponseEntity.ok(permissionRepository.save(permission));
    }

    // 更新权限
    @PutMapping("/{id}")
    public ResponseEntity<Permission> updatePermission(@PathVariable Long id, @RequestBody PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        permission.setCode(request.getCode());
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setModule(request.getModule());

        return ResponseEntity.ok(permissionRepository.save(permission));
    }

    // 删除权限
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // 获取所有角色
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    // 添加角色
    @PostMapping("/roles")
    public ResponseEntity<Role> addRole(@RequestBody RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return ResponseEntity.ok(roleRepository.save(role));
    }

    // 更新角色
    @PutMapping("/roles/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return ResponseEntity.ok(roleRepository.save(role));
    }

    // 删除角色
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 为角色分配权限
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Void> assignPermissionsToRole(@PathVariable Long roleId, @RequestBody RolePermissionRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // 先删除角色原有的权限
        rolePermissionRepository.deleteByRoleId(roleId);

        // 为角色分配新的权限
        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new RuntimeException("Permission not found"));

            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);

            rolePermissionRepository.save(rolePermission);
        }

        return ResponseEntity.noContent().build();
    }

    // 获取角色的权限
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        List<Permission> permissions = rolePermissions.stream()
                .map(RolePermission::getPermission)
                .toList();

        return ResponseEntity.ok(permissions);
    }

    // 请求体类
    public static class PermissionRequest {
        private String code;
        private String name;
        private String description;
        private String module;

        // Getters and Setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getModule() {
            return module;
        }

        public void setModule(String module) {
            this.module = module;
        }
    }

    public static class RoleRequest {
        private String name;
        private String description;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class RolePermissionRequest {
        private List<Long> permissionIds;

        // Getters and Setters
        public List<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}