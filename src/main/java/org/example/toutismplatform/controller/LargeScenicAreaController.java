package org.example.toutismplatform.controller;

import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/large-areas")
public class LargeScenicAreaController {
    @Autowired
    private LargeScenicAreaRepository largeScenicAreaRepository;
    
    // 获取所有大景区（非公共措施）
    @GetMapping
    public ResponseEntity<List<LargeScenicArea>> getAllLargeAreas() {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        List<LargeScenicArea> scenicAreas = areas.stream()
                .filter(area -> area.getIsAreaType() == null || area.getIsAreaType() == 0)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(scenicAreas);
    }
    
    // 获取所有大景区（包括公共措施，供管理员使用）
    @GetMapping("/all")
    public ResponseEntity<List<LargeScenicArea>> getAllLargeAreasIncludingPublic() {
        List<LargeScenicArea> areas = largeScenicAreaRepository.findAll();
        return ResponseEntity.ok(areas);
    }
    
    // 根据 ID 获取大景区详情
    @GetMapping("/{id}")
    public ResponseEntity<LargeScenicArea> getLargeAreaById(@PathVariable Long id) {
        return largeScenicAreaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 创建大景区
    @PostMapping
    public ResponseEntity<LargeScenicArea> createLargeArea(@RequestBody LargeScenicArea largeArea) {
        LargeScenicArea savedArea = largeScenicAreaRepository.save(largeArea);
        return ResponseEntity.ok(savedArea);
    }
    
    // 更新大景区
    @PutMapping("/{id}")
    public ResponseEntity<LargeScenicArea> updateLargeArea(@PathVariable Long id, @RequestBody LargeScenicArea largeArea) {
        return largeScenicAreaRepository.findById(id)
                .map(existingArea -> {
                    if (largeArea.getName() != null) {
                        existingArea.setName(largeArea.getName());
                    }
                    if (largeArea.getDescription() != null) {
                        existingArea.setDescription(largeArea.getDescription());
                    }
                    if (largeArea.getLocation() != null) {
                        existingArea.setLocation(largeArea.getLocation());
                    }
                    if (largeArea.getImageUrl() != null) {
                        existingArea.setImageUrl(largeArea.getImageUrl());
                    }
                    if (largeArea.getOpeningHours() != null) {
                        existingArea.setOpeningHours(largeArea.getOpeningHours());
                    }
                    if (largeArea.getPrice() != null) {
                        existingArea.setPrice(largeArea.getPrice());
                    }
                    if (largeArea.getTags() != null) {
                        existingArea.setTags(largeArea.getTags());
                    }
                    if (largeArea.getIsAreaType() != null) {
                        existingArea.setIsAreaType(largeArea.getIsAreaType());
                    }
                    if (largeArea.getRecommendedVisitDuration() != null) {
                        existingArea.setRecommendedVisitDuration(largeArea.getRecommendedVisitDuration());
                    }
                    if (largeArea.getIntensityLevel() != null) {
                        existingArea.setIntensityLevel(largeArea.getIntensityLevel());
                    }
                    if (largeArea.getCrowdLevel() != null) {
                        existingArea.setCrowdLevel(largeArea.getCrowdLevel());
                    }
                    if (largeArea.getFamilyFriendlyScore() != null) {
                        existingArea.setFamilyFriendlyScore(largeArea.getFamilyFriendlyScore());
                    }
                    if (largeArea.getElderlyFriendlyScore() != null) {
                        existingArea.setElderlyFriendlyScore(largeArea.getElderlyFriendlyScore());
                    }
                    if (largeArea.getNatureScore() != null) {
                        existingArea.setNatureScore(largeArea.getNatureScore());
                    }
                    if (largeArea.getCultureScore() != null) {
                        existingArea.setCultureScore(largeArea.getCultureScore());
                    }
                    if (largeArea.getPhotographyScore() != null) {
                        existingArea.setPhotographyScore(largeArea.getPhotographyScore());
                    }
                    if (largeArea.getLeisureScore() != null) {
                        existingArea.setLeisureScore(largeArea.getLeisureScore());
                    }
                    if (largeArea.getFoodConvenienceScore() != null) {
                        existingArea.setFoodConvenienceScore(largeArea.getFoodConvenienceScore());
                    }
                    if (largeArea.getRestroomConvenienceScore() != null) {
                        existingArea.setRestroomConvenienceScore(largeArea.getRestroomConvenienceScore());
                    }
                    if (largeArea.getPopularityScore() != null) {
                        existingArea.setPopularityScore(largeArea.getPopularityScore());
                    }
                    return ResponseEntity.ok(largeScenicAreaRepository.save(existingArea));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // 删除大景区
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLargeArea(@PathVariable Long id) {
        if (!largeScenicAreaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        largeScenicAreaRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}