package com.example.aigc.repository.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return JsonConverters.toJson(attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return JsonConverters.fromJson(dbData, new TypeReference<>() {}, new ArrayList<>());
    }
}
