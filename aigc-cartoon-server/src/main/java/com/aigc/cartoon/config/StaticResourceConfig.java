package com.aigc.cartoon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadDir = Path.of(uploadBaseDir).toAbsolutePath().normalize().toString();
        String location = "file:" + absoluteUploadDir + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
