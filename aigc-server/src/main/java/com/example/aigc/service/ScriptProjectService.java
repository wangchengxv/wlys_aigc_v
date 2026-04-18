package com.example.aigc.service;

import com.example.aigc.dto.PromptTemplateOverridesUpdateRequest;
import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.ScriptProjectCreateRequest;
import com.example.aigc.dto.UpdateScriptRequest;
import com.example.aigc.dto.WorkflowModelSettingsResponse;
import com.example.aigc.dto.WorkflowModelSettingsUpdateRequest;
import com.example.aigc.entity.ScriptDocumentVersion;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.ScriptRevision;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.DocumentVersionType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.RevisionKind;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.repository.ScriptProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final StyleTemplateService styleTemplateService;
    private final AuditLogService auditLogService;
    private final AuthorizationService authorizationService;

    private static final TypeReference<Map<String, String>> STR_MAP_TYPE = new TypeReference<>() {};

    public ScriptProjectService(
            ScriptProjectRepository scriptProjectRepository,
            LocalAssetFileService localAssetFileService,
            ScriptDocxService scriptDocxService,
            VideoStylePresetRegistry videoStylePresetRegistry,
            ObjectMapper objectMapper,
            PromptTemplateService promptTemplateService,
            StyleTemplateService styleTemplateService,
            AuditLogService auditLogService,
            AuthorizationService authorizationService
    ) {
        this.scriptProjectRepository = scriptProjectRepository;
        this.localAssetFileService = localAssetFileService;
        this.scriptDocxService = scriptDocxService;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
        this.styleTemplateService = styleTemplateService;
        this.auditLogService = auditLogService;
        this.authorizationService = authorizationService;
    }

    public ScriptProjectAggregate create(RequestUserContext userContext, ScriptProjectCreateRequest request) {
        return createInternal(
                userContext,
                request.name(),
                request.sourceText(),
                null,
                null,
                request.visualStyle(),
                request.styleTemplateId(),
                request.aspectRatio(),
                request.targetDuration(),
                request.language(),
                request.courseId(),
                request.explicitTextModel(),
                request.explicitImageModel(),
                request.explicitVideoModel(),
                "text"
        );
    }

    public ScriptProjectAggregate createFromUpload(
            RequestUserContext userContext,
            String name,
            MultipartFile file,
            String visualStyle,
            String styleTemplateId,
            String aspectRatio,
            Integer targetDuration,
            String language,
            String courseId,
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
                    userContext,
                    name,
                    originalText,
                    file,
                    bytes,
                    visualStyle,
                    styleTemplateId,
                    aspectRatio,
                    targetDuration,
                    language,
                    courseId,
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

    public List<ScriptProjectSummary> list(RequestUserContext userContext, boolean deleted) {
        return scriptProjectRepository.findAll(deleted).stream()
                .filter(item -> userContext.isAdmin() || isVisibleToOwner(item.ownerId, userContext.userId()))
                .sorted(Comparator.comparing((ScriptProjectSummary item) -> item.updatedAt, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
    }

    public ScriptProjectAggregate require(String projectId) {
        return requireInternal(projectId, false);
    }

    public ScriptProjectAggregate require(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = requireInternal(projectId, false);
        if (!authorizationService.canReadProject(aggregate.project, actor)) {
            throw new BizException(403, "无权访问该剧本工程");
        }
        return aggregate;
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

    public ScriptProjectAggregate restore(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = requireInternal(projectId, true);
        if (!authorizationService.canWriteProject(aggregate.project, actor)) {
            throw new BizException(403, "无权恢复该剧本工程");
        }
        aggregate = restore(projectId);
        auditLogService.record(actor, "PROJECT_RESTORED", "SCRIPT_PROJECT", projectId, Map.of(
                "projectName", aggregate.project.name
        ));
        return aggregate;
    }

    public void assertProjectAccess(String projectId, RequestUserContext actor, boolean writeAccess) {
        ScriptProjectAggregate aggregate = requireInternal(projectId, true);
        boolean allowed = writeAccess
                ? authorizationService.canWriteProject(aggregate.project, actor)
                : authorizationService.canReadProject(aggregate.project, actor);
        if (!allowed) {
            throw new BizException(403, writeAccess ? "无权修改该剧本工程" : "无权访问该剧本工程");
        }
    }

    private ScriptProjectAggregate requireInternal(String projectId, boolean includeDeleted) {
        ScriptProjectAggregate aggregate = scriptProjectRepository.findById(projectId)
                .orElseThrow(() -> new BizException(404, "剧本工程不存在"));
        if (!includeDeleted && aggregate.project != null && aggregate.project.deletedAt != null) {
            throw new BizException(404, "剧本工程不存在或已删除");
        }
        if (aggregate.project != null) {
            aggregate.project.visualStyle = videoStylePresetRegistry.normalizeStyleKeyForRead(aggregate.project.visualStyle);
            if (aggregate.project.contentReviewStatus == null) {
                aggregate.project.contentReviewStatus = ContentReviewStatus.NOT_SUBMITTED;
            }
            if (aggregate.project.reviewResubmitCount == null) {
                aggregate.project.reviewResubmitCount = 0;
            }
        }
        if (aggregate.contentReviewRecords == null) {
            aggregate.contentReviewRecords = new ArrayList<>();
        }
        if (aggregate.revisions == null) {
            aggregate.revisions = new ArrayList<>();
        }
        if (aggregate.documents == null) {
            aggregate.documents = new ArrayList<>();
        }
        if (aggregate.files == null) {
            aggregate.files = new ArrayList<>();
        }
        if (aggregate.assets == null) {
            aggregate.assets = new ArrayList<>();
        }
        if (aggregate.keyframes == null) {
            aggregate.keyframes = new ArrayList<>();
        }
        if (aggregate.shots == null) {
            aggregate.shots = new ArrayList<>();
        }
        if (aggregate.videoTasks == null) {
            aggregate.videoTasks = new ArrayList<>();
        }
        if (aggregate.dubbingTasks == null) {
            aggregate.dubbingTasks = new ArrayList<>();
        }
        if (aggregate.lipSyncTasks == null) {
            aggregate.lipSyncTasks = new ArrayList<>();
        }
        if (aggregate.videoEditRenderTasks == null) {
            aggregate.videoEditRenderTasks = new ArrayList<>();
        }
        if (aggregate.finalCompositionTasks == null) {
            aggregate.finalCompositionTasks = new ArrayList<>();
        }
        if (aggregate.exportPackageTasks == null) {
            aggregate.exportPackageTasks = new ArrayList<>();
        }
        if (aggregate.pipelineRuns == null) {
            aggregate.pipelineRuns = new ArrayList<>();
        }
        return aggregate;
    }

    public ScriptProjectAggregate save(ScriptProjectAggregate aggregate) {
        aggregate.project.updatedAt = Instant.now();
        return scriptProjectRepository.save(aggregate);
    }

    public void delete(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = requireInternal(projectId, false);
        if (!authorizationService.canDeleteProject(aggregate.project, actor)) {
            throw new BizException(403, "无权删除该剧本工程");
        }
        scriptProjectRepository.delete(projectId);
        auditLogService.record(actor, "PROJECT_DELETED", "SCRIPT_PROJECT", projectId, Map.of(
                "projectId", projectId
        ));
    }

    public ScriptDocumentPayload getScriptPayload(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = require(projectId, actor);
        return buildScriptPayload(aggregate);
    }

    public ScriptDocumentPayload updateScript(String projectId, UpdateScriptRequest request, RequestUserContext actor) {
        assertProjectAccess(projectId, actor, true);
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
            RequestUserContext userContext,
            String name,
            String originalText,
            MultipartFile uploadedFile,
            byte[] uploadedBytes,
            String visualStyle,
            String styleTemplateId,
            String aspectRatio,
            Integer targetDuration,
            String language,
            String courseId,
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
        aggregate.project.ownerId = userContext.userId();
        aggregate.project.ownerName = defaultIfBlank(userContext.userName(), userContext.userId());
        aggregate.project.orgUnitId = blankToNull(userContext.orgUnitId());
        String effectiveCourseId = firstNonBlank(courseId, userContext.courseId());
        aggregate.project.courseId = effectiveCourseId;
        aggregate.project.name = name == null || name.isBlank() ? "未命名剧本工程" : name.trim();
        aggregate.project.status = ProjectStatus.DRAFT;
        aggregate.project.sourceType = sourceType;
        aggregate.project.styleTemplateId = validateStyleTemplateIfPresent(styleTemplateId, userContext, effectiveCourseId);
        aggregate.project.visualStyle = normalizeVisualStyleForWrite(visualStyle);
        aggregate.project.aspectRatio = defaultIfBlank(aspectRatio, "16:9");
        aggregate.project.targetDuration = targetDuration == null ? 15 : targetDuration;
        aggregate.project.language = defaultIfBlank(language, "中文");
        aggregate.project.explicitTextModel = blankToNull(explicitTextModel);
        aggregate.project.explicitImageModel = blankToNull(explicitImageModel);
        aggregate.project.explicitVideoModel = blankToNull(explicitVideoModel);
        aggregate.project.explicitTtsModel = null;
        aggregate.project.dubbingVoice = "通用女声";
        aggregate.project.dubbingLanguage = aggregate.project.language;
        aggregate.project.dubbingSpeed = 1.0d;
        aggregate.project.contentReviewStatus = ContentReviewStatus.NOT_SUBMITTED;
        aggregate.project.reviewResubmitCount = 0;
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

        ScriptProjectAggregate saved = save(aggregate);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("projectName", saved.project.name);
        details.put("sourceType", sourceType);
        details.put("styleTemplateId", saved.project.styleTemplateId);
        details.put("courseId", saved.project.courseId);
        auditLogService.record(userContext, "PROJECT_CREATED", "SCRIPT_PROJECT", saved.project.projectId, details);
        return saved;
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

    private String firstNonBlank(String value, String fallback) {
        String normalized = blankToNull(value);
        if (normalized != null) {
            return normalized;
        }
        return blankToNull(fallback);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private boolean isVisibleToOwner(String projectOwnerId, String currentOwnerId) {
        return projectOwnerId == null || projectOwnerId.isBlank() || Objects.equals(projectOwnerId, currentOwnerId);
    }

    private String validateStyleTemplateIfPresent(String styleTemplateId, RequestUserContext userContext, String courseId) {
        String normalized = blankToNull(styleTemplateId);
        if (normalized == null) {
            return null;
        }
        styleTemplateService.requireVisibleForCourse(normalized, userContext, courseId);
        return normalized;
    }

    // ── Workflow model settings ────────────────────────────────────────────

    public WorkflowModelSettingsResponse getModelSettings(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = require(projectId, actor);
        return toModelSettingsResponse(aggregate.project);
    }

    public WorkflowModelSettingsResponse updateModelSettings(String projectId, WorkflowModelSettingsUpdateRequest request, RequestUserContext actor) {
        assertProjectAccess(projectId, actor, true);
        ScriptProjectAggregate aggregate = requireInternal(projectId, false);
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
        if (request.defaultTtsModel() != null) {
            p.explicitTtsModel = blankToNull(request.defaultTtsModel());
        }
        if (request.dubbingVoice() != null) {
            p.dubbingVoice = blankToNull(request.dubbingVoice());
        }
        if (request.dubbingLanguage() != null) {
            p.dubbingLanguage = blankToNull(request.dubbingLanguage());
        }
        if (request.dubbingSpeed() != null) {
            p.dubbingSpeed = normalizeDubbingSpeed(request.dubbingSpeed());
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
     *   2. project-level explicit field (matched by defaultCapability: "text" | "image" | "video" | "tts")
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
            case "tts"   -> project.explicitTtsModel;
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
                p.explicitTtsModel,
                p.dubbingVoice,
                p.dubbingLanguage,
                normalizeDubbingSpeed(p.dubbingSpeed),
                parseOverrides(p.workflowModelOverrides)
        );
    }

    private Double normalizeDubbingSpeed(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 1.0d;
        }
        return Math.max(0.5d, Math.min(2.0d, value));
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
