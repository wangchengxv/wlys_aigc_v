package com.example.aigc.dto;

import java.util.List;

public record RouterConnectionTestResponse(
        boolean ok,
        String message,
        List<String> models
) {
}
