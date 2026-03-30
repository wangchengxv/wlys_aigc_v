package com.example.aigc.service;

import com.example.aigc.model.ModelConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ModelCapabilityService {

    public boolean supports(ModelConfig config, String capability) {
        if (config == null || capability == null || capability.isBlank()) {
            return false;
        }
        return resolveCapabilities(config).contains(normalize(capability));
    }

    public List<String> resolveCapabilities(ModelConfig config) {
        if (config == null) {
            return List.of();
        }
        return resolveCapabilities(config.getMetadata(), config.getProvider(), config.getModelName());
    }

    public List<String> resolveCapabilities(Map<String, Object> metadata, String provider, String modelName) {
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        if (metadata != null) {
            Object raw = metadata.get("capabilities");
            if (raw instanceof Collection<?> collection) {
                for (Object item : collection) {
                    String normalized = normalize(String.valueOf(item));
                    if (!normalized.isBlank()) {
                        capabilities.add(normalized);
                    }
                }
            } else if (raw instanceof String rawString) {
                for (String item : rawString.split(",")) {
                    String normalized = normalize(item);
                    if (!normalized.isBlank()) {
                        capabilities.add(normalized);
                    }
                }
            }
        }
        if (!capabilities.isEmpty()) {
            return new ArrayList<>(capabilities);
        }
        String normalizedModel = normalize(modelName);
        String normalizedProvider = normalize(provider);
        if (normalizedModel.contains("seedream")
                || normalizedModel.contains("image")
                || normalizedModel.contains("flux")
                || normalizedModel.contains("wanx")
                || normalizedModel.contains("dall")
                || normalizedModel.contains("sdxl")) {
            capabilities.add("image");
        }
        if (normalizedModel.contains("seedance")
                || normalizedModel.contains("video")
                || normalizedModel.contains("veo")
                || normalizedModel.contains("sora")) {
            capabilities.add("video");
        }
        if (capabilities.isEmpty() && !"ark".equals(normalizedProvider)) {
            capabilities.add("text");
        }
        if (capabilities.isEmpty() && "ark".equals(normalizedProvider)) {
            capabilities.add("image");
        }
        return new ArrayList<>(capabilities);
    }

    public Map<String, Object> mergeCapabilities(Map<String, Object> metadata, List<String> capabilities) {
        Map<String, Object> merged = metadata == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(metadata);
        List<String> normalized = new ArrayList<>();
        if (capabilities != null) {
            for (String capability : capabilities) {
                String value = normalize(capability);
                if (!value.isBlank() && !normalized.contains(value)) {
                    normalized.add(value);
                }
            }
        }
        merged.put("capabilities", normalized);
        return merged;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
