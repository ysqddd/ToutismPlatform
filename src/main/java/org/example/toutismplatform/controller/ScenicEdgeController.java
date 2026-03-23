package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.ScenicEdge;
import org.example.toutismplatform.repository.ScenicEdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenic-edges")
public class ScenicEdgeController {

    @Autowired
    private ScenicEdgeRepository scenicEdgeRepository;

    @GetMapping
    public ResponseEntity<List<ScenicEdge>> getAllEdges() {
        return ResponseEntity.ok(scenicEdgeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenicEdge> getEdgeById(@PathVariable Long id) {
        return scenicEdgeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/start/{startAreaId}")
    public ResponseEntity<List<ScenicEdge>> getEdgesByStartArea(@PathVariable Long startAreaId) {
        return ResponseEntity.ok(scenicEdgeRepository.findByStartAreaId(startAreaId));
    }

    @GetMapping("/end/{endAreaId}")
    public ResponseEntity<List<ScenicEdge>> getEdgesByEndArea(@PathVariable Long endAreaId) {
        return ResponseEntity.ok(scenicEdgeRepository.findByEndAreaId(endAreaId));
    }

    @PostMapping
    public ResponseEntity<ScenicEdge> createEdge(@RequestBody ScenicEdge edge) {
        return ResponseEntity.ok(scenicEdgeRepository.save(edge));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenicEdge> updateEdge(@PathVariable Long id, @RequestBody ScenicEdge edge) {
        return scenicEdgeRepository.findById(id)
                .map(existingEdge -> {
                    if (edge.getStartAreaId() != null) {
                        existingEdge.setStartAreaId(edge.getStartAreaId());
                    }
                    if (edge.getEndAreaId() != null) {
                        existingEdge.setEndAreaId(edge.getEndAreaId());
                    }
                    if (edge.getDistance() != null) {
                        existingEdge.setDistance(edge.getDistance());
                    }
                    if (edge.getTimeCost() != null) {
                        existingEdge.setTimeCost(edge.getTimeCost());
                    }
                    if (edge.getPathDescription() != null) {
                        existingEdge.setPathDescription(edge.getPathDescription());
                    }
                    return ResponseEntity.ok(scenicEdgeRepository.save(existingEdge));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long id) {
        if (!scenicEdgeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scenicEdgeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
