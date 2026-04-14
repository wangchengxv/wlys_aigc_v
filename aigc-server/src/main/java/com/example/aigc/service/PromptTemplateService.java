package com.example.aigc.service;

import com.example.aigc.entity.ScriptProject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PromptTemplateService {

    private final ObjectMapper objectMapper;

    public PromptTemplateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String load(String classpathLocation, String fallback) {
        try (InputStream inputStream = new ClassPathResource(classpathLocation).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return fallback;
        }
    }

    public String render(String classpathLocation, Map<String, ?> variables, String fallback) {
        return renderTemplate(load(classpathLocation, fallback), variables);
    }

    /**
     * 若项目在 DB 中对 classpath 路径有覆盖，则使用覆盖正文作为模板，否则读 classpath 默认文件。
     */
    public String renderForProject(ScriptProject project, String classpathLocation, Map<String, ?> variables, String fallback) {
        String body = resolveTemplateBody(project, classpathLocation, fallback);
        return renderTemplate(body, variables);
    }

    public String resolveTemplateBody(ScriptProject project, String classpathLocation, String fallback) {
        String override = findOverride(project, classpathLocation);
        if (override != null) {
            return override;
        }
        return load(classpathLocation, fallback);
    }

    public String findOverride(ScriptProject project, String classpathLocation) {
        if (project == null || project.promptTemplateOverrides == null || project.promptTemplateOverrides.isBlank()) {
            return null;
        }
        try {
            Map<String, String> map = objectMapper.readValue(
                    project.promptTemplateOverrides,
                    new TypeReference<Map<String, String>>() {
                    }
            );
            if (map == null) {
                return null;
            }
            String v = map.get(classpathLocation);
            return v != null ? v : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 保存前清洗：与 classpath 默认正文相同的条目删除；null/空值删除。
     */
    public String sanitizePromptTemplateOverridesJson(String json, String classpathLocation, String defaultBody) {
        Map<String, String> map = parseOverrideMap(json);
        if (map.isEmpty()) {
            return null;
        }
        String def = defaultBody == null ? "" : defaultBody;
        map.entrySet().removeIf(e ->
                e.getKey() == null
                        || e.getValue() == null
                        || Objects.equals(normalizeTemplate(e.getValue()), normalizeTemplate(def))
        );
        if (map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return null;
        }
    }

    public Map<String, String> parseOverrideMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    public String mergeSanitizeAllOverrides(String existingJson, Map<String, String> incoming) {
        Map<String, String> base = parseOverrideMap(existingJson);
        if (incoming != null) {
            for (Map.Entry<String, String> e : incoming.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) {
                    continue;
                }
                String path = e.getKey().trim();
                String val = e.getValue();
                if (val == null || val.isBlank()) {
                    base.remove(path);
                    continue;
                }
                String def = load(path, "");
                if (normalizeTemplate(val).equals(normalizeTemplate(def))) {
                    base.remove(path);
                } else {
                    base.put(path, val);
                }
            }
        }
        if (base.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(base);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String normalizeTemplate(String s) {
        return s == null ? "" : s.trim();
    }

    public String renderTemplate(String template, Map<String, ?> variables) {
        String result = template == null ? "" : template;
        if (variables == null || variables.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace(key, value);
        }
        return result;
    }
}
