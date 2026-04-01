package com.example.aigc.repository;

import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.StoredFileRecord;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FileScriptProjectRepository implements ScriptProjectRepository {

    private static final String ROOT_DIR = "script-projects";
    private static final String INDEX_FILE = "script-projects/index.json";
    private static final String PROJECT_FILE_NAME = "project.json";

    private final JsonFileStorageSupport storageSupport;

    public FileScriptProjectRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
    }

    @Override
    public synchronized ScriptProjectAggregate save(ScriptProjectAggregate aggregate) {
        Path projectFile = resolveProjectFile(aggregate.project.projectId);
        storageSupport.writeValue(projectFile, aggregate);
        List<ScriptProjectSummary> summaries = readSummaries();
        ScriptProjectSummary summary = toSummary(aggregate);
        List<ScriptProjectSummary> updated = new ArrayList<>();
        boolean replaced = false;
        for (ScriptProjectSummary item : summaries) {
            if (item.projectId != null && item.projectId.equals(summary.projectId)) {
                updated.add(summary);
                replaced = true;
            } else {
                updated.add(item);
            }
        }
        if (!replaced) {
            updated.add(summary);
        }
        updated.sort(Comparator.comparing((ScriptProjectSummary item) -> item.updatedAt, Comparator.nullsLast(Instant::compareTo)).reversed());
        storageSupport.writeValue(INDEX_FILE, updated);
        return aggregate;
    }

    @Override
    public Optional<ScriptProjectAggregate> findById(String projectId) {
        Path projectFile = resolveProjectFile(projectId);
        ScriptProjectAggregate aggregate = storageSupport.readValue(
                projectFile,
                new TypeReference<>() {
                },
                ScriptProjectAggregate::new
        );
        if (aggregate.project == null || aggregate.project.projectId == null || aggregate.project.projectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(aggregate);
    }

    @Override
    public List<ScriptProjectSummary> findAll(boolean deleted) {
        return readSummaries().stream()
                .filter(item -> deleted ? item.deletedAt != null : item.deletedAt == null)
                .toList();
    }

    @Override
    public synchronized void delete(String projectId) {
        ScriptProjectAggregate aggregate = findById(projectId).orElse(null);
        if (aggregate == null || aggregate.project == null) {
            return;
        }
        Instant now = Instant.now();
        aggregate.project.deletedAt = now;
        aggregate.project.updatedAt = now;
        save(aggregate);
    }

    private List<ScriptProjectSummary> readSummaries() {
        return storageSupport.readValue(
                INDEX_FILE,
                new TypeReference<>() {
                },
                ArrayList::new
        );
    }

    private ScriptProjectSummary toSummary(ScriptProjectAggregate aggregate) {
        ScriptProjectSummary summary = new ScriptProjectSummary();
        summary.projectId = aggregate.project.projectId;
        summary.name = aggregate.project.name;
        summary.status = aggregate.project.status;
        summary.scriptSummary = aggregate.project.scriptSummary;
        summary.visualStyle = aggregate.project.visualStyle;
        summary.aspectRatio = aggregate.project.aspectRatio;
        summary.targetDuration = aggregate.project.targetDuration;
        summary.coverFileId = resolveCoverFileId(aggregate);
        summary.assetCount = aggregate.assets.size();
        summary.keyframeCount = aggregate.keyframes.size();
        summary.videoTaskCount = aggregate.videoTasks.size();
        summary.createdAt = aggregate.project.createdAt;
        summary.updatedAt = aggregate.project.updatedAt;
        summary.deletedAt = aggregate.project.deletedAt;
        return summary;
    }

    private String resolveCoverFileId(ScriptProjectAggregate aggregate) {
        for (KeyframeRecord keyframe : aggregate.keyframes) {
            if (keyframe.selected && keyframe.imageFileId != null && !keyframe.imageFileId.isBlank()) {
                return keyframe.imageFileId;
            }
        }
        for (StoredFileRecord file : aggregate.files) {
            if (file.mediaType != null && file.mediaType.startsWith("image/")) {
                return file.fileId;
            }
        }
        for (StoredFileRecord file : aggregate.files) {
            if (file.mediaType != null && file.mediaType.startsWith("video/")) {
                return file.fileId;
            }
        }
        return null;
    }

    private Path resolveProjectDir(String projectId) {
        return storageSupport.resolve(ROOT_DIR + "/" + projectId);
    }

    private Path resolveProjectFile(String projectId) {
        return resolveProjectDir(projectId).resolve(PROJECT_FILE_NAME);
    }
}
