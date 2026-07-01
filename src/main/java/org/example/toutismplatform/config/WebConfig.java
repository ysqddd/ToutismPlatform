package org.example.toutismplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StorageProperties storageProperties;

    public WebConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourcePattern = storageProperties.normalizedUrlPrefix() + "/**";
        String localResourceLocation = localResourceLocation();

        registry.addResourceHandler(resourcePattern)
                .addResourceLocations(localResourceLocation, "classpath:/images/");
    }

    private String localResourceLocation() {
        String basePath = storageProperties.getLocal() == null ? null : storageProperties.getLocal().getBasePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "uploads/images";
        }

        Path path = Paths.get(basePath).toAbsolutePath().normalize();
        String location = path.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
