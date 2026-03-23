package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.Employee;
import org.example.toutismplatform.entity.User;
import org.example.toutismplatform.repository.EmployeeRepository;
import org.example.toutismplatform.repository.UserRepository;
import org.example.toutismplatform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // 验证必填字段
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("用户名不能为空");
            }
            
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("邮箱不能为空");
            }
            
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("密码不能为空");
            }
            
            if (user.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body("密码长度至少为 6 位");
            }
            
            // 检查用户名是否存在
            if (userRepository.existsByUsername(user.getUsername())) {
                return ResponseEntity.badRequest().body("用户名已存在，请使用其他用户名");
            }
            
            // 检查邮箱是否存在
            if (userRepository.existsByEmail(user.getEmail())) {
                return ResponseEntity.badRequest().body("邮箱已被注册，请更换其他邮箱");
            }
            
            // 加密密码
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            
            // 保存用户
            User savedUser = userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "注册成功");
            response.put("username", savedUser.getUsername());
            response.put("userId", savedUser.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "注册失败：" + e.getMessage();
            
            // 更详细的错误信息
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Duplicate entry")) {
                    errorMessage = "注册失败：用户名或邮箱已存在";
                } else if (e.getMessage().contains("Data too long")) {
                    errorMessage = "注册失败：输入的数据长度超过限制";
                } else if (e.getMessage().contains("Incorrect email format")) {
                    errorMessage = "注册失败：邮箱格式不正确";
                }
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            System.out.println("=== 用户登录请求 ===");
            System.out.println("用户名：" + user.getUsername());
            
            if (employeeRepository.existsByUsername(user.getUsername())) {
                System.err.println("该账号是管理员账号，请使用管理员登录入口");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("该账号是管理员账号，请使用管理员登录入口");
            }
            
            if (!userRepository.existsByUsername(user.getUsername())) {
                System.err.println("用户不存在：" + user.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户不存在：" + user.getUsername());
            }
            
            User existingUser = userRepository.findByUsername(user.getUsername()).get();
            System.out.println("数据库中的用户信息:");
            System.out.println("  - username: " + existingUser.getUsername());
            System.out.println("  - password: " + existingUser.getPassword());
            System.out.println("输入的密码：" + user.getPassword());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            
            System.out.println("认证成功：" + authentication.getName());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(user.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("isUser", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== 登录失败 ===");
            e.printStackTrace();
            System.err.println("错误类型：" );
            System.err.println("错误信息：" );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误：");
        }
    }
    
    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> loginRequest) {
        try {
            System.out.println("=== 管理员登录请求 ===");
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            System.out.println("用户名：" + username);
            
            if (userRepository.existsByUsername(username)) {
                System.err.println("该账号是普通用户账号，请使用用户登录入口");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("该账号是普通用户账号，请使用用户登录入口");
            }
            
            if (!employeeRepository.existsByUsername(username)) {
                System.err.println("管理员不存在：" + username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("管理员不存在：" + username);
            }
            
            Employee existingEmployee = employeeRepository.findByUsername(username).get();
            System.out.println("数据库中的管理员信息:");
            System.out.println("  - username: " + existingEmployee.getUsername());
            System.out.println("  - role: " + existingEmployee.getRole().getName());
            
            String roleName = existingEmployee.getRole().getName();
            if (!roleName.equals("系统管理员") && !roleName.equals("用户管理员") && !roleName.equals("员工管理员") && !roleName.equals("商品管理员") && !roleName.equals("景区管理员")) {
                System.err.println("用户不是管理员：" + username);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("该用户不是管理员，无权访问管理后台");
            }
            
            boolean passwordMatches = false;
            try {
                passwordMatches = passwordEncoder.matches(password, existingEmployee.getPassword());
            } catch (Exception e) {
                passwordMatches = password.equals(existingEmployee.getPassword());
                if (passwordMatches) {
                    existingEmployee.setPassword(passwordEncoder.encode(password));
                    employeeRepository.save(existingEmployee);
                    System.out.println("密码已更新为BCrypt格式");
                }
            }
            
            if (!passwordMatches) {
                System.err.println("密码错误：" + username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("管理员用户名或密码错误");
            }
            
            System.out.println("管理员登录认证成功");
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username, password
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtUtil.generateToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            response.put("isAdmin", true);
            response.put("role", existingEmployee.getRole().getName());
            response.put("roleId", existingEmployee.getRole().getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== 管理员登录失败 ===");
            e.printStackTrace();
            System.err.println("错误类型：" + e.getClass().getName());
            System.err.println("错误信息：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("管理员用户名或密码错误");
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // 由于使用 JWT，服务端不需要真正注销，客户端清除 token 即可
        Map<String, Object> response = new HashMap<>();
        response.put("message", "注销成功");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录或 Token 无效");
        }
    }
}