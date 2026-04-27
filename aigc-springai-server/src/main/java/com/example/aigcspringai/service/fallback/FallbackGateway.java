package com.example.aigcspringai.service.fallback;

import com.example.aigcspringai.dto.TextGenerationContext;
import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;
import com.example.aigcspringai.exception.TextGenerationException;

import java.util.List;

public interface FallbackGateway {

    TextGenerationResult fallbackGenerate(
            TextGenerationRequest request,
            TextGenerationContext context,
            TextGenerationException cause
    );

    List<TextGenerationStreamChunk> fallbackStream(
            TextGenerationRequest request,
            TextGenerationContext context,
            TextGenerationException cause
    );
}
