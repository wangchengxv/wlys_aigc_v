package com.example.aigc.dto;

import java.util.List;

public record ImageModelOptionsData(
        String defaultModel,
        List<String> options
) {
}
