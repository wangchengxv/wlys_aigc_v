package com.example.aigc.dto;

import com.example.aigc.enums.PromptVersionSource;

/**
 * 提示词编辑历史条目（对齐前端/BigBanana 语义）。
 */
public record PromptVersion(
        String id,
        String prompt,
        long createdAt,
        PromptVersionSource source,
        String note
) {
}
