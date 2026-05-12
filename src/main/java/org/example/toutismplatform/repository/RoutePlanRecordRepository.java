package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.RoutePlanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutePlanRecordRepository extends JpaRepository<RoutePlanRecord, Long> {
    List<RoutePlanRecord> findByUserId(Long userId);
    List<RoutePlanRecord> findByStartAreaId(Long startAreaId);
    List<RoutePlanRecord> findByEndAreaId(Long endAreaId);
}
