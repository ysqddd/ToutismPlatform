package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.ScenicAreaEdge;
import org.example.toutismplatform.repository.ScenicAreaEdgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scenic-area-edges")
public class ScenicAreaEdgeController {
    @Autowired
    private ScenicAreaEdgeRepository scenicAreaEdgeRepository;

    @GetMapping
    public ResponseEntity<List<ScenicAreaEdge>> getAllEdges() {
        List<ScenicAreaEdge> edges = scenicAreaEdgeRepository.findAll();
        return ResponseEntity.ok(edges);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenicAreaEdge> getEdgeById(@PathVariable Long id) {
        return scenicAreaEdgeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ScenicAreaEdge> createEdge(@RequestBody ScenicAreaEdge edge) {
        ScenicAreaEdge savedEdge = scenicAreaEdgeRepository.save(edge);
        return ResponseEntity.ok(savedEdge);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScenicAreaEdge> updateEdge(@PathVariable Long id, @RequestBody ScenicAreaEdge edge) {
        return scenicAreaEdgeRepository.findById(id)
                .map(existingEdge -> {
                    if (edge.getFromAreaId() != null) {
                        existingEdge.setFromAreaId(edge.getFromAreaId());
                    }
                    if (edge.getToAreaId() != null) {
                        existingEdge.setToAreaId(edge.getToAreaId());
                    }
                    if (edge.getDistance() != null) {
                        existingEdge.setDistance(edge.getDistance());
                    }
                    if (edge.getDuration() != null) {
                        existingEdge.setDuration(edge.getDuration());
                    }
                    if (edge.getDescription() != null) {
                        existingEdge.setDescription(edge.getDescription());
                    }
                    if (edge.getCostAmount() != null) {
                        existingEdge.setCostAmount(edge.getCostAmount());
                    }
                    return ResponseEntity.ok(scenicAreaEdgeRepository.save(existingEdge));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEdge(@PathVariable Long id) {
        if (!scenicAreaEdgeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scenicAreaEdgeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
