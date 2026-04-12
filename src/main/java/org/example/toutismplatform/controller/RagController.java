package org.example.toutismplatform.controller;

import org.example.toutismplatform.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/generate")
    public Map<String, String> generateAnswer(@RequestBody Map<String, Object> request) {
        String query = request.get("query") == null ? null : String.valueOf(request.get("query"));
        Long userId = toLong(request.get("userId"));
        String username = request.get("username") == null ? null : String.valueOf(request.get("username"));
        String answer = ragService.generateAnswer(query, userId, username);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("answer", answer);
        return response;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
