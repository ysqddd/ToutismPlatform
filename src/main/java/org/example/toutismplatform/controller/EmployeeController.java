package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Employee;
import org.example.toutismplatform.entity.NonLoginEmployee;
import org.example.toutismplatform.entity.Role;
import org.example.toutismplatform.repository.EmployeeRepository;
import org.example.toutismplatform.repository.NonLoginEmployeeRepository;
import org.example.toutismplatform.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private NonLoginEmployeeRepository nonLoginEmployeeRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 获取所有员工
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        List<Map<String, Object>> allEmployees = new ArrayList<>();
        
        // 添加需要登录的员工
        for (Employee emp : employeeRepository.findAll()) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", emp.getUsername());
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("role", emp.getRole().getName());
            empMap.put("roleId", emp.getRole().getId());
            allEmployees.add(empMap);
        }
        
        // 添加不需要登录的员工
        for (NonLoginEmployee emp : nonLoginEmployeeRepository.findAll()) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", null); // 不需要登录的员工没有用户名
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("permissionLevel", 0); // 不需要登录的员工默认权限级别为0
            allEmployees.add(empMap);
        }
        
        return ResponseEntity.ok(allEmployees);
    }
    
    // 根据 ID 获取员工
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 创建需要登录的员工
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Map<String, Object> request) {
        boolean needsLogin = (Boolean) request.get("needsLogin");
        
        if (needsLogin) {
            // 检查用户名是否已存在
            if (employeeRepository.existsByUsername((String) request.get("username"))) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // 检查邮箱是否已存在
            if (employeeRepository.existsByEmail((String) request.get("email"))) {
                return ResponseEntity.badRequest().body(null);
            }
            
            Employee employee = new Employee();
            employee.setUsername((String) request.get("username"));
            employee.setPassword(passwordEncoder.encode((String) request.get("password")));
            Long roleId = Long.valueOf(request.get("roleId").toString());
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
            employee.setRole(role);
            employee.setRealName((String) request.get("realName"));
            employee.setEmail((String) request.get("email"));
            employee.setPhone((String) request.get("phone"));
            employee.setDepartment((String) request.get("department"));
            employee.setPosition((String) request.get("position"));
            employee.setStatus((String) request.get("status"));
            
            Employee savedEmployee = employeeRepository.save(employee);
            return ResponseEntity.ok(savedEmployee);
        } else {
            NonLoginEmployee employee = new NonLoginEmployee();
            employee.setRealName((String) request.get("realName"));
            employee.setEmail((String) request.get("email"));
            employee.setPhone((String) request.get("phone"));
            employee.setDepartment((String) request.get("department"));
            employee.setPosition((String) request.get("position"));
            employee.setStatus((String) request.get("status"));
            
            NonLoginEmployee savedEmployee = nonLoginEmployeeRepository.save(employee);
            return ResponseEntity.ok().build();
        }
    }
    
    // 创建不需要登录的员工
    @PostMapping("/non-login-employees")
    public ResponseEntity<NonLoginEmployee> createNonLoginEmployee(@RequestBody Map<String, Object> request) {
        NonLoginEmployee employee = new NonLoginEmployee();
        employee.setRealName((String) request.get("realName"));
        employee.setEmail((String) request.get("email"));
        employee.setPhone((String) request.get("phone"));
        employee.setDepartment((String) request.get("department"));
        employee.setPosition((String) request.get("position"));
        employee.setStatus((String) request.get("status"));
        
        NonLoginEmployee savedEmployee = nonLoginEmployeeRepository.save(employee);
        return ResponseEntity.ok(savedEmployee);
    }
    
    // 更新需要登录的员工
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return employeeRepository.findById(id)
                .map(existingEmployee -> {
                    if (request.containsKey("realName")) {
                        existingEmployee.setRealName((String) request.get("realName"));
                    }
                    if (request.containsKey("email")) {
                        String newEmail = (String) request.get("email");
                        // 检查新邮箱是否已被其他员工使用
                        if (!existingEmployee.getEmail().equals(newEmail) &&
                                employeeRepository.existsByEmail(newEmail)) {
                            return ResponseEntity.badRequest().<Employee>build();
                        }
                        existingEmployee.setEmail(newEmail);
                    }
                    if (request.containsKey("phone")) {
                        existingEmployee.setPhone((String) request.get("phone"));
                    }
                    if (request.containsKey("department")) {
                        existingEmployee.setDepartment((String) request.get("department"));
                    }
                    if (request.containsKey("position")) {
                        existingEmployee.setPosition((String) request.get("position"));
                    }
                    if (request.containsKey("status")) {
                        existingEmployee.setStatus((String) request.get("status"));
                    }
                    if (request.containsKey("roleId")) {
                        Long roleId = Long.valueOf(request.get("roleId").toString());
                        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
                        existingEmployee.setRole(role);
                    }

                    return ResponseEntity.ok(employeeRepository.save(existingEmployee));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 更新不需要登录的员工
    @PutMapping("/non-login-employees/{id}")
    public ResponseEntity<NonLoginEmployee> updateNonLoginEmployee(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return nonLoginEmployeeRepository.findById(id)
                .map(existingEmployee -> {
                    if (request.containsKey("realName")) {
                        existingEmployee.setRealName((String) request.get("realName"));
                    }
                    if (request.containsKey("email")) {
                        existingEmployee.setEmail((String) request.get("email"));
                    }
                    if (request.containsKey("phone")) {
                        existingEmployee.setPhone((String) request.get("phone"));
                    }
                    if (request.containsKey("department")) {
                        existingEmployee.setDepartment((String) request.get("department"));
                    }
                    if (request.containsKey("position")) {
                        existingEmployee.setPosition((String) request.get("position"));
                    }
                    if (request.containsKey("status")) {
                        existingEmployee.setStatus((String) request.get("status"));
                    }

                    return ResponseEntity.ok(nonLoginEmployeeRepository.save(existingEmployee));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 删除需要登录的员工
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else if (nonLoginEmployeeRepository.existsById(id)) {
            nonLoginEmployeeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // 删除不需要登录的员工
    @DeleteMapping("/non-login-employees/{id}")
    public ResponseEntity<Void> deleteNonLoginEmployee(@PathVariable Long id) {
        if (!nonLoginEmployeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        nonLoginEmployeeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    

    
    // 按状态筛选员工
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByStatus(@PathVariable String status) {
        List<Map<String, Object>> filteredEmployees = new ArrayList<>();
        
        // 添加需要登录的员工
        for (Employee emp : employeeRepository.findByStatus(status)) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", emp.getUsername());
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("role", emp.getRole().getName());
            empMap.put("roleId", emp.getRole().getId());
            filteredEmployees.add(empMap);
        }
        
        // 添加不需要登录的员工
        for (NonLoginEmployee emp : nonLoginEmployeeRepository.findByStatus(status)) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", null);
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("permissionLevel", 0);
            filteredEmployees.add(empMap);
        }
        
        return ResponseEntity.ok(filteredEmployees);
    }
    
    // 按部门筛选员工
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByDepartment(@PathVariable String department) {
        List<Map<String, Object>> filteredEmployees = new ArrayList<>();
        
        // 添加需要登录的员工
        for (Employee emp : employeeRepository.findByDepartment(department)) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", emp.getUsername());
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("role", emp.getRole().getName());
            empMap.put("roleId", emp.getRole().getId());
            filteredEmployees.add(empMap);
        }
        
        // 添加不需要登录的员工
        for (NonLoginEmployee emp : nonLoginEmployeeRepository.findByDepartment(department)) {
            Map<String, Object> empMap = new java.util.HashMap<>();
            empMap.put("id", emp.getId());
            empMap.put("username", null);
            empMap.put("realName", emp.getRealName());
            empMap.put("email", emp.getEmail());
            empMap.put("phone", emp.getPhone());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("permissionLevel", 0);
            filteredEmployees.add(empMap);
        }
        
        return ResponseEntity.ok(filteredEmployees);
    }
    
    // 获取所有不同的部门
    @GetMapping("/departments")
    public ResponseEntity<List<String>> getDepartments() {
        List<String> departments = new ArrayList<>();
        
        // 从employee表中获取部门
        departments.addAll(employeeRepository.findDistinctDepartments());
        
        // 从nonLoginEmployee表中获取部门
        List<NonLoginEmployee> nonLoginEmployees = nonLoginEmployeeRepository.findAll();
        for (NonLoginEmployee emp : nonLoginEmployees) {
            if (emp.getDepartment() != null && !emp.getDepartment().isEmpty() && !departments.contains(emp.getDepartment())) {
                departments.add(emp.getDepartment());
            }
        }
        
        return ResponseEntity.ok(departments);
    }
}
