package com.example.aigc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aigc.comfy")
public class AigcComfyProperties {
    private String baseUrl = "http://127.0.0.1:8188";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
