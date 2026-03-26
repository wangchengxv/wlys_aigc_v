package com.example.aigc.dto;

import java.util.List;

public record VideoModelOptionsData(
        String defaultModel,
        List<String> options
) {
}
