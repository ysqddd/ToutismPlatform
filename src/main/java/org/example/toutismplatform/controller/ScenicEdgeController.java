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

    @GetMapping("/large-area/{largeAreaId}")
    public ResponseEntity<List<ScenicEdge>> getEdgesByLargeArea(@PathVariable Long largeAreaId) {
        return ResponseEntity.ok(scenicEdgeRepository.findByLargeAreaId(largeAreaId));
    }

    @GetMapping("/start-spot/{startSpotId}")
    public ResponseEntity<List<ScenicEdge>> getEdgesByStartSpot(@PathVariable Long startSpotId) {
        return ResponseEntity.ok(scenicEdgeRepository.findByStartSpotId(startSpotId));
    }

    @GetMapping("/end-spot/{endSpotId}")
    public ResponseEntity<List<ScenicEdge>> getEdgesByEndSpot(@PathVariable Long endSpotId) {
        return ResponseEntity.ok(scenicEdgeRepository.findByEndSpotId(endSpotId));
    }

    @PostMapping
    public ResponseEntity<ScenicEdge> createEdge(@RequestBody ScenicEdge edge) {
        return ResponseEntity.ok(scenicEdgeRepository.save(edge));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenicEdge> updateEdge(@PathVariable Long id, @RequestBody ScenicEdge edge) {
        return scenicEdgeRepository.findById(id)
                .map(existingEdge -> {
                    if (edge.getLargeAreaId() != null) {
                        existingEdge.setLargeAreaId(edge.getLargeAreaId());
                    }
                    if (edge.getStartSpotId() != null) {
                        existingEdge.setStartSpotId(edge.getStartSpotId());
                    }
                    if (edge.getEndSpotId() != null) {
                        existingEdge.setEndSpotId(edge.getEndSpotId());
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
                    if (edge.getTransportMode() != null) {
                        existingEdge.setTransportMode(edge.getTransportMode());
                    }
                    if (edge.getIntensityLevel() != null) {
                        existingEdge.setIntensityLevel(edge.getIntensityLevel());
                    }
                    if (edge.getScenicScore() != null) {
                        existingEdge.setScenicScore(edge.getScenicScore());
                    }
                    if (edge.getComfortScore() != null) {
                        existingEdge.setComfortScore(edge.getComfortScore());
                    }
                    if (edge.getElderlyFriendlyScore() != null) {
                        existingEdge.setElderlyFriendlyScore(edge.getElderlyFriendlyScore());
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
