package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/small-spots")
public class SmallScenicSpotController {
    @Autowired
    private SmallScenicSpotRepository smallScenicSpotRepository;

    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSmallSpots() {
        List<SmallScenicSpot> spots = smallScenicSpotRepository.findAll();
        List<Map<String, Object>> result = spots.stream()
                .map(this::toSmallSpotResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/large-area/{largeAreaId}")
    public ResponseEntity<List<Map<String, Object>>> getSmallSpotsByLargeAreaId(@PathVariable Long largeAreaId) {
        List<SmallScenicSpot> spots = smallScenicSpotRepository.findByLargeAreaId(largeAreaId);
        List<Map<String, Object>> result = spots.stream()
                .map(this::toSmallSpotResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSmallSpotById(@PathVariable Long id) {
        return smallScenicSpotRepository.findById(id)
                .map(this::toSmallSpotResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSmallSpot(@RequestBody SmallScenicSpot smallSpot) {
        SmallScenicSpot savedSpot = smallScenicSpotRepository.save(smallSpot);
        return ResponseEntity.ok(toSmallSpotResponse(savedSpot));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSmallSpot(@PathVariable Long id, @RequestBody SmallScenicSpot smallSpot) {
        return smallScenicSpotRepository.findById(id)
                .map(existingSpot -> {
                    if (smallSpot.getName() != null) {
                        existingSpot.setName(smallSpot.getName());
                    }
                    if (smallSpot.getDescription() != null) {
                        existingSpot.setDescription(smallSpot.getDescription());
                    }
                    if (smallSpot.getImageUrl() != null) {
                        existingSpot.setImageUrl(smallSpot.getImageUrl());
                    }
                    if (smallSpot.getVisitingDuration() != null) {
                        existingSpot.setVisitingDuration(smallSpot.getVisitingDuration());
                    }
                    if (smallSpot.getTags() != null) {
                        existingSpot.setTags(smallSpot.getTags());
                    }
                    if (smallSpot.getIsSpotType() != null) {
                        existingSpot.setIsSpotType(smallSpot.getIsSpotType());
                    }
                    if (smallSpot.getIntensityLevel() != null) {
                        existingSpot.setIntensityLevel(smallSpot.getIntensityLevel());
                    }
                    if (smallSpot.getQueueLevel() != null) {
                        existingSpot.setQueueLevel(smallSpot.getQueueLevel());
                    }
                    if (smallSpot.getFamilyFriendlyScore() != null) {
                        existingSpot.setFamilyFriendlyScore(smallSpot.getFamilyFriendlyScore());
                    }
                    if (smallSpot.getElderlyFriendlyScore() != null) {
                        existingSpot.setElderlyFriendlyScore(smallSpot.getElderlyFriendlyScore());
                    }
                    if (smallSpot.getNatureScore() != null) {
                        existingSpot.setNatureScore(smallSpot.getNatureScore());
                    }
                    if (smallSpot.getCultureScore() != null) {
                        existingSpot.setCultureScore(smallSpot.getCultureScore());
                    }
                    if (smallSpot.getPhotographyScore() != null) {
                        existingSpot.setPhotographyScore(smallSpot.getPhotographyScore());
                    }
                    if (smallSpot.getRestConvenienceScore() != null) {
                        existingSpot.setRestConvenienceScore(smallSpot.getRestConvenienceScore());
                    }

                    SmallScenicSpot savedSpot = smallScenicSpotRepository.save(existingSpot);
                    return ResponseEntity.ok(toSmallSpotResponse(savedSpot));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSmallSpot(@PathVariable Long id) {
        if (!smallScenicSpotRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        smallScenicSpotRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toSmallSpotResponse(SmallScenicSpot spot) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", spot.getId());
        map.put("name", spot.getName());
        map.put("description", spot.getDescription());
        map.put("imageUrl", spot.getImageUrl());
        map.put("visitingDuration", spot.getVisitingDuration());
        map.put("tags", spot.getTags());
        map.put("largeAreaId", spot.getLargeAreaId());
        map.put("isSpotType", spot.getIsSpotType());
        map.put("intensityLevel", spot.getIntensityLevel());
        map.put("queueLevel", spot.getQueueLevel());
        map.put("familyFriendlyScore", spot.getFamilyFriendlyScore());
        map.put("elderlyFriendlyScore", spot.getElderlyFriendlyScore());
        map.put("natureScore", spot.getNatureScore());
        map.put("cultureScore", spot.getCultureScore());
        map.put("photographyScore", spot.getPhotographyScore());
        map.put("restConvenienceScore", spot.getRestConvenienceScore());
        map.put("createdAt", spot.getCreatedAt());
        map.put("updatedAt", spot.getUpdatedAt());

        if (spot.getLargeAreaId() != null) {
            largeScenicAreaRepository.findById(spot.getLargeAreaId())
                    .map(LargeScenicArea::getName)
                    .ifPresent(areaName -> map.put("areaName", areaName));
        }

        return map;
    }
}
