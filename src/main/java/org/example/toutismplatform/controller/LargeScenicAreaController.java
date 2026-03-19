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
        // 过滤掉公共措施，只返回非公共措施的景区
        List<LargeScenicArea> nonPublicAreas = areas.stream()
                .filter(area -> !area.isPublicFacility())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(nonPublicAreas);
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
                    // 更新是否是公共措施
                    existingArea.setPublicFacility(largeArea.isPublicFacility());
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