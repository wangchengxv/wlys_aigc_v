package com.example.aigc.service.social;

import com.example.aigc.config.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class WecomSocialProvider extends AbstractOnelinkDelegatingSocialProvider {
    public WecomSocialProvider(AuthProperties authProperties, ObjectMapper objectMapper) {
        super(authProperties, objectMapper);
    }

    @Override
    public String provider() {
        return "wecom";
    }

    @Override
    protected String providerDisplayName() {
        return "企业微信";
    }
}
