package com.example.aigc.dto;

public record PromptTemplateCatalogItem(
        String path,
        String title,
        String category,
        String description,
        String defaultBody
) {
}
