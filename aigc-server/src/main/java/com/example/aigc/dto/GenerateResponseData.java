package com.example.aigc.dto;

import com.example.aigc.enums.GenerateMode;
import com.example.aigc.enums.TaskStatus;

import java.util.List;

public record GenerateResponseData(
        String taskId,
        TaskStatus status,
        List<String> textResults,
        List<String> imageResults,
        List<String> videoResults,
        String createdAt,
        long latencyMs,
        String prompt,
        GenerateMode mode,
        String style,
        String imageModel,
        String videoModel
) {
}
