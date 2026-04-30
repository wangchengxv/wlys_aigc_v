package com.example.aigc.repository;

import com.example.aigc.entity.CanvasGraph;

import java.util.List;
import java.util.Optional;

public interface CanvasGraphRepository {
    CanvasGraph save(CanvasGraph graph);

    Optional<CanvasGraph> findByIdAndOwnerId(String id, String ownerId);

    List<CanvasGraph> findAllByOwnerId(String ownerId);
    List<CanvasGraph> findAllByOwnerIdAndProjectId(String ownerId, String projectId);
    List<CanvasGraph> findAllByOwnerIdAndProjectIdIsNull(String ownerId);

    void deleteByIdAndOwnerId(String id, String ownerId);
}
