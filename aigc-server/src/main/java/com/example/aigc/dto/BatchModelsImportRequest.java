package com.example.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchModelsImportRequest(
        @NotBlank(message = "connectionId 不能为空") String connectionId,
        @NotEmpty(message = "modelNames 不能为空") List<String> modelNames,
        /** Defaults to [\"text\"] when null or empty. */
        List<String> capabilities
) {
}
