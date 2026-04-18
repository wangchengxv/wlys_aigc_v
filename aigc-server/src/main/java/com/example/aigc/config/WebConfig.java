package com.example.aigc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOriginPatterns;
    private final ScriptProjectAccessInterceptor scriptProjectAccessInterceptor;

    public WebConfig(
            @Value("${aigc.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}") String allowedOriginPatternsRaw,
            ScriptProjectAccessInterceptor scriptProjectAccessInterceptor
    ) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatternsRaw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        this.scriptProjectAccessInterceptor = scriptProjectAccessInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(scriptProjectAccessInterceptor)
                .addPathPatterns("/api/v1/script-projects")
                .addPathPatterns("/api/v1/script-projects/**");
    }
}
