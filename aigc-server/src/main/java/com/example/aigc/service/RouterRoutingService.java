package com.example.aigc.service;

import com.example.aigc.dto.RouterRoutingResponse;
import com.example.aigc.dto.RouterRoutingUpdateRequest;
import com.example.aigc.dto.TimeScheduleSlotDto;
import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.model.RoutingConfig;
import com.example.aigc.model.TimeScheduleSlot;
import com.example.aigc.repository.ConnectionConfigRepository;
import com.example.aigc.repository.RoutingConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RouterRoutingService {

    private final RoutingConfigRepository routingConfigRepository;
    private final ConnectionConfigRepository connectionConfigRepository;

    public RouterRoutingService(RoutingConfigRepository routingConfigRepository, ConnectionConfigRepository connectionConfigRepository) {
        this.routingConfigRepository = routingConfigRepository;
        this.connectionConfigRepository = connectionConfigRepository;
    }

    public RoutingConfig getConfig() {
        return routingConfigRepository.get();
    }

    public RouterRoutingResponse getResponse() {
        return toResponse(getConfig());
    }

    public RouterRoutingResponse update(RouterRoutingUpdateRequest request) {
        RoutingConfig config = getConfig();
        if (request.strategy() != null && !request.strategy().isBlank()) {
            config.setStrategy(request.strategy().trim());
        }
        if (request.priorityConnectionIds() != null) {
            config.setPriorityConnectionIds(request.priorityConnectionIds());
        }
        if (request.failoverEnabled() != null) {
            config.setFailoverEnabled(request.failoverEnabled());
        }
        if (request.failoverTimeoutSeconds() != null) {
            config.setFailoverTimeoutSeconds(Math.max(1, request.failoverTimeoutSeconds()));
        }
        if (request.timeSchedule() != null) {
            List<TimeScheduleSlot> slots = request.timeSchedule().stream()
                    .map(slot -> new TimeScheduleSlot(slot.start(), slot.end(), slot.connectionId()))
                    .toList();
            config.setTimeSchedule(slots);
        }
        routingConfigRepository.save(config);
        return toResponse(config);
    }

    public void appendConnectionIfAbsent(String connectionId) {
        RoutingConfig config = getConfig();
        List<String> ids = new ArrayList<>(config.getPriorityConnectionIds());
        if (!ids.contains(connectionId)) {
            ids.add(connectionId);
            config.setPriorityConnectionIds(ids);
            routingConfigRepository.save(config);
        }
    }

    public void removeConnection(String connectionId) {
        RoutingConfig config = getConfig();
        List<String> ids = config.getPriorityConnectionIds().stream()
                .filter(id -> !connectionId.equals(id))
                .toList();
        List<TimeScheduleSlot> slots = config.getTimeSchedule().stream()
                .filter(slot -> !connectionId.equals(slot.getConnectionId()))
                .toList();
        config.setPriorityConnectionIds(ids);
        config.setTimeSchedule(slots);
        routingConfigRepository.save(config);
    }

    public List<ConnectionConfig> resolveOrderedConnections(boolean includeFailoverCandidates) {
        List<ConnectionConfig> enabledConnections = connectionConfigRepository.findAll().stream()
                .filter(ConnectionConfig::isEnabled)
                .toList();
        if (enabledConnections.isEmpty()) {
            return List.of();
        }

        Map<String, ConnectionConfig> byId = enabledConnections.stream()
                .collect(Collectors.toMap(ConnectionConfig::getId, connection -> connection));

        RoutingConfig config = getConfig();
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        if ("time_schedule".equalsIgnoreCase(config.getStrategy())) {
            String currentId = matchCurrentSlot(config.getTimeSchedule());
            if (currentId != null) {
                orderedIds.add(currentId);
                if (includeFailoverCandidates) {
                    orderedIds.addAll(config.getPriorityConnectionIds());
                }
            }
        }
        if (orderedIds.isEmpty()) {
            orderedIds.addAll(config.getPriorityConnectionIds());
        }
        if (orderedIds.isEmpty()) {
            orderedIds.addAll(enabledConnections.stream().map(ConnectionConfig::getId).toList());
        }

        List<ConnectionConfig> ordered = orderedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (ordered.isEmpty()) {
            ordered = enabledConnections;
        }
        if (!includeFailoverCandidates || !config.isFailoverEnabled()) {
            return ordered.isEmpty() ? List.of() : List.of(ordered.get(0));
        }
        return ordered;
    }

    public int timeoutSeconds() {
        return Math.max(1, getConfig().getFailoverTimeoutSeconds());
    }

    public RouterRoutingResponse toResponse(RoutingConfig config) {
        List<TimeScheduleSlotDto> timeSchedule = config.getTimeSchedule().stream()
                .map(slot -> new TimeScheduleSlotDto(slot.getStart(), slot.getEnd(), slot.getConnectionId()))
                .toList();
        return new RouterRoutingResponse(
                config.getStrategy(),
                config.getPriorityConnectionIds(),
                config.isFailoverEnabled(),
                config.getFailoverTimeoutSeconds(),
                timeSchedule
        );
    }

    private String matchCurrentSlot(List<TimeScheduleSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return null;
        }
        LocalTime now = LocalTime.now();
        for (TimeScheduleSlot slot : slots) {
            if (isInSlot(now, slot.getStart(), slot.getEnd())) {
                return slot.getConnectionId();
            }
        }
        return null;
    }

    private boolean isInSlot(LocalTime now, String startText, String endText) {
        LocalTime start = LocalTime.parse(startText);
        LocalTime end = LocalTime.parse(endText);
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }
}
