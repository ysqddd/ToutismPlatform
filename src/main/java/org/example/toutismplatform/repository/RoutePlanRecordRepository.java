package org.example.toutismplatform.repository;

import org.example.toutismplatform.entity.RoutePlanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutePlanRecordRepository extends JpaRepository<RoutePlanRecord, Long> {
    List<RoutePlanRecord> findByUserId(Long userId);
    List<RoutePlanRecord> findByStartAreaId(Long startAreaId);
    List<RoutePlanRecord> findByEndAreaId(Long endAreaId);
}
