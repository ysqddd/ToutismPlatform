package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.ScenicEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenicEdgeRepository extends JpaRepository<ScenicEdge, Long> {
    List<ScenicEdge> findByLargeAreaId(Long largeAreaId);
    List<ScenicEdge> findByStartSpotId(Long startSpotId);
    List<ScenicEdge> findByEndSpotId(Long endSpotId);
    List<ScenicEdge> findByLargeAreaIdAndStartSpotId(Long largeAreaId, Long startSpotId);
    List<ScenicEdge> findByLargeAreaIdAndEndSpotId(Long largeAreaId, Long endSpotId);
    List<ScenicEdge> findByStartSpotIdAndEndSpotId(Long startSpotId, Long endSpotId);
    void deleteByLargeAreaId(Long largeAreaId);
    boolean existsByStartSpotIdAndEndSpotId(Long startSpotId, Long endSpotId);
}
