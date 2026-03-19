package org.example.toutismplatform.controller;

import org.example.toutismplatform.service.PathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/path")
public class PathController {
    @Autowired
    private PathService pathService;

    // 计算大景区之间的最优路径
    @GetMapping("/shortest")
    public ResponseEntity<Map<String, Object>> calculateShortestPath(
            @RequestParam Long startAreaId,
            @RequestParam Long endAreaId,
            @RequestParam(defaultValue = "distance") String weightType) {
        Map<String, Object> result = pathService.calculateShortestPath(startAreaId, endAreaId, weightType);
        return ResponseEntity.ok(result);
    }
}
