package com.example.aigcspringai.service.fallback;

import com.example.aigcspringai.dto.TextGenerationContext;
import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.dto.UsageStats;
import com.example.aigcspringai.exception.TextGenerationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockFallbackGateway implements FallbackGateway {

    @Override
    public TextGenerationResult fallbackGenerate(
            TextGenerationRequest request,
            TextGenerationContext context,
            TextGenerationException cause
    ) {
        String content = "fallback-response: " + cause.getMessage();
        return new TextGenerationResult(
                content,
                "fallback",
                UsageStats.empty(),
                request.providerCode(),
                request.modelCode(),
                "fallback-gateway",
                true
        );
    }

    @Override
    public List<TextGenerationStreamChunk> fallbackStream(
            TextGenerationRequest request,
            TextGenerationContext context,
            TextGenerationException cause
    ) {
        return List.of(new TextGenerationStreamChunk("fallback-stream: " + cause.getMessage(), true, "fallback"));
    }
}
