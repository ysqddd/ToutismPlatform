package org.example.toutismplatform.service.storage;

import org.example.toutismplatform.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void storesFileInConfiguredLocalDirectory() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setUrlPrefix("/assets/images");
        properties.getLocal().setBasePath(tempDir.toString());
        LocalFileStorageService service = new LocalFileStorageService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "../龙亭 照片.jpg",
                "image/jpeg",
                "image-data".getBytes(StandardCharsets.UTF_8)
        );

        StoredFile storedFile = service.store(file);

        assertThat(storedFile.url()).startsWith("/assets/images/");
        assertThat(storedFile.fileName()).doesNotContain("/", "\\");
        assertThat(Files.readString(tempDir.resolve(storedFile.fileName()))).isEqualTo("image-data");
    }

    @Test
    void rejectsEmptyFile() {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setBasePath(tempDir.toString());
        LocalFileStorageService service = new LocalFileStorageService(properties);
        MockMultipartFile file = new MockMultipartFile("image", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("上传文件不能为空");
    }
}
