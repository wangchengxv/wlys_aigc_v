package com.miioo.backend.integration.ai;

import org.springframework.stereotype.Component;

@Component
public class ProviderRouter {
    private final MockAiProvider mockAiProvider;

    public ProviderRouter(MockAiProvider mockAiProvider) {
        this.mockAiProvider = mockAiProvider;
    }

    public AiProvider provider() {
        return mockAiProvider;
    }
}
