package com.example.aigc.service;

import com.example.aigc.dto.CanvasGraphDto;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.dto.SaveCanvasRequest;
import com.example.aigc.entity.CanvasGraph;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.CanvasGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CanvasGraphService {
    private final CanvasGraphRepository canvasGraphRepository;
    private final ObjectMapper objectMapper;
    private static final String UNBOUND_PROJECT_KEY = "__unbound__";

    public CanvasGraphService(CanvasGraphRepository canvasGraphRepository, ObjectMapper objectMapper) {
        this.canvasGraphRepository = canvasGraphRepository;
        this.objectMapper = objectMapper;
    }

    public PagedResult<CanvasGraphDto> list(String ownerId, String projectId, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, Math.min(pageSize, 100));
        List<CanvasGraph> all;
        if (projectId == null) {
            all = canvasGraphRepository.findAllByOwnerId(ownerId);
        } else if (projectId.isBlank() || UNBOUND_PROJECT_KEY.equals(projectId.trim())) {
            all = canvasGraphRepository.findAllByOwnerIdAndProjectIdIsNull(ownerId);
        } else {
            all = canvasGraphRepository.findAllByOwnerIdAndProjectId(ownerId, projectId.trim());
        }
        int from = Math.min((p - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        List<CanvasGraphDto> list = all.subList(from, to).stream().map(this::toDto).toList();
        return new PagedResult<>(list, all.size());
    }

    public CanvasGraphDto get(String ownerId, String id) {
        CanvasGraph graph = canvasGraphRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new BizException(404, "画布不存在"));
        return toDto(graph);
    }

    public CanvasGraphDto createOrOverwrite(String ownerId, SaveCanvasRequest request) {
        String id = request.id() == null || request.id().isBlank() ? nextId() : request.id().trim();
        CanvasGraph existing = canvasGraphRepository.findByIdAndOwnerId(id, ownerId).orElse(null);
        CanvasGraph graph = existing == null ? new CanvasGraph() : existing;
        Instant now = Instant.now();
        if (graph.createdAt == null) {
            graph.createdAt = now;
        }
        graph.updatedAt = now;
        graph.id = id;
        graph.ownerId = ownerId;
        graph.projectId = blankToNull(request.projectId());
        graph.title = blankToNull(request.title());
        graph.graphJson = writeJson(request.graph());
        graph.viewportJson = request.viewport() == null ? null : writeJson(request.viewport());
        return toDto(canvasGraphRepository.save(graph));
    }

    public CanvasGraphDto update(String ownerId, String id, SaveCanvasRequest request) {
        CanvasGraph graph = canvasGraphRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new BizException(404, "画布不存在"));
        graph.updatedAt = Instant.now();
        graph.projectId = blankToNull(request.projectId());
        graph.title = blankToNull(request.title());
        graph.graphJson = writeJson(request.graph());
        graph.viewportJson = request.viewport() == null ? null : writeJson(request.viewport());
        return toDto(canvasGraphRepository.save(graph));
    }

    public void delete(String ownerId, String id) {
        canvasGraphRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private CanvasGraphDto toDto(CanvasGraph entity) {
        return new CanvasGraphDto(
                entity.id,
                entity.projectId,
                entity.title,
                readJson(entity.graphJson),
                readJson(entity.viewportJson),
                entity.createdAt,
                entity.updatedAt
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BizException(400, "画布 JSON 序列化失败");
        }
    }

    private Object readJson(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return objectMapper.readValue(text, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nextId() {
        return "canvas-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
