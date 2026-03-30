package com.example.aigc.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateAssetRequest(
        @Size(max = 120, message = "资产名称最长120字")
        String name,

        @Size(max = 5000, message = "资产描述最长5000字")
        String description,

        List<String> tags,

        @Size(max = 5000, message = "提示词草稿最长5000字")
        String promptDraft,

        Map<String, Object> metadata
) {
}
