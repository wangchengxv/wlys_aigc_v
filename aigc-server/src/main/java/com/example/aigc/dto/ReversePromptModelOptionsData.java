package com.example.aigc.dto;

import java.util.List;

public record ReversePromptModelOptionsData(
        String defaultModel,
        List<String> options,
        List<ModelOptionDetailData> details
) {
}
