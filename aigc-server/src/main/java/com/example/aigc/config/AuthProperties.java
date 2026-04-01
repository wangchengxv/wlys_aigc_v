package com.example.aigc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aigc.auth")
public class AuthProperties {
    private String accessToken = "dev-local-token";
    private boolean userIdRequired = true;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isUserIdRequired() {
        return userIdRequired;
    }

    public void setUserIdRequired(boolean userIdRequired) {
        this.userIdRequired = userIdRequired;
    }
}
