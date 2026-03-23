package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Employee;
import org.example.toutismplatform.entity.Role;
import org.example.toutismplatform.repository.EmployeeRepository;
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
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        List<Map<String, Object>> allEmployees = new ArrayList<>();
        
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
        
        return ResponseEntity.ok(allEmployees);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Map<String, Object> request) {
        if (employeeRepository.existsByUsername((String) request.get("username"))) {
            return ResponseEntity.badRequest().body(null);
        }
        
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
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return employeeRepository.findById(id)
                .map(existingEmployee -> {
                    if (request.containsKey("realName")) {
                        existingEmployee.setRealName((String) request.get("realName"));
                    }
                    if (request.containsKey("email")) {
                        String newEmail = (String) request.get("email");
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
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByStatus(@PathVariable String status) {
        List<Map<String, Object>> filteredEmployees = new ArrayList<>();
        
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
        
        return ResponseEntity.ok(filteredEmployees);
    }
    
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesByDepartment(@PathVariable String department) {
        List<Map<String, Object>> filteredEmployees = new ArrayList<>();
        
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
        
        return ResponseEntity.ok(filteredEmployees);
    }
    
    @GetMapping("/departments")
    public ResponseEntity<List<String>> getDepartments() {
        List<String> departments = new ArrayList<>();
        departments.addAll(employeeRepository.findDistinctDepartments());
        return ResponseEntity.ok(departments);
    }
}
