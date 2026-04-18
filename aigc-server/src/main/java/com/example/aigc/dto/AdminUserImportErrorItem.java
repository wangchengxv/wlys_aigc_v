package com.example.aigc.dto;

public record AdminUserImportErrorItem(
        int rowNumber,
        String username,
        String message
) {
}
