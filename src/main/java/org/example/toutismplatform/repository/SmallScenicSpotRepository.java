package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.SmallScenicSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmallScenicSpotRepository extends JpaRepository<SmallScenicSpot, Long> {
    List<SmallScenicSpot> findByLargeAreaId(Long largeAreaId);
}
