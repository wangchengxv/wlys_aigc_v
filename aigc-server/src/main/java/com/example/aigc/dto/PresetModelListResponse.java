package com.example.aigc.dto;

import com.example.aigc.model.PresetModel;

import java.util.List;

public record PresetModelListResponse(
        List<PresetModelDto> models,
        List<String> providers
) {
    public record PresetModelDto(
            String id,
            String provider,
            String modelName,
            String baseUrl,
            String displayName,
            java.util.List<String> capabilities
    ) {
        public static PresetModelDto from(PresetModel m) {
            return new PresetModelDto(m.getId(), m.getProvider(), m.getModelName(), m.getBaseUrl(), m.getDisplayName(), m.getCapabilities());
        }
    }
}
