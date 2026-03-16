package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.NonLoginEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NonLoginEmployeeRepository extends JpaRepository<NonLoginEmployee, Long> {
    List<NonLoginEmployee> findByDepartment(String department);
    List<NonLoginEmployee> findByStatus(String status);
}