package com.example.aigc.repository.jpa;

import com.example.aigc.entity.VideoEditSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class VideoEditSegmentListJsonConverter implements AttributeConverter<List<VideoEditSegment>, String> {
    @Override
    public String convertToDatabaseColumn(List<VideoEditSegment> attribute) {
        return JsonConverters.toJson(attribute);
    }

    @Override
    public List<VideoEditSegment> convertToEntityAttribute(String dbData) {
        return JsonConverters.fromJson(dbData, new TypeReference<>() {}, new ArrayList<>());
    }
}
