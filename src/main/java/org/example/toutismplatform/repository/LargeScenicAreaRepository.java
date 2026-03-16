package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LargeScenicAreaRepository extends JpaRepository<LargeScenicArea, Long> {
    List<LargeScenicArea> findByProductId(Long productId);
}
