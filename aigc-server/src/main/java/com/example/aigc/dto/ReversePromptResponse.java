package com.example.aigc.dto;

import java.util.Map;

public record ReversePromptResponse(
        String model,
        String positivePrompt,
        String negativePrompt,
        String style,
        String lighting,
        String composition,
        String camera,
        String colorTone,
        Map<String, String> parameters,
        String rawText
) {
}
