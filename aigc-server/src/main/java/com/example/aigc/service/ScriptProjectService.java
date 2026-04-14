package com.example.aigc.service;

import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.ScriptProjectCreateRequest;
import com.example.aigc.dto.UpdateScriptRequest;
import com.example.aigc.dto.PromptTemplateOverridesUpdateRequest;
import com.example.aigc.dto.WorkflowModelSettingsResponse;
import com.example.aigc.dto.WorkflowModelSettingsUpdateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.aigc.entity.ScriptDocumentVersion;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.ScriptRevision;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.DocumentVersionType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.RevisionKind;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.repository.ScriptProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ScriptProjectService {

    private final ScriptProjectRepository scriptProjectRepository;
    private final LocalAssetFileService localAssetFileService;
    private final ScriptDocxService scriptDocxService;
    private final VideoStylePresetRegistry videoStylePresetRegistry;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    private static final TypeReference<Map<String, String>> STR_MAP_TYPE = new TypeReference<>() {};

    public ScriptProjectService(
            ScriptProjectRepository scriptProjectRepository,
            LocalAssetFileService localAssetFileService,
            ScriptDocxService scriptDocxService,
            VideoStylePresetRegistry videoStylePresetRegistry,
            ObjectMapper objectMapper,
            PromptTemplateService promptTemplateService
    ) {
        this.scriptProjectRepository = scriptProjectRepository;
        this.localAssetFileService = localAssetFileService;
        this.scriptDocxService = scriptDocxService;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
    }

    public ScriptProjectAggregate create(ScriptProjectCreateRequest request) {
        return createInternal(
                request.name(),
                request.sourceText(),
                null,
                null,
                request.visualStyle(),
                request.aspectRatio(),
                request.targetDuration(),
                request.language(),
                request.explicitTextModel(),
                request.explicitImageModel(),
                request.explicitVideoModel(),
                "text"
        );
    }

    public ScriptProjectAggregate createFromUpload(
            String name,
            MultipartFile file,
            String visualStyle,
            String aspectRatio,
            Integer targetDuration,
            String language,
            String explicitTextModel,
            String explicitImageModel,
            String explicitVideoModel
    ) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请上传剧本文件");
        }
        try {
            byte[] bytes = file.getBytes();
            String originalText = scriptDocxService.extractText(file.getOriginalFilename(), bytes);
            return createInternal(
                    name,
                    originalText,
                    file,
                    bytes,
                    visualStyle,
                    aspectRatio,
                    targetDuration,
                    language,
                    explicitTextModel,
                    explicitImageModel,
                    explicitVideoModel,
                    "upload"
            );
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(400, "上传剧本文件失败");
        }
    }

    public List<ScriptProjectSummary> list(boolean deleted) {
        return scriptProjectRepository.findAll(deleted).stream()
                .sorted(Comparator.comparing((ScriptProjectSummary item) -> item.updatedAt, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
    }

    public ScriptProjectAggregate require(String projectId) {
        return requireInternal(projectId, false);
    }

    public ScriptProjectAggregate restore(String projectId) {
        ScriptProjectAggregate aggregate = requireInternal(projectId, true);
        if (aggregate.project.deletedAt == null) {
            return aggregate;
        }
        aggregate.project.deletedAt = null;
        save(aggregate);
        return aggregate;
    }

    private ScriptProjectAggregate requireInternal(String projectId, boolean includeDeleted) {
        ScriptProjectAggregate aggregate = scriptProjectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(404, "剧本工程不存在"));
        if (!includeDeleted && aggregate.project != null && aggregate.project.deletedAt != null) {
            throw new BizException(404, "剧本工程不存在或已删除");
        }
        if (aggregate.project != null) {
            aggregate.project.visualStyle = videoStylePresetRegistry.normalizeStyleKeyForRead(aggregate.project.visualStyle);
        }
        if (aggregate.revisions == null) {
            aggregate.revisions = new ArrayList<>();
        }
        return aggregate;
    }

    public ScriptProjectAggregate save(ScriptProjectAggregate aggregate) {
        aggregate.project.updatedAt = Instant.now();
        return scriptProjectRepository.save(aggregate);
    }

    public void delete(String projectId) {
        require(projectId);
        scriptProjectRepository.delete(projectId);
    }

    public ScriptDocumentPayload getScriptPayload(String projectId) {
        ScriptProjectAggregate aggregate = require(projectId);
        return buildScriptPayload(aggregate);
    }

    public ScriptDocumentPayload updateScript(String projectId, UpdateScriptRequest request) {
        return updateScript(projectId, request, true);
    }

    public ScriptDocumentPayload updateScript(String projectId, UpdateScriptRequest request, boolean snapshotBeforeOverwrite) {
        ScriptProjectAggregate aggregate = require(projectId);
        boolean willWriteMarkdown = request.refinedMarkdown() != null && !request.refinedMarkdown().isBlank();
        boolean willWriteStructured = request.structuredScript() != null && !request.structuredScript().isEmpty();
        if (snapshotBeforeOverwrite && (willWriteMarkdown || willWriteStructured) && hasRefinedSnapshot(aggregate)) {
            snapshotBeforeRefinedOverwrite(aggregate, "before-update", RevisionKind.USER_EDIT);
        }
        if (willWriteMarkdown) {
            StoredFileRecord markdownFile = localAssetFileService.storeText(
                    projectId,
                    "documents/refined-script.md",
                    "text/markdown; charset=UTF-8",
                    request.refinedMarkdown()
            );
            upsertFile(aggregate, markdownFile);
            replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_MARKDOWN, "md", markdownFile);
            aggregate.project.refinedScriptFileId = markdownFile.fileId;
            aggregate.project.scriptSummary = summarizeText(request.refinedMarkdown());
        }
        if (willWriteStructured) {
            StoredFileRecord jsonFile = localAssetFileService.storeJson(projectId, "documents/refined-script.json", request.structuredScript());
            upsertFile(aggregate, jsonFile);
            replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_JSON, "json", jsonFile);
            aggregate.project.refinedScriptJsonFileId = jsonFile.fileId;
            if (aggregate.project.scriptSummary == null || aggregate.project.scriptSummary.isBlank()) {
                Object summary = request.structuredScript().get("summary");
                if (summary != null) {
                    aggregate.project.scriptSummary = String.valueOf(summary);
                }
            }
        }
        aggregate.project.status = ProjectStatus.SCRIPT_READY;
        save(aggregate);
        return buildScriptPayload(aggregate);
    }

    public void snapshotBeforeRefineIfNeeded(String projectId) {
        ScriptProjectAggregate aggregate = require(projectId);
        if (hasRefinedSnapshot(aggregate)) {
            snapshotBeforeRefinedOverwrite(aggregate, "before-refine", RevisionKind.REFINE);
            save(aggregate);
        }
    }

    public void snapshotBeforeOptimize(String projectId, String label, RevisionKind kind) {
        ScriptProjectAggregate aggregate = require(projectId);
        if (hasRefinedSnapshot(aggregate)) {
            snapshotBeforeRefinedOverwrite(aggregate, label, kind);
            save(aggregate);
        }
    }

    public List<ScriptRevision> listRevisions(String projectId) {
        ScriptProjectAggregate aggregate = require(projectId);
        if (aggregate.revisions == null || aggregate.revisions.isEmpty()) {
            return List.of();
        }
        return aggregate.revisions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt((ScriptRevision r) -> r.revisionIndex).reversed())
                .toList();
    }

    public ScriptDocumentPayload restoreRevision(String projectId, String revisionId) {
        ScriptProjectAggregate aggregate = require(projectId);
        if (aggregate.revisions == null || aggregate.revisions.isEmpty()) {
            throw new BizException(404, "修订不存在");
        }
        ScriptRevision target = aggregate.revisions.stream()
                .filter(Objects::nonNull)
                .filter(item -> revisionId != null && revisionId.equals(item.revisionId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "修订不存在"));
        if (target.refinedMarkdownFileId == null && target.refinedJsonFileId == null) {
            throw new BizException(400, "该修订没有可恢复的剧本内容");
        }
        if (hasRefinedSnapshot(aggregate)) {
            snapshotBeforeRefinedOverwrite(aggregate, "before-restore", RevisionKind.RESTORE);
            // snapshotBeforeRefinedOverwrite 会修改 aggregate.revisions/files，但需要先持久化，
            // 否则 updateScript 内部会重新 require() 聚合，导致 RESTORE 修订丢失。
            save(aggregate);
        }
        String md = target.refinedMarkdownFileId != null
                ? readText(findFile(aggregate, target.refinedMarkdownFileId))
                : "";
        Map<String, Object> json = target.refinedJsonFileId != null
                ? readJson(findFile(aggregate, target.refinedJsonFileId))
                : new LinkedHashMap<>();
        if (md.isBlank() && json.isEmpty()) {
            throw new BizException(400, "修订内容为空");
        }
        return updateScript(projectId, new UpdateScriptRequest(md.isBlank() ? null : md, json.isEmpty() ? null : json), false);
    }

    public ScriptProjectAggregate importScript(String projectId, MultipartFile file, String replaceName) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请上传剧本文件");
        }
        try {
            byte[] bytes = file.getBytes();
            String text = scriptDocxService.extractText(file.getOriginalFilename(), bytes);
            if (text == null || text.isBlank()) {
                throw new BizException(400, "解析后剧本文本为空");
            }
            ScriptProjectAggregate aggregate = require(projectId);
            if (replaceName != null && !replaceName.isBlank()) {
                aggregate.project.name = replaceName.trim();
            }
            StoredFileRecord originalScript = localAssetFileService.storeText(
                    aggregate.project.projectId,
                    "documents/original-script.txt",
                    "text/plain; charset=UTF-8",
                    text
            );
            upsertFile(aggregate, originalScript);
            replaceDocumentVersion(aggregate, DocumentVersionType.ORIGINAL, "txt", originalScript);
            aggregate.project.originalScriptFileId = originalScript.fileId;
            aggregate.project.scriptSummary = summarizeText(text);
            aggregate.project.sourceType = "import";
            aggregate.project.status = ProjectStatus.DRAFT;
            return save(aggregate);
        } catch (BizException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BizException(400, "读取上传文件失败");
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(400, "导入剧本失败");
        }
    }

    private boolean hasRefinedSnapshot(ScriptProjectAggregate aggregate) {
        boolean md = aggregate.project.refinedScriptFileId != null && !aggregate.project.refinedScriptFileId.isBlank();
        boolean json = aggregate.project.refinedScriptJsonFileId != null && !aggregate.project.refinedScriptJsonFileId.isBlank();
        return md || json;
    }

    private void snapshotBeforeRefinedOverwrite(ScriptProjectAggregate aggregate, String label, RevisionKind kind) {
        String mdId = aggregate.project.refinedScriptFileId;
        String jsonId = aggregate.project.refinedScriptJsonFileId;
        if ((mdId == null || mdId.isBlank()) && (jsonId == null || jsonId.isBlank())) {
            return;
        }
        String revisionId = nextId("rev");
        String projectId = aggregate.project.projectId;
        StoredFileRecord mdRec = null;
        StoredFileRecord jsonRec = null;
        if (mdId != null && !mdId.isBlank()) {
            String text = readText(findFile(aggregate, mdId));
            mdRec = localAssetFileService.storeText(
                    projectId,
                    "documents/revisions/" + revisionId + ".md",
                    "text/markdown; charset=UTF-8",
                    text
            );
            upsertFile(aggregate, mdRec);
        }
        if (jsonId != null && !jsonId.isBlank()) {
            Map<String, Object> json = readJson(findFile(aggregate, jsonId));
            jsonRec = localAssetFileService.storeJson(projectId, "documents/revisions/" + revisionId + ".json", json);
            upsertFile(aggregate, jsonRec);
        }
        ScriptRevision revision = new ScriptRevision();
        revision.revisionId = revisionId;
        revision.revisionIndex = aggregate.revisions.size() + 1;
        revision.label = label;
        revision.kind = kind;
        revision.createdAt = Instant.now();
        revision.refinedMarkdownFileId = mdRec != null ? mdRec.fileId : null;
        revision.refinedJsonFileId = jsonRec != null ? jsonRec.fileId : null;
        aggregate.revisions.add(revision);
    }

    public ScriptDocumentPayload buildScriptPayload(ScriptProjectAggregate aggregate) {
        ScriptDocumentPayload payload = new ScriptDocumentPayload();
        payload.projectId = aggregate.project.projectId;
        payload.originalText = readText(findFile(aggregate, aggregate.project.originalScriptFileId));
        payload.refinedMarkdown = readText(findFile(aggregate, aggregate.project.refinedScriptFileId));
        StoredFileRecord structuredFile = findFile(aggregate, aggregate.project.refinedScriptJsonFileId);
        if (structuredFile != null) {
            payload.structuredScript = localAssetFileService.readJson(structuredFile);
        }
        payload.documents = new ArrayList<>(aggregate.documents);
        return payload;
    }

    public StoredFileRecord findFile(ScriptProjectAggregate aggregate, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        for (StoredFileRecord file : aggregate.files) {
            if (fileId.equals(file.fileId)) {
                return file;
            }
        }
        return null;
    }

    /**
     * 按 fileId 解析 {@link StoredFileRecord}（跨剧本工程查询，用于历史恢复等）。
     */
    public StoredFileRecord findStoredRecordByFileId(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        String projectId = localAssetFileService.extractProjectId(fileId);
        if (projectId == null) {
            return null;
        }
        return scriptProjectRepository.findById(projectId)
                .map(aggregate -> findFile(aggregate, fileId))
                .orElse(null);
    }

    public String readText(StoredFileRecord record) {
        return record == null ? "" : localAssetFileService.readText(record);
    }

    public Map<String, Object> readJson(StoredFileRecord record) {
        return record == null ? new LinkedHashMap<>() : localAssetFileService.readJson(record);
    }

    public void upsertFile(ScriptProjectAggregate aggregate, StoredFileRecord newRecord) {
        aggregate.files.removeIf(item -> Objects.equals(item.relativePath, newRecord.relativePath) || Objects.equals(item.fileId, newRecord.fileId));
        aggregate.files.add(newRecord);
    }

    public void replaceDocumentVersion(
            ScriptProjectAggregate aggregate,
            DocumentVersionType versionType,
            String format,
            StoredFileRecord fileRecord
    ) {
        aggregate.documents.removeIf(item -> item.versionType == versionType);
        ScriptDocumentVersion version = new ScriptDocumentVersion();
        version.documentId = nextId("doc");
        version.projectId = aggregate.project.projectId;
        version.versionType = versionType;
        version.format = format;
        version.fileId = fileRecord.fileId;
        version.contentDigest = digestBase64(readBytesForDigest(fileRecord));
        version.createdAt = Instant.now();
        aggregate.documents.add(version);
    }

    private ScriptProjectAggregate createInternal(
            String name,
            String originalText,
            MultipartFile uploadedFile,
            byte[] uploadedBytes,
            String visualStyle,
            String aspectRatio,
            Integer targetDuration,
            String language,
            String explicitTextModel,
            String explicitImageModel,
            String explicitVideoModel,
            String sourceType
    ) {
        if (originalText == null || originalText.isBlank()) {
            throw new BizException(400, "剧本文本不能为空");
        }

        ScriptProjectAggregate aggregate = new ScriptProjectAggregate();
        aggregate.project = new ScriptProject();
        aggregate.project.projectId = nextId("sp");
        aggregate.project.name = name == null || name.isBlank() ? "未命名剧本工程" : name.trim();
        aggregate.project.status = ProjectStatus.DRAFT;
        aggregate.project.sourceType = sourceType;
        aggregate.project.visualStyle = normalizeVisualStyleForWrite(visualStyle);
        aggregate.project.aspectRatio = defaultIfBlank(aspectRatio, "16:9");
        aggregate.project.targetDuration = targetDuration == null ? 15 : targetDuration;
        aggregate.project.language = defaultIfBlank(language, "中文");
        aggregate.project.explicitTextModel = blankToNull(explicitTextModel);
        aggregate.project.explicitImageModel = blankToNull(explicitImageModel);
        aggregate.project.explicitVideoModel = blankToNull(explicitVideoModel);
        aggregate.project.createdAt = Instant.now();
        aggregate.project.updatedAt = aggregate.project.createdAt;

        StoredFileRecord originalScript = localAssetFileService.storeText(
                aggregate.project.projectId,
                "documents/original-script.txt",
                "text/plain; charset=UTF-8",
                originalText
        );
        upsertFile(aggregate, originalScript);
        replaceDocumentVersion(aggregate, DocumentVersionType.ORIGINAL, "txt", originalScript);
        aggregate.project.originalScriptFileId = originalScript.fileId;
        aggregate.project.scriptSummary = summarizeText(originalText);

        if (uploadedFile != null && uploadedBytes != null) {
            String originalFilename = uploadedFile.getOriginalFilename() == null ? "script-upload.txt" : uploadedFile.getOriginalFilename();
            StoredFileRecord uploadedSource = localAssetFileService.storeBytes(
                    aggregate.project.projectId,
                    "documents/uploads/" + sanitizeFileName(originalFilename),
                    uploadedFile.getContentType(),
                    uploadedBytes
            );
            upsertFile(aggregate, uploadedSource);
            replaceDocumentVersion(aggregate, DocumentVersionType.UPLOADED_SOURCE, extensionOf(originalFilename), uploadedSource);
            aggregate.project.uploadedSourceFileId = uploadedSource.fileId;
        }

        return save(aggregate);
    }

    private String normalizeVisualStyleForWrite(String visualStyle) {
        return videoStylePresetRegistry.normalizeVisualStyleForScriptProjectWrite(visualStyle);
    }

    private String summarizeText(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 140) {
            return normalized;
        }
        return normalized.substring(0, 140) + "…";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-\\u4e00-\\u9fa5]", "_");
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private byte[] readBytesForDigest(StoredFileRecord fileRecord) {
        return localAssetFileService.readBytes(fileRecord);
    }

    private String digestBase64(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(input));
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }

    private String nextId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    // ── Workflow model settings ────────────────────────────────────────────

    public WorkflowModelSettingsResponse getModelSettings(String projectId) {
        ScriptProjectAggregate aggregate = require(projectId);
        return toModelSettingsResponse(aggregate.project);
    }

    public WorkflowModelSettingsResponse updateModelSettings(String projectId, WorkflowModelSettingsUpdateRequest request) {
        ScriptProjectAggregate aggregate = require(projectId);
        ScriptProject p = aggregate.project;
        if (request.defaultTextModel() != null) {
            p.explicitTextModel = blankToNull(request.defaultTextModel());
        }
        if (request.defaultImageModel() != null) {
            p.explicitImageModel = blankToNull(request.defaultImageModel());
        }
        if (request.defaultVideoModel() != null) {
            p.explicitVideoModel = blankToNull(request.defaultVideoModel());
        }
        if (request.overrides() != null) {
            Map<String, String> current = parseOverrides(p.workflowModelOverrides);
            for (Map.Entry<String, String> entry : request.overrides().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) continue;
                String val = entry.getValue();
                if (val == null || val.isBlank()) {
                    current.remove(entry.getKey());
                } else {
                    current.put(entry.getKey(), val.trim());
                }
            }
            p.workflowModelOverrides = serializeOverrides(current);
        }
        save(aggregate);
        return toModelSettingsResponse(p);
    }

    /**
     * Resolve the effective model name for a given workflow function key.
     * Lookup order:
     *   1. workflowModelOverrides[functionKey]
     *   2. project-level explicit field (matched by defaultCapability: "text" | "image" | "video")
     *   3. null  (caller falls back to AiCapabilityRoutingService auto-routing)
     */
    public String resolveWorkflowModel(ScriptProject project, String functionKey, String defaultCapability) {
        Map<String, String> overrides = parseOverrides(project.workflowModelOverrides);
        String override = overrides.get(functionKey);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return switch (defaultCapability) {
            case "text"  -> project.explicitTextModel;
            case "image" -> project.explicitImageModel;
            case "video" -> project.explicitVideoModel;
            default      -> null;
        };
    }

    public Map<String, String> parseOverrides(String json) {
        if (json == null || json.isBlank()) return new java.util.LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, STR_MAP_TYPE);
        } catch (Exception ex) {
            return new java.util.LinkedHashMap<>();
        }
    }

    private String serializeOverrides(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(overrides);
        } catch (Exception ex) {
            return null;
        }
    }

    private WorkflowModelSettingsResponse toModelSettingsResponse(ScriptProject p) {
        return new WorkflowModelSettingsResponse(
                p.projectId,
                p.explicitTextModel,
                p.explicitImageModel,
                p.explicitVideoModel,
                parseOverrides(p.workflowModelOverrides)
        );
    }

    public Map<String, String> getPromptTemplateOverrides(String projectId) {
        ScriptProject p = require(projectId).project;
        return promptTemplateService.parseOverrideMap(p.promptTemplateOverrides);
    }

    public Map<String, String> updatePromptTemplateOverrides(String projectId, PromptTemplateOverridesUpdateRequest request) {
        ScriptProjectAggregate aggregate = require(projectId);
        ScriptProject p = aggregate.project;
        Map<String, String> incoming = request == null || request.overrides() == null ? Map.of() : request.overrides();
        String merged = promptTemplateService.mergeSanitizeAllOverrides(p.promptTemplateOverrides, incoming);
        p.promptTemplateOverrides = merged;
        p.updatedAt = Instant.now();
        save(aggregate);
        return promptTemplateService.parseOverrideMap(merged);
    }
}
