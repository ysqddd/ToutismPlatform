package org.example.toutismplatform.service;

import org.example.toutismplatform.entity.Employee;
import org.example.toutismplatform.entity.User;
import org.example.toutismplatform.repository.EmployeeRepository;
import org.example.toutismplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 首先检查User表
        java.util.Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<GrantedAuthority> authorities = new ArrayList<>();
            
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            System.out.println("=== UserDetailsServiceImpl ===");
            System.out.println("加载用户：" + user.getUsername());
            System.out.println("权限列表：" + authorities);
            
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    authorities
            );
        }
        
        // 检查Employee表
        java.util.Optional<Employee> employeeOptional = employeeRepository.findByUsername(username);
        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            List<GrantedAuthority> authorities = new ArrayList<>();
            
            // 根据角色添加权限
            String roleName = employee.getRole().getName();
            if (roleName.equals("系统管理员") || roleName.equals("用户管理员") || roleName.equals("员工管理员") || roleName.equals("商品管理员") || roleName.equals("景区管理员")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            System.out.println("=== UserDetailsServiceImpl ===");
            System.out.println("加载员工：" + employee.getUsername());
            System.out.println("角色：" + roleName);
            System.out.println("权限列表：" + authorities);
            
            return new org.springframework.security.core.userdetails.User(
                    employee.getUsername(),
                    employee.getPassword(),
                    authorities
            );
        }
        
        // 两个表都找不到用户
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}