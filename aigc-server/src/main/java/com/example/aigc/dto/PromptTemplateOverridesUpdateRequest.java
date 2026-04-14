package com.example.aigc.dto;

import java.util.Map;

/**
 * 与 classpath 模板路径对齐的 key；value 为完整模板正文，传 null 或空字符串表示删除该路径覆盖。
 */
public record PromptTemplateOverridesUpdateRequest(Map<String, String> overrides) {
}
