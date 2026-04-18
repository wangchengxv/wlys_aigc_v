package com.example.aigc.repository.jpa;

import com.example.aigc.entity.FinalCompositionInputSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class FinalCompositionSegmentListJsonConverter implements AttributeConverter<List<FinalCompositionInputSegment>, String> {
    @Override
    public String convertToDatabaseColumn(List<FinalCompositionInputSegment> attribute) {
        return JsonConverters.toJson(attribute);
    }

    @Override
    public List<FinalCompositionInputSegment> convertToEntityAttribute(String dbData) {
        return JsonConverters.fromJson(dbData, new TypeReference<>() {}, new ArrayList<>());
    }
}
