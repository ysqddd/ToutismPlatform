package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);
    
    Optional<Employee> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<Employee> findByStatus(String status);
    
    List<Employee> findByDepartment(String department);
    
    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL AND e.department != ''")
    List<String> findDistinctDepartments();
}
