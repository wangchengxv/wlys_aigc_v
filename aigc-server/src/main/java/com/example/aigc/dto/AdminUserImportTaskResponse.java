package com.example.aigc.dto;

import java.util.List;

public record AdminUserImportTaskResponse(
        String taskId,
        String status,
        String sourceFileName,
        String operatorUserId,
        String operatorUserName,
        int totalRows,
        int successRows,
        int failedRows,
        String createdAt,
        String finishedAt,
        List<AdminUserImportErrorItem> errors
) {
}
