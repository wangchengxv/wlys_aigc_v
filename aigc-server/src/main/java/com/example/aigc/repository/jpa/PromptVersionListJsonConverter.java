package com.example.aigc.repository.jpa;

import com.example.aigc.dto.PromptVersion;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter(autoApply = false)
public class PromptVersionListJsonConverter implements AttributeConverter<List<PromptVersion>, String> {
    @Override
    public String convertToDatabaseColumn(List<PromptVersion> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return JsonConverters.toJson(attribute);
    }

    @Override
    public List<PromptVersion> convertToEntityAttribute(String dbData) {
        return JsonConverters.fromJson(dbData, new TypeReference<>() {
        }, new ArrayList<>());
    }
}
