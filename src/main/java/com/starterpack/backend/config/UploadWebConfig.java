package com.starterpack.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;

    public UploadWebConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicBaseUrl = uploadProperties.getLocal().getPublicBaseUrl();
        if (!publicBaseUrl.startsWith("/")) {
            publicBaseUrl = "/" + publicBaseUrl;
        }

        Path basePath = Paths.get(uploadProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        String location = basePath.toUri().toString();

        registry.addResourceHandler(publicBaseUrl + "/**")
                .addResourceLocations(location);
    }
}
