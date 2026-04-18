package com.example.aigc.dto;

import java.util.List;

public record AdminUserImportResultResponse(
        String taskId,
        int totalRows,
        int successRows,
        int failedRows,
        List<AdminUserImportErrorItem> errors
) {
}
