package org.example.toutismplatform.service.storage;

import org.example.toutismplatform.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements StorageService {
    private final StorageProperties storageProperties;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String fileName = buildSafeFileName(file.getOriginalFilename());
        Path storageRoot = storageRoot();
        Files.createDirectories(storageRoot);

        Path target = storageRoot.resolve(fileName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IOException("文件存储路径非法");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredFile(fileName, storageProperties.normalizedUrlPrefix() + "/" + fileName);
    }

    private Path storageRoot() {
        String basePath = storageProperties.getLocal() == null ? null : storageProperties.getLocal().getBasePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "uploads/images";
        }
        return Paths.get(basePath).toAbsolutePath().normalize();
    }

    private String buildSafeFileName(String originalFilename) {
        String cleanedName = StringUtils.cleanPath(originalFilename == null ? "image" : originalFilename);
        cleanedName = cleanedName.replace("\\", "/");
        int lastSlash = cleanedName.lastIndexOf('/');
        if (lastSlash >= 0) {
            cleanedName = cleanedName.substring(lastSlash + 1);
        }
        cleanedName = cleanedName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleanedName.isBlank() || ".".equals(cleanedName) || "..".equals(cleanedName)) {
            cleanedName = "image";
        }
        return UUID.randomUUID() + "_" + cleanedName;
    }
}
