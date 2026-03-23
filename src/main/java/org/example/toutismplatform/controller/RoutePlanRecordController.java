package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.RoutePlanRecord;
import org.example.toutismplatform.repository.RoutePlanRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-plans")
public class RoutePlanRecordController {

    @Autowired
    private RoutePlanRecordRepository routePlanRecordRepository;

    @GetMapping
    public ResponseEntity<List<RoutePlanRecord>> getAllRecords() {
        return ResponseEntity.ok(routePlanRecordRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoutePlanRecord> getRecordById(@PathVariable Long id) {
        return routePlanRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RoutePlanRecord>> getRecordsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(routePlanRecordRepository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<RoutePlanRecord> createRecord(@RequestBody RoutePlanRecord record) {
        return ResponseEntity.ok(routePlanRecordRepository.save(record));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        if (!routePlanRecordRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        routePlanRecordRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
