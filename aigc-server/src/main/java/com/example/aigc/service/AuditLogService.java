package com.example.aigc.service;

import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void record(
            RequestUserContext actor,
            String action,
            String entityType,
            String entityId,
            Map<String, ?> details
    ) {
        AuditLogRecord record = new AuditLogRecord();
        record.entityType = entityType;
        record.entityId = entityId;
        record.action = action;
        record.actorUserId = actor == null ? null : actor.userId();
        record.actorUserName = actor == null ? null : actor.userName();
        record.orgUnitId = actor == null ? null : actor.orgUnitId();
        record.courseId = actor == null ? null : actor.courseId();
        record.detailsJson = toJson(details);
        record.createdAt = Instant.now();
        auditLogRepository.save(record);
    }

    public List<AuditLogRecord> listRecent(String entityType, String entityId, String actorUserId) {
        return auditLogRepository.findRecent().stream()
                .filter(item -> entityType == null || entityType.isBlank() || Objects.equals(entityType, item.entityType))
                .filter(item -> entityId == null || entityId.isBlank() || Objects.equals(entityId, item.entityId))
                .filter(item -> actorUserId == null || actorUserId.isBlank() || Objects.equals(actorUserId, item.actorUserId))
                .limit(100)
                .toList();
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"audit-details-serialize-failed\"}";
        }
    }
}
