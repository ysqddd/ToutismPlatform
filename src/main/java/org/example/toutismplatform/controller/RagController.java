package org.example.toutismplatform.controller;

import org.example.toutismplatform.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/generate")
    public Map<String, String> generateAnswer(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String answer = ragService.generateAnswer(query);
        return Map.of("answer", answer);
    }
}
