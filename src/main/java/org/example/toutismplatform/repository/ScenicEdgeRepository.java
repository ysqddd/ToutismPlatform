package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.ScenicEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenicEdgeRepository extends JpaRepository<ScenicEdge, Long> {
    List<ScenicEdge> findByStartAreaId(Long startAreaId);
    List<ScenicEdge> findByEndAreaId(Long endAreaId);
    List<ScenicEdge> findByStartAreaIdAndEndAreaId(Long startAreaId, Long endAreaId);
    List<ScenicEdge> findByStartAreaIdOrEndAreaId(Long startAreaId, Long endAreaId);
    void deleteByStartAreaId(Long startAreaId);
    void deleteByEndAreaId(Long endAreaId);
    boolean existsByStartAreaIdAndEndAreaId(Long startAreaId, Long endAreaId);
}
