package org.example.toutismplatform.controller;

import org.example.toutismplatform.service.storage.StorageService;
import org.example.toutismplatform.service.storage.StoredFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    private final StorageService storageService;

    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        try {
            StoredFile storedFile = storageService.store(file);
            response.put("imageUrl", storedFile.url());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "上传失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
