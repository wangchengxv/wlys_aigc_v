package com.example.aigc.service;

import com.example.aigc.dto.ReversePromptModelOptionsData;
import com.example.aigc.dto.ReversePromptRequest;
import com.example.aigc.dto.ReversePromptResponse;

public interface ReversePromptService {
    ReversePromptModelOptionsData listModels();

    ReversePromptResponse reversePrompt(ReversePromptRequest request, String ownerId);
}
