package com.example.aigc.repository.jpa;

import com.example.aigc.model.TimeScheduleSlot;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class TimeScheduleSlotListJsonConverter implements AttributeConverter<List<TimeScheduleSlot>, String> {
    @Override
    public String convertToDatabaseColumn(List<TimeScheduleSlot> attribute) {
        return JsonConverters.toJson(attribute);
    }

    @Override
    public List<TimeScheduleSlot> convertToEntityAttribute(String dbData) {
        return JsonConverters.fromJson(dbData, new TypeReference<>() {}, new ArrayList<>());
    }
}
