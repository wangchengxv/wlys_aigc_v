package com.example.aigc.service;

import com.example.aigc.model.ModelConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
            String modelType = normalize(String.valueOf(metadata.getOrDefault("modelType", "")));
            if (!modelType.isBlank()) {
                switch (modelType) {
                    case "chat" -> capabilities.add("text");
                    case "image" -> capabilities.add("image");
                    case "video" -> capabilities.add("video");
                    case "embedding" -> capabilities.add("embedding");
                    case "rerank" -> capabilities.add("rerank");
                    default -> {
                    }
                }
            }
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
        if (isViduReference2ImageFamilyModel(normalizedModel)) {
            capabilities.add("image");
        }
        if (isViduWorkspaceModel(normalizedModel)) {
            capabilities.add("video");
        }
        if (isKlingImageModel(normalizedProvider, normalizedModel)) {
            capabilities.add("image");
        }
        if (isKlingVideoModel(normalizedProvider, normalizedModel)) {
            capabilities.add("video");
        }
        if (isKlingModel(normalizedModel) && capabilities.isEmpty()) {
            return List.of();
        }
        if (normalizedModel.contains("tts")
                || normalizedModel.contains("speech")
                || normalizedModel.contains("voice")
                || normalizedModel.contains("audio")) {
            capabilities.add("tts");
        }
        if (capabilities.isEmpty() && !"ark".equals(normalizedProvider)) {
            capabilities.add("text");
        }
        if (capabilities.isEmpty() && "ark".equals(normalizedProvider)) {
            capabilities.add("image");
        }
        return new ArrayList<>(capabilities);
    }

    public Map<String, Object> normalizeModelMetadata(Map<String, Object> metadata, String provider, String modelName) {
        Map<String, Object> source = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        Map<String, Object> merged = mergeCapabilities(source, resolveCapabilities(source, provider, modelName));
        return normalizeViduModelMetadata(merged, provider, modelName);
    }

    private Map<String, Object> normalizeViduModelMetadata(Map<String, Object> metadata, String provider, String modelName) {
        if (!"vidu".equals(normalize(provider)) && !isViduWorkspaceModel(normalize(modelName))) {
            return metadata;
        }
        String family = detectViduModelFamily(modelName);
        if (family.isBlank()) {
            family = "q2";
        }
        ViduMatrix matrix = defaultViduMatrix(family);
        Map<String, Object> out = new HashMap<>(metadata);
        out.put("viduFamily", family);
        out.put("viduDurations", ensureIntList(out.get("viduDurations"), matrix.durations()));
        out.put("viduResolutions", ensureStringList(out.get("viduResolutions"), matrix.resolutions()));
        if (!out.containsKey("viduAudioSupported")) {
            out.put("viduAudioSupported", matrix.audioSupported());
        }
        return out;
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

    private boolean isViduWorkspaceModel(String modelName) {
        String normalized = normalize(modelName);
        return normalized.startsWith("viduq")
                || normalized.startsWith("vidu")
                || normalized.startsWith("image-vidu-");
    }

    private boolean isViduReference2ImageFamilyModel(String modelName) {
        String normalized = normalize(modelName);
        return normalized.startsWith("image-vidu-")
                || normalized.startsWith("image-viduq3-")
                || normalized.startsWith("viduq2")
                || normalized.startsWith("viduq1");
    }

    private boolean isKlingModel(String modelName) {
        String normalized = normalize(modelName);
        return normalized.startsWith("kling-");
    }

    private boolean isKlingImageModel(String provider, String modelName) {
        String normalizedProvider = normalize(provider);
        String normalizedModel = normalize(modelName);
        if (!"onelinkai".equals(normalizedProvider)) {
            return false;
        }
        return "video-kling-v3".equals(normalizedModel) || "video-kling-v3".equals(normalizedModel);
    }

    private boolean isKlingVideoModel(String provider, String modelName) {
        String normalizedProvider = normalize(provider);
        String normalizedModel = normalize(modelName);
        if ("kling".equals(normalizedProvider) || "kling_onelink".equals(normalizedProvider)) {
            return isKlingModel(normalizedModel);
        }
        if (!"onelinkai".equals(normalizedProvider)) {
            return false;
        }
        return "kling-v1".equals(normalizedModel)
                || "kling-v1-6".equals(normalizedModel)
                || "video-kling-v3-6".equals(normalizedModel);
    }

    private String detectViduModelFamily(String modelName) {
        String normalized = normalize(modelName);
        if (normalized.contains("q3")) {
            return "q3";
        }
        if (normalized.contains("q2")) {
            return "q2";
        }
        if (normalized.contains("q1")) {
            return "q1";
        }
        if (normalized.contains("2.0") || normalized.contains("v2.0") || normalized.contains("vidu2")) {
            return "2.0";
        }
        return "";
    }

    private ViduMatrix defaultViduMatrix(String family) {
        return switch (normalize(family)) {
            case "q1" -> new ViduMatrix(List.of(5), List.of("1080p"), false);
            case "q3" -> new ViduMatrix(
                    List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
                    List.of("540p", "720p", "1080p"),
                    true
            );
            case "2.0" -> new ViduMatrix(
                    List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                    List.of("540p", "720p", "1080p"),
                    false
            );
            default -> new ViduMatrix(
                    List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                    List.of("540p", "720p", "1080p"),
                    false
            );
        };
    }

    private List<Integer> ensureIntList(Object raw, List<Integer> defaults) {
        List<Integer> values = toIntList(raw);
        if (values.isEmpty()) {
            values = new ArrayList<>(defaults);
        }
        Collections.sort(values);
        return values;
    }

    private List<String> ensureStringList(Object raw, List<String> defaults) {
        List<String> values = toStringList(raw);
        if (values.isEmpty()) {
            values = new ArrayList<>(defaults);
        }
        return values;
    }

    private List<Integer> toIntList(Object raw) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                Integer parsed = parseInteger(item);
                if (parsed != null) {
                    values.add(parsed);
                }
            }
        } else if (raw instanceof String text) {
            for (String item : text.split(",")) {
                Integer parsed = parseInteger(item);
                if (parsed != null) {
                    values.add(parsed);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private Integer parseInteger(Object raw) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            int rounded = number.intValue();
            return value == rounded ? rounded : null;
        }
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> toStringList(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                String value = item == null ? "" : String.valueOf(item).trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        } else if (raw instanceof String text) {
            Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .forEach(values::add);
        }
        return new ArrayList<>(values);
    }

    private record ViduMatrix(List<Integer> durations, List<String> resolutions, boolean audioSupported) {
    }
}
