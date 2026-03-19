package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(String status);
    
    // 关键修复：使用 DISTINCT 避免 JOIN FETCH 产生的笛卡尔积重复数据
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.largeScenicAreas")
    List<Product> findAllWithScenicAreas();
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.largeScenicAreas WHERE p.id = :id")
    Optional<Product> findByIdWithScenicAreas(Long id);
    
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.largeScenicAreas WHERE p.status = :status")
    List<Product> findOnSaleWithScenicAreas(String status);
}
