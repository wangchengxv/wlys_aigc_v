package com.example.aigcspringai.service.ai;

import com.example.aigcspringai.dto.TextGenerationContext;
import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;

import java.util.List;

public interface TextGenerationFacade {
    TextGenerationResult generate(TextGenerationRequest request, TextGenerationContext context);

    List<TextGenerationStreamChunk> stream(TextGenerationRequest request, TextGenerationContext context);
}
