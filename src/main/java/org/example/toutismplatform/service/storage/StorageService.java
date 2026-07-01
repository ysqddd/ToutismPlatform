package org.example.toutismplatform.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    StoredFile store(MultipartFile file) throws IOException;
}
