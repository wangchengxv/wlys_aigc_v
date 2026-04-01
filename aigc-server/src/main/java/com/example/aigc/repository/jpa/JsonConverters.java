package com.example.aigc.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonConverters {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonConverters() {
    }

    static <T> String toJson(T value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    static <T> T fromJson(String value, TypeReference<T> typeReference, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return OBJECT_MAPPER.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            return defaultValue;
        }
    }
}
