package com.example.aigc.repository;

import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;

import java.util.List;
import java.util.Optional;

public interface ScriptProjectRepository {
    ScriptProjectAggregate save(ScriptProjectAggregate aggregate);

    Optional<ScriptProjectAggregate> findById(String projectId);

    List<ScriptProjectSummary> findAll(boolean deleted);

    void delete(String projectId);
}
