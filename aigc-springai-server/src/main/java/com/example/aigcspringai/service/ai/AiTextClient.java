package com.example.aigcspringai.service.ai;

import com.example.aigcspringai.dto.TextGenerationRequest;
import com.example.aigcspringai.dto.TextGenerationResult;
import com.example.aigcspringai.dto.TextGenerationStreamChunk;

import java.util.List;

public interface AiTextClient {
    TextGenerationResult generate(TextGenerationRequest request);

    List<TextGenerationStreamChunk> generateStream(TextGenerationRequest request);
}
