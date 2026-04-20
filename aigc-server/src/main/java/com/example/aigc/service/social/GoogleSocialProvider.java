package com.example.aigc.service.social;

import com.example.aigc.config.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GoogleSocialProvider extends AbstractOnelinkDelegatingSocialProvider {
    public GoogleSocialProvider(AuthProperties authProperties, ObjectMapper objectMapper) {
        super(authProperties, objectMapper);
    }

    @Override
    public String provider() {
        return "google";
    }

    @Override
    protected String providerDisplayName() {
        return "Google";
    }
}
