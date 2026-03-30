package com.example.aigc.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PromptTemplateService {

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
