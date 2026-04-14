package com.example.aigc.service;

import com.example.aigc.dto.ArtDirectionResponse;
import com.example.aigc.dto.ApplyStoryboardFirstFrameRequest;
import com.example.aigc.dto.BatchVisualPromptResponse;
import com.example.aigc.dto.GenerateGroupSceneRequest;
import com.example.aigc.dto.GroupSceneResponse;
import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.ShotVisualPromptResponse;
import com.example.aigc.dto.StoryboardFirstFrameResponse;
import com.example.aigc.dto.StoryboardImageResponse;
import com.example.aigc.dto.StoryboardPanelCropResponse;
import com.example.aigc.dto.StoryboardPlanResponse;
import com.example.aigc.dto.StoryboardRewriteRequest;
import com.example.aigc.dto.ThreeViewResponse;
import com.example.aigc.dto.TurnaroundImageResponse;
import com.example.aigc.dto.TurnaroundPlanResponse;
import com.example.aigc.dto.PromptVersion;
import com.example.aigc.dto.RollbackPromptRequest;
import com.example.aigc.dto.UpdateKeyframePromptRequest;
import com.example.aigc.dto.UpdateShotRequest;
import com.example.aigc.dto.UpdateAssetRequest;
import com.example.aigc.dto.UpdateScriptRequest;
import com.example.aigc.dto.VisualPromptResponse;
import com.example.aigc.dto.AppendScriptPreviewRequest;
import com.example.aigc.dto.AppendScriptPreviewResponse;
import com.example.aigc.dto.RewriteScriptApplyRequest;
import com.example.aigc.dto.RewriteScriptPreviewRequest;
import com.example.aigc.dto.RewriteScriptPreviewResponse;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.enums.AssetHistoryType;
import com.example.aigc.enums.AssetStatus;
import com.example.aigc.enums.AssetType;
import com.example.aigc.enums.DocumentVersionType;
import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.PromptVersionSource;
import com.example.aigc.enums.RevisionKind;
import com.example.aigc.exception.BizException;
import com.example.aigc.model.VideoStylePresetRegistry;
import com.example.aigc.model.WorkflowModelKey;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScriptWorkflowService {

    private final ScriptProjectService scriptProjectService;
    private final PromptTemplateService promptTemplateService;
    private final PromptVersionService promptVersionService;
    private final AiCapabilityRoutingService aiCapabilityRoutingService;
    private final ProviderHttpGateway providerHttpGateway;
    private final LocalAssetFileService localAssetFileService;
    private final ObjectMapper objectMapper;
    private final VideoStylePresetRegistry videoStylePresetRegistry;
    private final AssetHistoryService assetHistoryService;

    private static final int STORYBOARD_PANEL_COUNT = 9;
    private static final String FIRST_FRAME_MODE_NONE = "NONE";
    private static final String FIRST_FRAME_MODE_FULL_GRID = "FULL_GRID";
    private static final String FIRST_FRAME_MODE_CROPPED_PANEL = "CROPPED_PANEL";

    public ScriptWorkflowService(
            ScriptProjectService scriptProjectService,
            PromptTemplateService promptTemplateService,
            PromptVersionService promptVersionService,
            AiCapabilityRoutingService aiCapabilityRoutingService,
            ProviderHttpGateway providerHttpGateway,
            LocalAssetFileService localAssetFileService,
            ObjectMapper objectMapper,
            VideoStylePresetRegistry videoStylePresetRegistry,
            AssetHistoryService assetHistoryService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.promptTemplateService = promptTemplateService;
        this.promptVersionService = promptVersionService;
        this.aiCapabilityRoutingService = aiCapabilityRoutingService;
        this.providerHttpGateway = providerHttpGateway;
        this.localAssetFileService = localAssetFileService;
        this.objectMapper = objectMapper;
        this.videoStylePresetRegistry = videoStylePresetRegistry;
        this.assetHistoryService = assetHistoryService;
    }

    public ScriptDocumentPayload refineScript(String projectId) {
        scriptProjectService.snapshotBeforeRefineIfNeeded(projectId);
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        String originalText = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.originalScriptFileId));
        if (originalText.isBlank()) {
            throw new BizException(400, "原始剧本为空，无法完善");
        }

        PipelineRun pipelineRun = beginPipeline(aggregate, PipelineType.REFINE, "REFINE_SCRIPT", 1);
        aggregate.project.status = ProjectStatus.SCRIPT_REFINING;
        scriptProjectService.save(aggregate);

        Map<String, Object> structuredScript;
        try {
            structuredScript = buildStructuredScript(aggregate, originalText, null);
            String refinedMarkdown = buildRefinedMarkdown(structuredScript);

            StoredFileRecord markdownFile = localAssetFileService.storeText(
                    projectId,
                    "documents/refined-script.md",
                    "text/markdown; charset=UTF-8",
                    refinedMarkdown
            );
            scriptProjectService.upsertFile(aggregate, markdownFile);
            scriptProjectService.replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_MARKDOWN, "md", markdownFile);
            aggregate.project.refinedScriptFileId = markdownFile.fileId;

            StoredFileRecord jsonFile = localAssetFileService.storeJson(projectId, "documents/refined-script.json", structuredScript);
            scriptProjectService.upsertFile(aggregate, jsonFile);
            scriptProjectService.replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_JSON, "json", jsonFile);
            aggregate.project.refinedScriptJsonFileId = jsonFile.fileId;

            aggregate.project.scriptSummary = stringValue(structuredScript.get("summary"), aggregate.project.scriptSummary);
            aggregate.project.status = ProjectStatus.SCRIPT_READY;
            finishPipeline(pipelineRun, PipelineStatus.SUCCESS, 1, 0, null);
            scriptProjectService.save(aggregate);
            return scriptProjectService.buildScriptPayload(aggregate);
        } catch (Exception ex) {
            finishPipeline(pipelineRun, PipelineStatus.FAILED, 0, 1, ex.getMessage());
            aggregate.project.status = ProjectStatus.FAILED;
            scriptProjectService.save(aggregate);
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(500, "完善剧本失败：" + safeError(ex));
        }
    }

    public ScriptDocumentPayload refineScriptWithBrief(String projectId, String briefPrompt) {
        scriptProjectService.snapshotBeforeRefineIfNeeded(projectId);
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        String originalText = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.originalScriptFileId));
        if (originalText.isBlank()) {
            throw new BizException(400, "原始剧本为空，无法完善");
        }

        PipelineRun pipelineRun = beginPipeline(aggregate, PipelineType.REFINE, "REFINE_SCRIPT", 1);
        aggregate.project.status = ProjectStatus.SCRIPT_REFINING;
        scriptProjectService.save(aggregate);

        Map<String, Object> structuredScript;
        try {
            structuredScript = buildStructuredScript(aggregate, originalText, briefPrompt);
            String refinedMarkdown = buildRefinedMarkdown(structuredScript);

            StoredFileRecord markdownFile = localAssetFileService.storeText(
                    projectId,
                    "documents/refined-script.md",
                    "text/markdown; charset=UTF-8",
                    refinedMarkdown
            );
            scriptProjectService.upsertFile(aggregate, markdownFile);
            scriptProjectService.replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_MARKDOWN, "md", markdownFile);
            aggregate.project.refinedScriptFileId = markdownFile.fileId;

            StoredFileRecord jsonFile = localAssetFileService.storeJson(projectId, "documents/refined-script.json", structuredScript);
            scriptProjectService.upsertFile(aggregate, jsonFile);
            scriptProjectService.replaceDocumentVersion(aggregate, DocumentVersionType.REFINED_JSON, "json", jsonFile);
            aggregate.project.refinedScriptJsonFileId = jsonFile.fileId;

            aggregate.project.scriptSummary = stringValue(structuredScript.get("summary"), aggregate.project.scriptSummary);
            aggregate.project.status = ProjectStatus.SCRIPT_READY;
            finishPipeline(pipelineRun, PipelineStatus.SUCCESS, 1, 0, null);
            scriptProjectService.save(aggregate);
            return scriptProjectService.buildScriptPayload(aggregate);
        } catch (Exception ex) {
            finishPipeline(pipelineRun, PipelineStatus.FAILED, 0, 1, ex.getMessage());
            aggregate.project.status = ProjectStatus.FAILED;
            scriptProjectService.save(aggregate);
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(500, "完善剧本失败：" + safeError(ex));
        }
    }

    public RewriteScriptPreviewResponse rewriteScriptPreview(String projectId, RewriteScriptPreviewRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        String refined = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.refinedScriptFileId));
        String original = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.originalScriptFileId));
        String baseUsed = refined != null && !refined.isBlank() ? "refined" : "original";
        String originalScript = "refined".equals(baseUsed) ? refined : original;
        if (originalScript == null || originalScript.isBlank()) {
            throw new BizException(400, "已有剧本为空，无法改写");
        }

        String rewriteInstruction = stringValue(request.rewriteInstruction(), "").trim();
        if (rewriteInstruction.isBlank()) {
            throw new BizException(400, "改写要求不能为空");
        }
        String targetStyle = stringValue(request.targetStyle(), "保持原风格并提升冲突与情感张力");
        String language = stringValue(request.language(), stringValue(aggregate.project.language, "中文"));
        String maxOutputCharsRule = request.maxOutputChars() == null
                ? ""
                : "- 本次改写的字数上限为：**" + request.maxOutputChars() + " 字**，请在此范围内确保内容完整性";

        String effectiveTextModelRewrite = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.SCRIPT_REWRITE, "text");
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(effectiveTextModelRewrite);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String reason = effectiveTextModelRewrite == null || effectiveTextModelRewrite.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + effectiveTextModelRewrite;
            throw new BizException(400, reason);
        }

        String systemPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/script/rewrite-system.md",
                Map.of("language", language),
                "你是一位专业剧本顾问，请在保留核心剧情与角色设定的前提下完成高质量改写。"
        );
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/script/rewrite-user.md",
                Map.of(
                        "language", language,
                        "rewriteInstruction", rewriteInstruction,
                        "targetStyle", targetStyle,
                        "maxOutputCharsRule", maxOutputCharsRule,
                        "originalScript", originalScript
                ),
                "请根据改写要求重写以下剧本：\n{{originalScript}}"
        );

        String extractedContent;
        try {
            Map<String, Object> payload = buildChatPayload(resolvedModel, systemPrompt, userPrompt);
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload,
                    Duration.ofSeconds(120)
            );
            extractedContent = extractTextContent(response, resolvedModel.provider().apiFormat(), resolvedModel.provider().key());
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用文本模型失败：" + safeError(ex));
        }

        String rewrittenText = normalizeRewriteText(extractedContent, request.maxOutputChars());
        if (rewrittenText.isBlank()) {
            throw new BizException(502, "模型未返回可用改写内容");
        }
        return new RewriteScriptPreviewResponse(baseUsed, originalScript.length(), request.maxOutputChars(), rewrittenText);
    }

    public ScriptDocumentPayload applyRewrite(String projectId, RewriteScriptApplyRequest request) {
        String rewrittenText = request.rewrittenText() == null ? "" : request.rewrittenText().trim();
        if (rewrittenText.isBlank()) {
            throw new BizException(400, "改写结果不能为空");
        }
        scriptProjectService.snapshotBeforeOptimize(projectId, "before-rewrite", RevisionKind.REWRITE);
        return scriptProjectService.updateScript(projectId, new UpdateScriptRequest(rewrittenText, null), false);
    }

    public ScriptDocumentPayload optimizeScenes(String projectId) {
        return runOptimizeScript(projectId, "scenes", RevisionKind.OPTIMIZE_SCENE,
                "prompts/script/optimize-scene-system.md",
                "prompts/script/optimize-scene-user.md");
    }

    public ScriptDocumentPayload optimizeCharacters(String projectId) {
        return runOptimizeScript(projectId, "characters", RevisionKind.OPTIMIZE_CHARACTER,
                "prompts/script/optimize-character-system.md",
                "prompts/script/optimize-character-user.md");
    }

    public ScriptDocumentPayload optimizeProps(String projectId) {
        return runOptimizeScript(projectId, "props", RevisionKind.OPTIMIZE_PROP,
                "prompts/script/optimize-prop-system.md",
                "prompts/script/optimize-prop-user.md");
    }

    public AppendScriptPreviewResponse appendScriptPreview(String projectId, AppendScriptPreviewRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        String language = stringValue(aggregate.project.language, "中文");
        String refined = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.refinedScriptFileId));
        String original = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.originalScriptFileId));
        String baseUsed = refined != null && !refined.isBlank() ? "refined" : "original";
        String baseText = "refined".equals(baseUsed) ? refined : original;
        if (baseText == null || baseText.isBlank()) {
            throw new BizException(400, "已有剧本为空，无法续写");
        }
        int existingLength = baseText.length();
        int maxAppendChars = resolveMaxAppendChars(existingLength, request == null ? null : request.maxAppendChars());

        String effectiveTextModelAppend = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.SCRIPT_APPEND, "text");
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(effectiveTextModelAppend);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String reason = effectiveTextModelAppend == null || effectiveTextModelAppend.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + effectiveTextModelAppend;
            throw new BizException(400, reason);
        }

        String systemPrompt = promptTemplateService.renderTemplate(
                "你是一位资深剧本创作者。你的任务是续写剧本后续情节，保持风格一致、情节连贯，且只输出续写正文。",
                Map.of()
        );
        String maxTotalCharsNote = "";
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/script/append-user.md",
                Map.of(
                        "language", language,
                        "existingScript", baseText,
                        "existingLength", String.valueOf(existingLength),
                        "maxAppendChars", String.valueOf(maxAppendChars),
                        "maxTotalCharsNote", maxTotalCharsNote
                ),
                baseText
        );
        String extractedContent;
        try {
            Map<String, Object> payload = buildChatPayload(resolvedModel, systemPrompt, userPrompt);
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload,
                    Duration.ofSeconds(120)
            );
            extractedContent = extractTextContent(response, resolvedModel.provider().apiFormat(), resolvedModel.provider().key());
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用文本模型失败：" + safeError(ex));
        }
        String appendText = normalizeAppendText(extractedContent, maxAppendChars);
        if (appendText.isBlank()) {
            throw new BizException(502, "模型未返回可用续写内容");
        }
        return new AppendScriptPreviewResponse(baseUsed, existingLength, maxAppendChars, appendText);
    }

    private ScriptDocumentPayload runOptimizeScript(
            String projectId,
            String optimizeKind,
            RevisionKind revisionKind,
            String systemPath,
            String userPath
    ) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        if (structuredScript.isEmpty()) {
            throw new BizException(400, "请先完善剧本，再执行智能体优化");
        }
        scriptProjectService.snapshotBeforeOptimize(projectId, "before-optimize-" + optimizeKind, revisionKind);
        aggregate = scriptProjectService.require(projectId);
        structuredScript = loadStructuredScript(aggregate);
        String originalText = scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.originalScriptFileId));
        String structuredJson;
        try {
            structuredJson = objectMapper.writeValueAsString(structuredScript);
        } catch (Exception ex) {
            throw new BizException(500, "剧本序列化失败");
        }
        String effectiveTextModelOpt = scriptProjectService.resolveWorkflowModel(aggregate.project, WorkflowModelKey.SCRIPT_REFINE, "text");
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(effectiveTextModelOpt);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String reason = effectiveTextModelOpt == null || effectiveTextModelOpt.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + effectiveTextModelOpt;
            throw new BizException(400, reason);
        }
        String systemPrompt = promptTemplateService.renderForProject(aggregate.project, 
                systemPath,
                Map.of("language", stringValue(aggregate.project.language, "中文")),
                "系统提示"
        );
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                userPath,
                Map.of(
                        "projectName", stringValue(aggregate.project.name, "未命名项目"),
                        "visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle),
                        "language", stringValue(aggregate.project.language, "中文"),
                        "structuredScriptJson", structuredJson,
                        "originalExcerpt", excerptForPrompt(originalText, 6000)
                ),
                "请优化剧本"
        );
        Map<String, Object> delta;
        String extractedContent = null;
        try {
            Map<String, Object> payload = buildChatPayload(resolvedModel, systemPrompt, userPrompt);
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload,
                    Duration.ofSeconds(120)
            );
            extractedContent = extractTextContent(response, resolvedModel.provider().apiFormat(), resolvedModel.provider().key());
            delta = parseJsonContent(extractedContent);
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用文本模型失败：" + safeError(ex));
        }
        if (delta == null || delta.isEmpty()) {
            persistOptimizeFailureContext(
                    aggregate,
                    projectId,
                    optimizeKind,
                    resolvedModel,
                    "模型返回内容不是有效 JSON",
                    extractedContent
            );
            aggregate.project.status = ProjectStatus.FAILED;
            scriptProjectService.save(aggregate);
            throw new BizException(502, "模型返回内容不是有效 JSON");
        }
        Map<String, Object> merged = new LinkedHashMap<>(structuredScript);
        switch (optimizeKind) {
            case "scenes" -> {
                merged.put("scenes", mergeMapListById(asMapList(structuredScript.get("scenes")), asMapList(delta.get("scenes"))));
                merged.put("segments", mergeMapListById(asMapList(structuredScript.get("segments")), asMapList(delta.get("segments"))));
            }
            case "characters" -> merged.put("characters", mergeMapListById(asMapList(structuredScript.get("characters")), asMapList(delta.get("characters"))));
            case "props" -> merged.put("props", mergeMapListById(asMapList(structuredScript.get("props")), asMapList(delta.get("props"))));
            default -> throw new BizException(500, "未知的优化类型");
        }
        normalizeSegmentRefs(merged);
        String refinedMarkdown = buildRefinedMarkdown(merged);
        return scriptProjectService.updateScript(projectId, new UpdateScriptRequest(refinedMarkdown, merged), false);
    }

    private void persistOptimizeFailureContext(
            ScriptProjectAggregate aggregate,
            String projectId,
            String optimizeKind,
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel,
            String reason,
            String extractedContent
    ) {
        String providerKey = resolvedModel.provider() == null ? "N/A" : resolvedModel.provider().key();
        String connectionName = resolvedModel.connection() == null ? "N/A" : resolvedModel.connection().getName();
        String modelName = stringValue(resolvedModel.modelName(), "N/A");
        String normalized = extractedContent == null ? "" : extractedContent.trim();
        String snippet = normalized;
        int maxLen = 4000;
        if (snippet.length() > maxLen) {
            snippet = snippet.substring(0, maxLen) + "…";
        }
        String fileContent = """
                reason: %s
                optimizeKind: %s
                model: %s
                provider: %s
                connection: %s
                contentSnippet:
                %s
                """.formatted(reason, optimizeKind, modelName, providerKey, connectionName, snippet);
        StoredFileRecord rawResponse = localAssetFileService.storeText(
                projectId,
                "documents/optimize-raw-response-" + optimizeKind + ".txt",
                "text/plain; charset=UTF-8",
                fileContent
        );
        scriptProjectService.upsertFile(aggregate, rawResponse);
    }

    private String excerptForPrompt(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= maxLen) {
            return t;
        }
        return t.substring(0, maxLen) + "…";
    }

    private int resolveMaxAppendChars(int existingLength, Integer requested) {
        int suggested = (int) Math.round(existingLength * 0.4);
        int base = requested != null && requested > 0 ? requested : suggested;
        return Math.max(base, 200);
    }

    private String normalizeAppendText(String raw, int maxAppendChars) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:text|markdown)?", "");
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        text = text.trim();
        // best-effort strip common prefixes
        String[] prefixes = {"续写：", "以下是续写内容：", "以下为续写内容：", "续写内容：", "续写如下："};
        for (String p : prefixes) {
            if (text.startsWith(p)) {
                text = text.substring(p.length()).trim();
                break;
            }
        }
        return text.trim();
    }

    private String normalizeRewriteText(String raw, Integer maxOutputChars) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:text|markdown)?", "");
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        text = text.trim();
        return text.trim();
    }

    private List<Map<String, Object>> mergeMapListById(List<Map<String, Object>> base, List<Map<String, Object>> delta) {
        if (delta == null || delta.isEmpty()) {
            return base;
        }
        Map<String, Map<String, Object>> deltaById = new LinkedHashMap<>();
        for (Map<String, Object> row : delta) {
            String id = stringValue(row.get("id"), "");
            if (!id.isBlank()) {
                deltaById.put(id, row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : base) {
            String id = stringValue(row.get("id"), "");
            Map<String, Object> merged = new LinkedHashMap<>(row);
            Map<String, Object> patch = deltaById.get(id);
            if (patch != null) {
                merged.putAll(patch);
            }
            result.add(merged);
        }
        return result;
    }

    public List<ExtractedAsset> extractAssets(String projectId, AssetType assetType) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        if (structuredScript.isEmpty()) {
            throw new BizException(400, "请先完善剧本，再执行资产抽取");
        }

        PipelineRun pipelineRun = beginPipeline(aggregate, PipelineType.ASSET_EXTRACTION, "EXTRACT_" + assetType.name(), 1);
        aggregate.project.status = ProjectStatus.ASSET_EXTRACTING;

        List<String> removedAssetIds = aggregate.assets.stream()
                .filter(item -> item.assetType == assetType)
                .map(item -> item.assetId)
                .toList();
        aggregate.assets.removeIf(item -> item.assetType == assetType);
        aggregate.keyframes.removeIf(item -> removedAssetIds.contains(item.assetId));

        List<Map<String, Object>> sourceItems = switch (assetType) {
            case CHARACTER -> asMapList(structuredScript.get("characters"));
            case BACKGROUND -> resolveBackgroundItems(structuredScript);
            case PROP -> asMapList(structuredScript.get("props"));
        };

        List<ExtractedAsset> created = new ArrayList<>();
        Instant now = Instant.now();
        for (int index = 0; index < sourceItems.size(); index++) {
            Map<String, Object> item = sourceItems.get(index);
            ExtractedAsset asset = new ExtractedAsset();
            asset.assetId = nextId(prefixForAssetType(assetType));
            asset.projectId = projectId;
            asset.assetType = assetType;
            asset.name = stringValue(item.get("name"), defaultAssetName(assetType, index + 1));
            asset.description = stringValue(item.get("description"),
                    stringValue(item.get("summary"), asset.name + "视觉设定"));
            asset.promptDraft = buildAssetPromptDraft(aggregate, assetType, item, asset.description);
            asset.tags = splitTags(item.get("tags"));
            asset.status = AssetStatus.EXTRACTED;
            asset.metadata = new LinkedHashMap<>(item);
            asset.metadata.putIfAbsent("sourceId", stringValue(item.get("id"), asset.assetId));
            asset.metadata.putIfAbsent("sourceType", assetType.name());
            asset.createdAt = now;
            asset.updatedAt = now;
            created.add(asset);
        }

        if (created.isEmpty()) {
            created.add(buildFallbackAsset(projectId, assetType, aggregate, now));
        }

        aggregate.assets.addAll(created);
        aggregate.shots.forEach(shot -> shot.keyframeRefs = resolveSelectedKeyframesForShot(aggregate, shot));
        aggregate.project.status = ProjectStatus.ASSET_READY;
        finishPipeline(pipelineRun, PipelineStatus.SUCCESS, created.size(), 0, null);
        scriptProjectService.save(aggregate);
        return created;
    }

    public List<ExtractedAsset> listAssets(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).assets);
    }

    public ExtractedAsset updateAsset(String projectId, String assetId, UpdateAssetRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (request.name() != null && !request.name().isBlank()) {
            asset.name = request.name().trim();
        }
        if (request.description() != null && !request.description().isBlank()) {
            asset.description = request.description().trim();
        }
        if (request.tags() != null) {
            asset.tags = request.tags().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        if (request.promptDraft() != null) {
            asset.promptDraft = request.promptDraft().trim();
        }
        if (request.metadata() != null) {
            asset.metadata = new LinkedHashMap<>(request.metadata());
        }
        if (request.visualPrompt() != null) {
            String next = request.visualPrompt().trim();
            asset.promptVersions = promptVersionService.updateWithVersion(
                    asset.visualPrompt, next, asset.promptVersions, PromptVersionSource.MANUAL_EDIT, null);
            asset.visualPrompt = next.isEmpty() ? null : next;
        }
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return asset;
    }

    public List<KeyframeRecord> generateKeyframes(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        PipelineRun pipelineRun = beginPipeline(aggregate, PipelineType.KEYFRAME_GENERATION, "KEYFRAME_" + asset.assetType.name(), 2);
        aggregate.project.status = ProjectStatus.KEYFRAME_GENERATING;
        asset.status = AssetStatus.KEYFRAME_GENERATING;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);

        try {
            for (KeyframeRecord kf : new ArrayList<>(aggregate.keyframes)) {
                if (Objects.equals(kf.assetId, assetId) && kf.imageFileId != null && !kf.imageFileId.isBlank()) {
                    assetHistoryService.appendSnapshot(
                            projectId,
                            AssetHistoryType.KEYFRAME,
                            kf.keyframeId,
                            kf.imageFileId,
                            kf.promptText,
                            kf.modelName,
                            null
                    );
                }
            }
            List<KeyframeRecord> created = new ArrayList<>();
            String generationBatchId = nextId("keyframe-batch");
            for (int index = 1; index <= 2; index++) {
                String prompt = buildKeyframePrompt(aggregate, asset, index);
                StoredFileRecord imageFile = generateKeyframeImage(aggregate, asset, prompt, generationBatchId, index);
                scriptProjectService.upsertFile(aggregate, imageFile);

                KeyframeRecord record = new KeyframeRecord();
                record.keyframeId = nextId("kf");
                record.projectId = projectId;
                record.assetId = asset.assetId;
                record.promptText = prompt;
                record.promptVersions = promptVersionService.updateWithVersion(
                        null, prompt, record.promptVersions, PromptVersionSource.AI_GENERATED, null);
                record.negativePrompt = "blurry, distorted, watermark, low quality";
                record.imageFileId = imageFile.fileId;
                record.selected = false;
                record.status = "SUCCESS";
                record.modelName = aiCapabilityRoutingService.resolveImage(aggregate.project.explicitImageModel).modelName();
                record.createdAt = Instant.now();
                record.updatedAt = record.createdAt;
                created.add(record);
            }
            aggregate.keyframes.addAll(created);
            asset.status = AssetStatus.KEYFRAME_READY;
            aggregate.project.status = ProjectStatus.KEYFRAME_READY;
            finishPipeline(pipelineRun, PipelineStatus.SUCCESS, created.size(), 0, null);
            scriptProjectService.save(aggregate);
            return created;
        } catch (Exception ex) {
            asset.status = AssetStatus.FAILED;
            aggregate.project.status = ProjectStatus.FAILED;
            finishPipeline(pipelineRun, PipelineStatus.FAILED, 0, 2, ex.getMessage());
            scriptProjectService.save(aggregate);
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(500, "关键帧生成失败");
        }
    }

    public List<KeyframeRecord> listKeyframes(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).keyframes);
    }

    public KeyframeRecord confirmKeyframe(String projectId, String keyframeId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        KeyframeRecord target = findKeyframe(aggregate, keyframeId);
        for (KeyframeRecord record : aggregate.keyframes) {
            if (Objects.equals(record.assetId, target.assetId)) {
                record.selected = Objects.equals(record.keyframeId, keyframeId);
                record.updatedAt = Instant.now();
            }
        }
        ExtractedAsset asset = findAsset(aggregate, target.assetId);
        asset.status = AssetStatus.CONFIRMED;
        asset.updatedAt = Instant.now();
        aggregate.project.status = hasAnyConfirmedKeyframe(aggregate) ? ProjectStatus.KEYFRAME_READY : aggregate.project.status;
        aggregate.shots.forEach(shot -> shot.keyframeRefs = resolveSelectedKeyframesForShot(aggregate, shot));
        scriptProjectService.save(aggregate);
        return target;
    }

    public List<KeyframeRecord> regenerateKeyframe(String projectId, String keyframeId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        KeyframeRecord record = findKeyframe(aggregate, keyframeId);
        return generateKeyframes(projectId, record.assetId);
    }

    public KeyframeRecord updateKeyframePrompt(String projectId, String keyframeId, UpdateKeyframePromptRequest request) {
        if (request == null || request.promptText() == null) {
            throw new BizException(400, "promptText 不能为空");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        KeyframeRecord record = findKeyframe(aggregate, keyframeId);
        String next = request.promptText().trim();
        record.promptVersions = promptVersionService.updateWithVersion(
                record.promptText, next, record.promptVersions, PromptVersionSource.MANUAL_EDIT, null);
        record.promptText = next;
        record.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return record;
    }

    public ExtractedAsset rollbackAssetVisualPrompt(String projectId, String assetId, RollbackPromptRequest request) {
        if (request == null || request.versionId() == null || request.versionId().isBlank()) {
            throw new BizException(400, "versionId 不能为空");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        PromptVersion target = promptVersionService.findVersion(asset.promptVersions, request.versionId());
        if (target == null) {
            throw new BizException(400, "未找到指定版本");
        }
        asset.promptVersions = promptVersionService.rollbackAppend(asset.visualPrompt, asset.promptVersions, request.versionId());
        asset.visualPrompt = target.prompt();
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return asset;
    }

    public StoryboardShot rollbackShotVisualPrompt(String projectId, String shotId, RollbackPromptRequest request) {
        if (request == null || request.versionId() == null || request.versionId().isBlank()) {
            throw new BizException(400, "versionId 不能为空");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        StoryboardShot shot = findShot(aggregate, shotId);
        PromptVersion target = promptVersionService.findVersion(shot.promptVersions, request.versionId());
        if (target == null) {
            throw new BizException(400, "未找到指定版本");
        }
        shot.promptVersions = promptVersionService.rollbackAppend(shot.visualPrompt, shot.promptVersions, request.versionId());
        shot.visualPrompt = target.prompt();
        shot.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return shot;
    }

    public KeyframeRecord rollbackKeyframePrompt(String projectId, String keyframeId, RollbackPromptRequest request) {
        if (request == null || request.versionId() == null || request.versionId().isBlank()) {
            throw new BizException(400, "versionId 不能为空");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        KeyframeRecord record = findKeyframe(aggregate, keyframeId);
        PromptVersion target = promptVersionService.findVersion(record.promptVersions, request.versionId());
        if (target == null) {
            throw new BizException(400, "未找到指定版本");
        }
        record.promptVersions = promptVersionService.rollbackAppend(record.promptText, record.promptVersions, request.versionId());
        record.promptText = target.prompt();
        record.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return record;
    }

    // -------------------------------------------------------------------------
    // Visual prompt system B-1 ~ B-9
    // -------------------------------------------------------------------------

    public ArtDirectionResponse generateArtDirection(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        if (structuredScript.isEmpty()) {
            throw new BizException(400, "请先完善剧本，再生成美术指导");
        }
        String title = stringValue(structuredScript.get("title"), aggregate.project.name);
        String genre = stringValue(structuredScript.get("genre"), "剧情");
        String logline = stringValue(structuredScript.get("summary"), stringValue(aggregate.project.scriptSummary, ""));
        String language = stringValue(aggregate.project.language, "中文");
        String visualStyle = resolveVisualStyleAnchor(aggregate.project.visualStyle);
        String stylePrompt = stringValue(aggregate.project.aspectRatio, "16:9") + " 画幅，统一叙事光影";

        String characterList = buildCharacterLinesForArtDirection(structuredScript);
        String sceneList = buildSceneLinesForArtDirection(structuredScript);

        String systemPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/art-direction-system.md",
                Map.of(),
                "You are a world-class Art Director."
        );
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/art-direction-user.md",
                Map.of(
                        "title", title,
                        "genre", genre,
                        "logline", logline,
                        "visualStyle", visualStyle,
                        "stylePrompt", stylePrompt,
                        "language", language,
                        "characterList", characterList,
                        "sceneList", sceneList
                ),
                ""
        );

        String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.ART_DIRECTION, systemPrompt, userPrompt);
        String json = stripJsonFence(raw);
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BizException(502, "美术指导 JSON 解析失败，请重试");
        }
        aggregate.project.artDirectionJson = json;
        aggregate.project.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new ArtDirectionResponse(json);
    }

    public ArtDirectionResponse getArtDirection(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        return new ArtDirectionResponse(aggregate.project.artDirectionJson);
    }

    public BatchVisualPromptResponse batchGenerateCharacterVisualPrompts(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        if (aggregate.project.artDirectionJson == null || aggregate.project.artDirectionJson.isBlank()) {
            throw new BizException(400, "请先生成美术指导（B-1）");
        }
        List<ExtractedAsset> characters = aggregate.assets.stream()
                .filter(a -> a.assetType == AssetType.CHARACTER)
                .toList();
        if (characters.isEmpty()) {
            throw new BizException(400, "暂无角色资产，请先抽取角色");
        }
        Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
        Map<String, Object> colorPalette = nestedMap(ad, "colorPalette");
        Map<String, Object> designRules = nestedMap(ad, "characterDesignRules");
        String moodJoined = formatMoodKeywords(ad.get("moodKeywords"));

        String characterList = characters.stream()
                .map(this::formatCharacterLineForBatch)
                .collect(Collectors.joining("\n"));

        Map<String, Object> batchVars = new LinkedHashMap<>();
        batchVars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        batchVars.put("n", String.valueOf(characters.size()));
        batchVars.put("consistencyAnchors", stringValue(ad.get("consistencyAnchors"), ""));
        batchVars.put("primary", stringValue(colorPalette.get("primary"), ""));
        batchVars.put("secondary", stringValue(colorPalette.get("secondary"), ""));
        batchVars.put("accent", stringValue(colorPalette.get("accent"), ""));
        batchVars.put("skinTones", stringValue(colorPalette.get("skinTones"), ""));
        batchVars.put("saturation", stringValue(colorPalette.get("saturation"), ""));
        batchVars.put("temperature", stringValue(colorPalette.get("temperature"), ""));
        batchVars.put("proportions", stringValue(designRules.get("proportions"), ""));
        batchVars.put("eyeStyle", stringValue(designRules.get("eyeStyle"), ""));
        batchVars.put("lineWeight", stringValue(designRules.get("lineWeight"), ""));
        batchVars.put("detailLevel", stringValue(designRules.get("detailLevel"), ""));
        batchVars.put("lightingStyle", stringValue(ad.get("lightingStyle"), ""));
        batchVars.put("textureStyle", stringValue(ad.get("textureStyle"), ""));
        batchVars.put("moodKeywords", moodJoined);
        batchVars.put("genre", stringValue(loadStructuredScript(aggregate).get("genre"), "剧情"));
        batchVars.put("stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9") + " 画幅");
        batchVars.put("characterList", characterList);
        batchVars.put("language", stringValue(aggregate.project.language, "中文"));
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/batch-character-user.md",
                batchVars,
                ""
        );
        String systemPrompt = "You output ONLY valid JSON matching the schema in the user message. No markdown.";
        String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.CHARACTER_VISUAL_PROMPT, systemPrompt, userPrompt);
        String json = stripJsonFence(raw);
        Map<String, Object> root = parseJsonObject(json);
        Object charsNode = root.get("characters");
        if (!(charsNode instanceof List<?> list)) {
            throw new BizException(502, "批量角色视觉提示词格式错误");
        }
        List<VisualPromptResponse> items = new ArrayList<>();
        for (Object row : list) {
            if (!(row instanceof Map<?, ?> m)) {
                continue;
            }
            String sourceId = stringValue(m.get("id"), "");
            String vp = stringValue(m.get("visualPrompt"), "");
            ExtractedAsset asset = findCharacterAssetBySourceOrId(aggregate, sourceId, characters);
            if (asset != null && !vp.isBlank()) {
                asset.promptVersions = promptVersionService.updateWithVersion(
                        asset.visualPrompt, vp, asset.promptVersions, PromptVersionSource.AI_GENERATED, null);
                asset.visualPrompt = vp;
                asset.updatedAt = Instant.now();
                items.add(new VisualPromptResponse(asset.assetId, vp));
            }
        }
        scriptProjectService.save(aggregate);
        if (items.isEmpty()) {
            throw new BizException(502, "未能将模型返回的角色 id 与资产匹配，请重试或检查剧本角色 id");
        }
        return new BatchVisualPromptResponse(items);
    }

    public VisualPromptResponse generateAssetVisualPrompt(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        String genre = stringValue(structuredScript.get("genre"), "剧情");
        String language = stringValue(aggregate.project.language, "中文");
        String visualStyle = resolveVisualStyleAnchor(aggregate.project.visualStyle);
        String stylePrompt = stringValue(aggregate.project.aspectRatio, "16:9") + " 画幅";
        String artBlock = buildArtDirectionAnchorBlock(aggregate);

        String systemPrompt = "You output ONLY the final prompt text requested. No labels, no quotes, no markdown.";
        String userPrompt;
        switch (asset.assetType) {
            case CHARACTER -> userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                    "prompts/visual/single-character-user.md",
                    Map.of(
                            "artDirectionBlock", artBlock,
                            "visualStyle", visualStyle,
                            "name", stringValue(asset.name, "角色"),
                            "gender", stringValue(asset.metadata.get("gender"), "未知"),
                            "age", stringValue(asset.metadata.get("age"), "未知"),
                            "personality", stringValue(asset.metadata.get("personality"), stringValue(asset.metadata.get("traits"), "未知")),
                            "genre", genre,
                            "language", language,
                            "stylePrompt", stylePrompt
                    ),
                    ""
            );
            case BACKGROUND -> userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                    "prompts/visual/single-scene-user.md",
                    Map.of(
                            "artDirectionBlock", artBlock,
                            "visualStyle", visualStyle,
                            "location", stringValue(asset.metadata.get("location"), asset.name),
                            "time", stringValue(asset.metadata.get("time"), "白天"),
                            "atmosphere", stringValue(asset.metadata.get("atmosphere"), "平稳"),
                            "genre", genre,
                            "language", language,
                            "stylePrompt", stylePrompt
                    ),
                    ""
            );
            case PROP -> userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                    "prompts/visual/prop-user.md",
                    Map.of(
                            "artDirectionBlock", artBlock,
                            "visualStyle", visualStyle,
                            "name", stringValue(asset.name, "道具"),
                            "category", stringValue(asset.metadata.get("category"), "道具"),
                            "description", stringValue(asset.description, ""),
                            "genre", genre,
                            "language", language,
                            "stylePrompt", stylePrompt
                    ),
                    ""
            );
            default -> throw new BizException(400, "未知资产类型");
        }
        String text = normalizeSingleLinePrompt(invokeTextModelOrThrow(aggregate, WorkflowModelKey.CHARACTER_VISUAL_PROMPT, systemPrompt, userPrompt));
        if (text.isBlank()) {
            throw new BizException(502, "模型未返回可用视觉提示词");
        }
        asset.promptVersions = promptVersionService.updateWithVersion(
                asset.visualPrompt, text, asset.promptVersions, PromptVersionSource.AI_GENERATED, null);
        asset.visualPrompt = text;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new VisualPromptResponse(asset.assetId, text);
    }

    public TurnaroundPlanResponse generateTurnaroundPlan(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.assetType != AssetType.CHARACTER) {
            throw new BizException(400, "九宫格规划仅支持角色资产");
        }
        if (asset.visualPrompt == null || asset.visualPrompt.isBlank()) {
            throw new BizException(400, "请先生成角色视觉提示词（B-3）");
        }
        String artBlock = buildArtDirectionAnchorBlock(aggregate);
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/turnaround-plan-user.md",
                Map.of(
                        "artDirectionBlock", artBlock,
                        "visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle),
                        "name", stringValue(asset.name, "角色"),
                        "gender", stringValue(asset.metadata.get("gender"), ""),
                        "age", stringValue(asset.metadata.get("age"), ""),
                        "personality", stringValue(asset.metadata.get("personality"), ""),
                        "visualPrompt", asset.visualPrompt,
                        "stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9")
                ),
                ""
        );
        String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.TURNAROUND_PLAN, "Output ONLY valid JSON as specified.", userPrompt);
        String json = stripJsonFence(raw);
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BizException(502, "九宫格规划 JSON 解析失败");
        }
        asset.turnaroundPlanJson = json;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new TurnaroundPlanResponse(asset.assetId, json);
    }

    public TurnaroundImageResponse generateTurnaroundImage(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.assetType != AssetType.CHARACTER) {
            throw new BizException(400, "九宫格图仅支持角色资产");
        }
        if (asset.turnaroundPlanJson == null || asset.turnaroundPlanJson.isBlank()) {
            throw new BizException(400, "请先生成九宫格视角规划（B-6）");
        }
        Map<String, Object> planRoot = parseJsonObject(asset.turnaroundPlanJson);
        Object panelsNode = planRoot.get("panels");
        if (!(panelsNode instanceof List<?> panelList) || panelList.size() < 9) {
            throw new BizException(400, "九宫格规划数据不完整");
        }
        List<Map<String, Object>> panels = new ArrayList<>();
        for (Object p : panelList) {
            if (p instanceof Map<?, ?> pm) {
                Map<String, Object> m = new LinkedHashMap<>();
                pm.forEach((k, v) -> m.put(String.valueOf(k), v));
                panels.add(m);
            }
        }
        panels.sort((a, b) -> Integer.compare(intValue(a.get("index"), 0), intValue(b.get("index"), 0)));
        String[] descriptions = new String[9];
        for (int i = 0; i < 9 && i < panels.size(); i++) {
            descriptions[i] = stringValue(panels.get(i).get("description"), "full body character view");
        }
        String characterSummary = shortenWords(asset.visualPrompt, 40);
        String artSuffix = "";
        if (aggregate.project.artDirectionJson != null && !aggregate.project.artDirectionJson.isBlank()) {
            Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
            String anchors = stringValue(ad.get("consistencyAnchors"), "");
            if (!anchors.isBlank()) {
                artSuffix = "\nArt Direction consistency: " + anchors;
            }
        }
        Map<String, Object> turnImageVars = new LinkedHashMap<>();
        turnImageVars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        turnImageVars.put("stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9"));
        turnImageVars.put("characterName", stringValue(asset.name, "角色"));
        turnImageVars.put("characterSummary", characterSummary);
        turnImageVars.put("description0", descriptions[0]);
        turnImageVars.put("description1", descriptions[1]);
        turnImageVars.put("description2", descriptions[2]);
        turnImageVars.put("description3", descriptions[3]);
        turnImageVars.put("description4", descriptions[4]);
        turnImageVars.put("description5", descriptions[5]);
        turnImageVars.put("description6", descriptions[6]);
        turnImageVars.put("description7", descriptions[7]);
        turnImageVars.put("description8", descriptions[8]);
        turnImageVars.put("artDirectionSuffix", artSuffix);
        String imagePrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/turnaround-image-user.md",
                turnImageVars,
                ""
        );
        if (asset.turnaroundImageFileId != null && !asset.turnaroundImageFileId.isBlank()) {
            assetHistoryService.appendSnapshot(
                    projectId,
                    AssetHistoryType.TURNAROUND,
                    asset.assetId,
                    asset.turnaroundImageFileId,
                    imagePrompt,
                    null,
                    null
            );
        }
        String basePath = "turnaround/" + asset.assetId + "/character-sheet";
        StoredFileRecord file = generateStoredImage(aggregate, WorkflowModelKey.TURNAROUND_IMAGE, imagePrompt, basePath + ".png", asset.name);
        scriptProjectService.upsertFile(aggregate, file);
        asset.turnaroundImageFileId = file.fileId;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new TurnaroundImageResponse(asset.assetId, file.fileId);
    }

    public StoryboardPlanResponse generateStoryboardPlan(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.visualPrompt == null || asset.visualPrompt.isBlank()) {
            throw new BizException(400, "请先生成资产视觉提示词");
        }
        Map<String, Object> vars = buildStoryboardCommonVars(aggregate, asset, STORYBOARD_PANEL_COUNT);
        String systemPrompt = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-split-system.md", vars, "");
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-split-user.md", vars, "");
        String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.STORYBOARD_PLAN, systemPrompt, userPrompt);
        String json = stripJsonFence(raw);
        validateStoryboardPanelsJson(json, STORYBOARD_PANEL_COUNT);
        asset.storyboardPlanJson = json;
        asset.storyboardTranslationsJson = translateStoryboardPanelsOrFallback(aggregate, json, STORYBOARD_PANEL_COUNT);
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new StoryboardPlanResponse(asset.assetId, asset.storyboardPlanJson, asset.storyboardTranslationsJson);
    }

    public StoryboardPlanResponse translateStoryboardPlan(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.storyboardPlanJson == null || asset.storyboardPlanJson.isBlank()) {
            throw new BizException(400, "请先生成九宫格分镜规划");
        }
        validateStoryboardPanelsJson(asset.storyboardPlanJson, STORYBOARD_PANEL_COUNT);
        asset.storyboardTranslationsJson = translateStoryboardPanelsOrFallback(aggregate, asset.storyboardPlanJson, STORYBOARD_PANEL_COUNT);
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new StoryboardPlanResponse(asset.assetId, asset.storyboardPlanJson, asset.storyboardTranslationsJson);
    }

    public StoryboardPlanResponse rewriteStoryboardPlan(String projectId, String assetId, StoryboardRewriteRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.storyboardPlanJson == null || asset.storyboardPlanJson.isBlank()) {
            throw new BizException(400, "请先生成九宫格分镜规划");
        }
        String instruction = stringValue(request == null ? null : request.instruction(), "").trim();
        if (instruction.isBlank()) {
            throw new BizException(400, "改写指令不能为空");
        }
        Map<String, Object> vars = buildStoryboardCommonVars(aggregate, asset, STORYBOARD_PANEL_COUNT);
        vars.put("instruction", instruction);
        vars.put("panelsJson", asset.storyboardPlanJson);
        vars.put("expectedCount", STORYBOARD_PANEL_COUNT);
        vars.put("expectedCountMinusOne", STORYBOARD_PANEL_COUNT - 1);
        vars.put("requiredShotSizeKinds", 3);
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-rewrite-user.md", vars, "");
        String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.STORYBOARD_PLAN, "Output ONLY valid JSON as specified.", userPrompt);
        String json = stripJsonFence(raw);
        validateStoryboardPanelsJson(json, STORYBOARD_PANEL_COUNT);
        asset.storyboardPlanJson = json;
        asset.storyboardTranslationsJson = translateStoryboardPanelsOrFallback(aggregate, json, STORYBOARD_PANEL_COUNT);
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new StoryboardPlanResponse(asset.assetId, asset.storyboardPlanJson, asset.storyboardTranslationsJson);
    }

    public StoryboardImageResponse generateStoryboardImage(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.storyboardPlanJson == null || asset.storyboardPlanJson.isBlank()) {
            throw new BizException(400, "请先生成九宫格分镜规划");
        }
        List<Map<String, Object>> panels = parseStoryboardPanels(asset.storyboardPlanJson, STORYBOARD_PANEL_COUNT);
        Map<String, Object> vars = buildStoryboardCommonVars(aggregate, asset, STORYBOARD_PANEL_COUNT);
        String prefix = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-image-prefix.md", vars, "");
        String suffix = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-image-suffix.md", vars, "");
        String noText = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-image-no-text.md", vars, "");
        StringBuilder panelPrompt = new StringBuilder();
        for (int i = 0; i < STORYBOARD_PANEL_COUNT; i++) {
            Map<String, Object> panel = i < panels.size() ? panels.get(i) : Map.of();
            Map<String, Object> panelVars = new LinkedHashMap<>();
            panelVars.put("displayIndex", i + 1);
            panelVars.put("position", panelPosition(i));
            panelVars.put("shotSize", stringValue(panel.get("shotSize"), "中景"));
            panelVars.put("cameraAngle", stringValue(panel.get("cameraAngle"), "平视"));
            panelVars.put("description", stringValue(panel.get("description"), "Cinematic frame with coherent action continuity."));
            panelPrompt.append(promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-image-panel.md", panelVars, ""));
        }
        String fullPrompt = (prefix + "\n" + panelPrompt + "\n" + suffix + "\n\n" + noText).trim();
        if (asset.storyboardImageFileId != null && !asset.storyboardImageFileId.isBlank()) {
            assetHistoryService.appendSnapshot(
                    projectId,
                    AssetHistoryType.STORYBOARD,
                    asset.assetId,
                    asset.storyboardImageFileId,
                    fullPrompt,
                    null,
                    null
            );
        }
        StoredFileRecord file = generateStoredImage(
                aggregate,
                WorkflowModelKey.STORYBOARD_IMAGE,
                fullPrompt,
                "storyboard/" + asset.assetId + "/grid.png",
                asset.name
        );
        scriptProjectService.upsertFile(aggregate, file);
        asset.storyboardPromptText = fullPrompt;
        asset.storyboardImageFileId = file.fileId;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new StoryboardImageResponse(asset.assetId, file.fileId, fullPrompt);
    }

    public StoryboardPanelCropResponse cropStoryboardPanel(String projectId, String assetId, int panelIndex) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        String cropFileId = cropStoryboardPanelInternal(aggregate, asset, panelIndex);
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new StoryboardPanelCropResponse(asset.assetId, panelIndex, cropFileId);
    }

    public StoryboardFirstFrameResponse applyStoryboardFirstFrame(
            String projectId,
            String shotId,
            ApplyStoryboardFirstFrameRequest request
    ) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        StoryboardShot shot = findShot(aggregate, shotId);
        String mode = stringValue(request == null ? null : request.mode(), FIRST_FRAME_MODE_FULL_GRID).trim().toUpperCase(Locale.ROOT);
        if (FIRST_FRAME_MODE_NONE.equals(mode)) {
            shot.firstFrameMode = FIRST_FRAME_MODE_NONE;
            shot.storyboardAssetId = null;
            shot.storyboardImageFileId = null;
            shot.storyboardCropFileId = null;
            shot.storyboardCropIndex = null;
            shot.updatedAt = Instant.now();
            scriptProjectService.save(aggregate);
            return new StoryboardFirstFrameResponse(shot.shotId, FIRST_FRAME_MODE_NONE, null, null, null);
        }

        String assetId = stringValue(request == null ? null : request.assetId(), "").trim();
        if (assetId.isBlank()) {
            throw new BizException(400, "请选择要应用的九宫格资产");
        }
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.storyboardImageFileId == null || asset.storyboardImageFileId.isBlank()) {
            throw new BizException(400, "该资产尚未生成九宫格分镜图");
        }

        shot.storyboardAssetId = asset.assetId;
        shot.storyboardImageFileId = asset.storyboardImageFileId;
        shot.storyboardCropFileId = null;
        shot.storyboardCropIndex = null;
        shot.firstFrameMode = FIRST_FRAME_MODE_FULL_GRID;

        if (FIRST_FRAME_MODE_CROPPED_PANEL.equals(mode)) {
            int panelIndex = intValue(request == null ? null : request.panelIndex(), 0);
            String cropFileId = cropStoryboardPanelInternal(aggregate, asset, panelIndex);
            shot.storyboardCropFileId = cropFileId;
            shot.storyboardCropIndex = panelIndex;
            shot.firstFrameMode = FIRST_FRAME_MODE_CROPPED_PANEL;
        }
        shot.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        String selectedImageFileId = FIRST_FRAME_MODE_CROPPED_PANEL.equals(shot.firstFrameMode)
                ? shot.storyboardCropFileId
                : shot.storyboardImageFileId;
        return new StoryboardFirstFrameResponse(
                shot.shotId,
                shot.firstFrameMode,
                shot.storyboardAssetId,
                shot.storyboardCropIndex,
                selectedImageFileId
        );
    }

    public ThreeViewResponse generateThreeView(String projectId, String assetId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        ExtractedAsset asset = findAsset(aggregate, assetId);
        if (asset.visualPrompt == null || asset.visualPrompt.isBlank()) {
            throw new BizException(400, "请先生成资产视觉提示词");
        }
        String summary = shortenWords(asset.visualPrompt, 40);
        String artSuffix = "";
        if (aggregate.project.artDirectionJson != null && !aggregate.project.artDirectionJson.isBlank()) {
            Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
            String anchors = stringValue(ad.get("consistencyAnchors"), "");
            if (!anchors.isBlank()) {
                artSuffix = "\nArt Direction consistency: " + anchors;
            }
        }
        String templatePath;
        String summaryKey;
        switch (asset.assetType) {
            case CHARACTER -> {
                templatePath = "prompts/visual/three-view-character-user.md";
                summaryKey = "characterSummary";
            }
            case BACKGROUND -> {
                templatePath = "prompts/visual/three-view-background-user.md";
                summaryKey = "sceneSummary";
            }
            case PROP -> {
                templatePath = "prompts/visual/three-view-prop-user.md";
                summaryKey = "propSummary";
            }
            default -> throw new BizException(400, "未知资产类型");
        }
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        vars.put("stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9"));
        vars.put("name", stringValue(asset.name, "资产"));
        vars.put(summaryKey, summary);
        vars.put("artDirectionSuffix", artSuffix);
        String imagePrompt = promptTemplateService.renderForProject(aggregate.project, templatePath, vars, "");
        if (asset.threeViewImageFileId != null && !asset.threeViewImageFileId.isBlank()) {
            assetHistoryService.appendSnapshot(
                    projectId,
                    AssetHistoryType.THREE_VIEW,
                    asset.assetId,
                    asset.threeViewImageFileId,
                    imagePrompt,
                    null,
                    null
            );
        }
        String basePath = "three-view/" + asset.assetId + "/sheet";
        StoredFileRecord file = generateStoredImage(aggregate, WorkflowModelKey.THREE_VIEW_IMAGE, imagePrompt, basePath + ".png", asset.name);
        scriptProjectService.upsertFile(aggregate, file);
        asset.threeViewImageFileId = file.fileId;
        asset.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new ThreeViewResponse(asset.assetId, file.fileId);
    }

    public GroupSceneResponse generateGroupScene(String projectId, GenerateGroupSceneRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        if (aggregate.project.artDirectionJson == null || aggregate.project.artDirectionJson.isBlank()) {
            throw new BizException(400, "请先生成美术指导（B-1）");
        }
        Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
        String anchors = stringValue(ad.get("consistencyAnchors"), "");
        if (anchors.isBlank()) {
            throw new BizException(400, "美术指导缺少 consistencyAnchors");
        }
        List<ExtractedAsset> selected = new ArrayList<>();
        for (String id : request.characterAssetIds()) {
            ExtractedAsset a = findAsset(aggregate, id);
            if (a.assetType != AssetType.CHARACTER) {
                throw new BizException(400, "仅可选择角色资产：" + id);
            }
            selected.add(a);
        }
        String characterSceneList = selected.stream()
                .map(a -> a.name + " | 中景 | 站立互动 | " + stringValue(a.metadata.get("personality"), "中性"))
                .collect(Collectors.joining("\n"));
        String characterVisualList = selected.stream()
                .map(a -> a.name + " | " + shortenWords(stringValue(a.visualPrompt, a.description), 35))
                .collect(Collectors.joining("\n"));

        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        Map<String, Object> groupVars = new LinkedHashMap<>();
        groupVars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        groupVars.put("consistencyAnchors", anchors);
        groupVars.put("location", stringValue(request.location(), "未指定"));
        groupVars.put("time", stringValue(request.time(), "白天"));
        groupVars.put("atmosphere", stringValue(request.atmosphere(), "电影感"));
        groupVars.put("genre", stringValue(structuredScript.get("genre"), "剧情"));
        groupVars.put("characterSceneList", characterSceneList);
        groupVars.put("characterVisualList", characterVisualList);
        groupVars.put("n", String.valueOf(selected.size()));
        groupVars.put("language", stringValue(aggregate.project.language, "中文"));
        groupVars.put("stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9") + " ensemble");
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/group-scene-user.md",
                groupVars,
                ""
        );
        String promptText = normalizeSingleLinePrompt(invokeTextModelOrThrow(
                aggregate,
                WorkflowModelKey.GROUP_SCENE_IMAGE,
                "Output ONLY the final image prompt paragraph. No labels.",
                userPrompt
        ));
        if (promptText.isBlank()) {
            throw new BizException(502, "群像提示词生成失败");
        }
        String basePath = "group-scene/" + projectId + "/" + System.currentTimeMillis();
        StoredFileRecord promptFile = localAssetFileService.storeText(
                projectId,
                basePath + ".txt",
                "text/plain; charset=UTF-8",
                promptText
        );
        scriptProjectService.upsertFile(aggregate, promptFile);
        String imageFileId = null;
        if (Boolean.TRUE.equals(request.generateImage())) {
            StoredFileRecord file = generateStoredImage(aggregate, WorkflowModelKey.GROUP_SCENE_IMAGE, promptText, basePath + ".png", "群像");
            scriptProjectService.upsertFile(aggregate, file);
            imageFileId = file.fileId;
        }
        scriptProjectService.save(aggregate);
        return new GroupSceneResponse(promptText, imageFileId);
    }

    public ShotVisualPromptResponse generateShotVisualPrompt(String projectId, String shotId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        StoryboardShot shot = findShot(aggregate, shotId);
        if (aggregate.project.artDirectionJson == null || aggregate.project.artDirectionJson.isBlank()) {
            throw new BizException(400, "请先生成美术指导（B-1）");
        }
        Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
        String anchors = stringValue(ad.get("consistencyAnchors"), "");
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        Map<String, String> sceneCtx = resolveSceneContextForShot(aggregate, shot, structuredScript);

        String shotCharacterList = buildShotCharacterLines(aggregate, shot);
        String shotType = stringValue(shot.shotType, "").isBlank() ? inferShotType(shot) : stringValue(shot.shotType, "");
        String cameraMove = stringValue(shot.cameraMove, "").isBlank()
                ? stringValue(shot.cameraMovement, "static静止")
                : stringValue(shot.cameraMove, "");
        String emotion = stringValue(shot.emotion, "").isBlank() ? "戏剧情绪" : stringValue(shot.emotion, "");

        Map<String, Object> shotVars = new LinkedHashMap<>();
        shotVars.put("consistencyAnchors", anchors);
        shotVars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        shotVars.put("sceneTitle", stringValue(shot.title, "镜头"));
        shotVars.put("shotNumber", String.valueOf(shot.sequenceNo == null ? 0 : shot.sequenceNo));
        shotVars.put("shotType", shotType);
        shotVars.put("cameraMove", cameraMove);
        shotVars.put("action", stringValue(shot.actionSummary, stringValue(shot.scriptText, "")));
        shotVars.put("duration", String.valueOf(shot.targetDurationSec == null ? 3 : shot.targetDurationSec));
        shotVars.put("emotion", emotion);
        shotVars.put("shotCharacterList", shotCharacterList);
        shotVars.put("location", sceneCtx.getOrDefault("location", ""));
        shotVars.put("time", sceneCtx.getOrDefault("time", ""));
        shotVars.put("atmosphere", sceneCtx.getOrDefault("atmosphere", ""));
        shotVars.put("language", stringValue(aggregate.project.language, "中文"));
        shotVars.put("stylePrompt", stringValue(aggregate.project.aspectRatio, "16:9"));
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/visual/shot-storyboard-user.md",
                shotVars,
                ""
        );
        String text = normalizeSingleLinePrompt(invokeTextModelOrThrow(
                aggregate,
                WorkflowModelKey.SHOT_VISUAL_PROMPT,
                "Output ONLY the final storyboard shot prompt. No labels.",
                userPrompt
        ));
        if (text.isBlank()) {
            throw new BizException(502, "分镜提示词生成失败");
        }
        shot.promptVersions = promptVersionService.updateWithVersion(
                shot.visualPrompt, text, shot.promptVersions, PromptVersionSource.AI_GENERATED, null);
        shot.visualPrompt = text;
        shot.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return new ShotVisualPromptResponse(shot.shotId, text);
    }

    public StoryboardShot updateShot(String projectId, String shotId, UpdateShotRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        StoryboardShot shot = findShot(aggregate, shotId);
        if (request == null) {
            return shot;
        }
        if (request.shotType() != null) {
            shot.shotType = request.shotType().trim();
        }
        if (request.cameraMove() != null) {
            shot.cameraMove = request.cameraMove().trim();
        }
        if (request.emotion() != null) {
            shot.emotion = request.emotion().trim();
        }
        if (request.targetDurationSec() != null) {
            shot.targetDurationSec = request.targetDurationSec();
        }
        if (request.visualPrompt() != null) {
            String next = request.visualPrompt().trim();
            shot.promptVersions = promptVersionService.updateWithVersion(
                    shot.visualPrompt, next, shot.promptVersions, PromptVersionSource.MANUAL_EDIT, null);
            shot.visualPrompt = next.isEmpty() ? null : next;
        }
        shot.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return shot;
    }

    public List<StoryboardShot> splitShots(String projectId) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        Map<String, Object> structuredScript = loadStructuredScript(aggregate);
        if (structuredScript.isEmpty()) {
            throw new BizException(400, "请先完善剧本，再拆分镜头");
        }

        PipelineRun pipelineRun = beginPipeline(aggregate, PipelineType.SHOT_SPLIT, "SHOT_SPLIT", 1);
        List<Map<String, Object>> segments = asMapList(structuredScript.get("segments"));
        if (segments.isEmpty()) {
            segments = fallbackSegments(structuredScript, scriptProjectService.readText(scriptProjectService.findFile(aggregate, aggregate.project.refinedScriptFileId)));
        }

        int targetDuration = aggregate.project.targetDuration == null ? 15 : aggregate.project.targetDuration;
        List<Integer> allocatedDurationsSec = allocateSegmentTargetDurationsSec(segments, targetDuration);

        aggregate.shots.clear();
        Instant now = Instant.now();
        for (int index = 0; index < segments.size(); index++) {
            Map<String, Object> segment = segments.get(index);
            StoryboardShot shot = new StoryboardShot();
            shot.shotId = nextId("shot");
            shot.projectId = projectId;
            shot.sequenceNo = index + 1;
            shot.title = stringValue(segment.get("title"), "镜头 " + (index + 1));
            shot.scriptText = stringValue(segment.get("scriptText"), stringValue(segment.get("content"), ""));
            shot.actionSummary = stringValue(segment.get("actionSummary"), summarizeSegment(shot.scriptText));
            shot.cameraMovement = stringValue(segment.get("cameraMovement"), suggestCameraMovement(index));
            shot.targetDurationSec = allocatedDurationsSec.size() > index ? allocatedDurationsSec.get(index) : null;
            shot.characterRefs = mapSourceIdsToAssetIds(aggregate, AssetType.CHARACTER, stringList(segment.get("characterIds")));
            shot.backgroundRefs = mapSourceIdsToAssetIds(aggregate, AssetType.BACKGROUND, stringList(segment.get("backgroundIds")));
            shot.propRefs = mapSourceIdsToAssetIds(aggregate, AssetType.PROP, stringList(segment.get("propIds")));
            shot.keyframeRefs = resolveSelectedKeyframesForAssetRefs(aggregate, shot.characterRefs, shot.backgroundRefs, shot.propRefs);
            shot.status = "READY";
            shot.createdAt = now;
            shot.updatedAt = now;
            aggregate.shots.add(shot);
        }

        finishPipeline(pipelineRun, PipelineStatus.SUCCESS, aggregate.shots.size(), 0, null);
        scriptProjectService.save(aggregate);
        return new ArrayList<>(aggregate.shots);
    }

    private List<Integer> allocateSegmentTargetDurationsSec(List<Map<String, Object>> segments, int targetDurationSec) {
        int n = segments == null ? 0 : segments.size();
        if (n == 0) {
            return List.of();
        }

        List<Integer> estimates = new ArrayList<>(n);
        long sumEst = 0;
        boolean allPresent = true;
        for (Map<String, Object> segment : segments) {
            Integer est = parseEstimatedDurationSec(segment == null ? null : segment.get("estimatedDurationSec"));
            if (est == null || est <= 0) {
                allPresent = false;
                break;
            }
            estimates.add(est);
            sumEst += est;
        }

        if (allPresent && sumEst > 0) {
            return allocateByProportion(estimates, targetDurationSec, sumEst);
        }

        return allocateEqual(n, targetDurationSec);
    }

    private Integer parseEstimatedDurationSec(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            int v = (int) Math.round(number.doubleValue());
            return v > 0 ? v : null;
        }
        if (raw instanceof String s) {
            String t = s.trim();
            if (t.isEmpty()) {
                return null;
            }
            try {
                int v = Integer.parseInt(t);
                return v > 0 ? v : null;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private List<Integer> allocateByProportion(List<Integer> estimates, int targetDurationSec, long sumEst) {
        int n = estimates.size();
        double[] fractions = new double[n];
        List<Integer> allocated = new ArrayList<>(n);
        long baseSum = 0;
        for (int i = 0; i < n; i++) {
            double exact = (double) estimates.get(i) * (double) targetDurationSec / (double) sumEst;
            int base = (int) Math.floor(exact);
            allocated.add(base);
            fractions[i] = exact - base;
            baseSum += base;
        }

        int remainder = targetDurationSec - (int) baseSum;
        if (remainder > 0) {
            List<Integer> indices = new ArrayList<>(n);
            for (int i = 0; i < n; i++) indices.add(i);
            indices.sort((a, b) -> Double.compare(fractions[b], fractions[a]));
            for (int i = 0; i < indices.size() && remainder > 0; i++) {
                allocated.set(indices.get(i), allocated.get(indices.get(i)) + 1);
                remainder--;
            }
        }

        clampMinOnePreserveSumAsPossible(allocated, targetDurationSec);
        return allocated;
    }

    private void clampMinOnePreserveSumAsPossible(List<Integer> allocated, int targetDurationSec) {
        int n = allocated.size();
        int currentSum = allocated.stream().mapToInt(Integer::intValue).sum();
        // If all are already >= 1, no need to adjust.
        boolean hasNonPositive = false;
        for (int i = 0; i < n; i++) {
            if (allocated.get(i) == null || allocated.get(i) <= 0) {
                hasNonPositive = true;
                allocated.set(i, 1);
            }
        }
        if (!hasNonPositive) {
            return;
        }

        currentSum = allocated.stream().mapToInt(Integer::intValue).sum();
        int toReduce = currentSum - targetDurationSec;
        if (toReduce <= 0) {
            return;
        }

        List<Integer> indices = new ArrayList<>(n);
        for (int i = 0; i < n; i++) indices.add(i);
        indices.sort((a, b) -> Integer.compare(allocated.get(b), allocated.get(a)));

        for (int idx : indices) {
            while (toReduce > 0 && allocated.get(idx) > 1) {
                allocated.set(idx, allocated.get(idx) - 1);
                toReduce--;
            }
            if (toReduce <= 0) {
                break;
            }
        }
    }

    private List<Integer> allocateEqual(int n, int targetDurationSec) {
        if (n <= 0) {
            return List.of();
        }
        int each = targetDurationSec / n;
        int rem = targetDurationSec % n;
        List<Integer> allocated = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int v = each + (i < rem ? 1 : 0);
            allocated.add(v);
        }
        // Ensure non-zero when possible.
        clampMinOnePreserveSumAsPossible(allocated, targetDurationSec);
        return allocated;
    }

    public List<StoryboardShot> listShots(String projectId) {
        return new ArrayList<>(scriptProjectService.require(projectId).shots);
    }

    private Map<String, Object> buildStructuredScript(ScriptProjectAggregate aggregate, String originalText, String briefPrompt) {
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(aggregate.project.explicitTextModel);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String requestedModel = stringValue(aggregate.project.explicitTextModel, "");
            String reason = requestedModel.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + requestedModel;
            persistRefineFailureContext(aggregate, resolvedModel, reason, null);
            throw new BizException(400, reason);
        }

        String systemPrompt = promptTemplateService.renderForProject(aggregate.project, 
                "prompts/script/refine-system.md",
                Map.of("language", stringValue(aggregate.project.language, "中文")),
                "你是一名专业的影视编剧与分镜策划师，请把用户原始剧本整理为适合视频生产的结构化 JSON。"
        );
        String userPath = briefPrompt == null ? "prompts/script/refine-user.md" : "prompts/script/refine-user-with-brief.md";
        Map<String, Object> userVars = new LinkedHashMap<>();
        userVars.put("projectName", stringValue(aggregate.project.name, "未命名项目"));
        userVars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        userVars.put("aspectRatio", stringValue(aggregate.project.aspectRatio, "16:9"));
        userVars.put(
                "targetDuration",
                String.valueOf(aggregate.project.targetDuration == null ? 15 : aggregate.project.targetDuration)
        );
        userVars.put("language", stringValue(aggregate.project.language, "中文"));
        userVars.put("originalScript", originalText);
        if (briefPrompt != null) {
            userVars.put("briefPrompt", briefPrompt);
        }
        String fallbackUserPrompt = briefPrompt == null
                ? """
                    请将以下原始剧本整理为 JSON，字段至少包含：
                    title、summary、characters、backgrounds、props、scenes、segments。
                    输出必须是严格 JSON。
                    原始剧本：
                    {{originalScript}}
                    """
                : """
                    请将以下原始剧本整理为 JSON，字段至少包含：
                    title、summary、characters、backgrounds、props、scenes、segments。
                    输出必须是严格 JSON。
                    用户补充需求（简短提示词）：{{briefPrompt}}
                    原始剧本：
                    {{originalScript}}
                    """;
        String userPrompt = promptTemplateService.renderForProject(aggregate.project, 
                userPath,
                userVars,
                fallbackUserPrompt
        );

        try {
            Map<String, Object> payload = buildChatPayload(resolvedModel, systemPrompt, userPrompt);
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload,
                    Duration.ofSeconds(90)
            );
            String content = extractTextContent(response, resolvedModel.provider().apiFormat(), resolvedModel.provider().key());
            Map<String, Object> parsed = parseJsonContent(content);
            if (parsed.isEmpty()) {
                String message = "文本模型返回内容不是有效 JSON";
                persistRefineFailureContext(aggregate, resolvedModel, message, null);
                throw new BizException(502, message);
            }
            Map<String, Object> fallback = fallbackStructuredScript(aggregate, originalText);
            parsed.putIfAbsent("title", fallback.get("title"));
            parsed.putIfAbsent("summary", fallback.get("summary"));
            parsed.putIfAbsent("characters", fallback.get("characters"));
            parsed.putIfAbsent("backgrounds", fallback.get("backgrounds"));
            parsed.putIfAbsent("props", fallback.get("props"));
            parsed.putIfAbsent("scenes", fallback.get("scenes"));
            parsed.putIfAbsent("segments", fallback.get("segments"));
            return refineStructuredScript(parsed, fallback, originalText);
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            persistRefineFailureContext(aggregate, resolvedModel, "调用文本模型失败", ex);
            throw new BizException(502, "调用文本模型失败：" + safeError(ex));
        }
    }

    private void persistRefineFailureContext(
            ScriptProjectAggregate aggregate,
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel,
            String reason,
            Exception ex
    ) {
        String providerKey = resolvedModel.provider() == null ? "N/A" : resolvedModel.provider().key();
        String connectionName = resolvedModel.connection() == null ? "N/A" : resolvedModel.connection().getName();
        String content = """
                reason: %s
                model: %s
                provider: %s
                source: %s
                connection: %s
                matchedBy: %s
                rejectReason: %s
                exception: %s
                """.formatted(
                reason,
                stringValue(resolvedModel.modelName(), "N/A"),
                providerKey,
                stringValue(resolvedModel.source(), "N/A"),
                connectionName,
                stringValue(resolvedModel.matchedBy(), "N/A"),
                stringValue(resolvedModel.rejectReason(), "N/A"),
                ex == null ? "N/A" : safeError(ex)
        );
        StoredFileRecord rawResponse = localAssetFileService.storeText(
                aggregate.project.projectId,
                "documents/refine-raw-response.txt",
                "text/plain; charset=UTF-8",
                content
        );
        scriptProjectService.upsertFile(aggregate, rawResponse);
    }

    private String safeError(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex == null ? "unknown" : ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private StoredFileRecord generateKeyframeImage(
            ScriptProjectAggregate aggregate,
            ExtractedAsset asset,
            String prompt,
            String generationBatchId,
            int index
    ) {
        String basePath = "keyframes/" + asset.assetId + "/" + generationBatchId + "/keyframe-" + index;
        return generateStoredImage(aggregate, WorkflowModelKey.KEYFRAME_IMAGE, prompt, basePath + ".png", asset.name);
    }

    /**
     * 通用图像生成并落库（关键帧、九宫格、群像等共用）。
     */
    private StoredFileRecord generateStoredImage(
            ScriptProjectAggregate aggregate,
            String prompt,
            String relativePathWithExt,
            String labelForPlaceholder
    ) {
        return generateStoredImage(aggregate, null, prompt, relativePathWithExt, labelForPlaceholder);
    }

    /**
     * 通用图像生成并落库，支持功能键级模型覆盖。
     * @param functionKey  WorkflowModelKey 常量，null 时回退到项目级 explicitImageModel
     */
    private StoredFileRecord generateStoredImage(
            ScriptProjectAggregate aggregate,
            String functionKey,
            String prompt,
            String relativePathWithExt,
            String labelForPlaceholder
    ) {
        String effectiveModel = scriptProjectService.resolveWorkflowModel(aggregate.project, functionKey, "image");
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveImage(effectiveModel);
        if (!resolvedModel.hasProvider() || resolvedModel.apiKey() == null || resolvedModel.apiKey().isBlank()) {
            return createPlaceholderImage(aggregate.project.projectId, relativePathWithExt.replace(".png", ".svg"), prompt, labelForPlaceholder);
        }
        try {
            Map<String, Object> responseBody;
            if ("ark".equalsIgnoreCase(resolvedModel.provider().key())) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("model", resolvedModel.modelName());
                payload.put("prompt", prompt);
                payload.put("response_format", "url");
                payload.put("size", "2K");
                payload.put("watermark", true);
                responseBody = providerHttpGateway.generateImage(
                        resolvedModel.provider(),
                        resolvedModel.connection() == null ? "https://ark.cn-beijing.volces.com" : resolvedModel.connection().getBaseUrl(),
                        resolvedModel.apiKey(),
                        payload,
                        Duration.ofSeconds(120)
                );
            } else {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("model", resolvedModel.modelName());
                payload.put("prompt", prompt);
                payload.put("n", 1);
                payload.put("size", "1024x1024");
                payload.put("response_format", "url");
                responseBody = providerHttpGateway.generateImage(
                        resolvedModel.provider(),
                        resolvedModel.connection().getBaseUrl(),
                        resolvedModel.apiKey(),
                        payload,
                        Duration.ofSeconds(120)
                );
            }
            String url = parseImageUrl(responseBody);
            String b64 = parseBase64Image(responseBody);
            if (url != null) {
                try {
                    return localAssetFileService.storeRemote(aggregate.project.projectId, relativePathWithExt, "image/png", url);
                } catch (Exception storeEx) {
                    if (resolvedModel.systemFallback()) {
                        return createPlaceholderImage(aggregate.project.projectId, relativePathWithExt.replace(".png", ".svg"), prompt, labelForPlaceholder);
                    }
                    throw new BizException(502, "图片下载/存储失败：" + safeError(storeEx));
                }
            }
            if (b64 != null) {
                try {
                    return localAssetFileService.storeBase64(aggregate.project.projectId, relativePathWithExt, "image/png", b64);
                } catch (Exception storeEx) {
                    if (resolvedModel.systemFallback()) {
                        return createPlaceholderImage(aggregate.project.projectId, relativePathWithExt.replace(".png", ".svg"), prompt, labelForPlaceholder);
                    }
                    throw new BizException(502, "Base64 图片解码/存储失败：" + safeError(storeEx));
                }
            }
            if (resolvedModel.systemFallback()) {
                return createPlaceholderImage(aggregate.project.projectId, relativePathWithExt.replace(".png", ".svg"), prompt, labelForPlaceholder);
            }
            throw new BizException(502, "图片模型未返回可用图片（模型：" + resolvedModel.modelName() + "）");
        } catch (Exception ex) {
            if (resolvedModel.systemFallback()) {
                return createPlaceholderImage(aggregate.project.projectId, relativePathWithExt.replace(".png", ".svg"), prompt, labelForPlaceholder);
            }
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用图片模型失败（模型：" + resolvedModel.modelName() + "）：" + safeError(ex));
        }
    }

    private Map<String, Object> fallbackStructuredScript(ScriptProjectAggregate aggregate, String originalText) {
        List<String> paragraphs = splitParagraphs(originalText);
        String title = aggregate.project.name == null || aggregate.project.name.isBlank()
                ? firstNonBlankLine(originalText, "未命名剧本")
                : aggregate.project.name;

        List<Map<String, Object>> characters = inferCharacters(originalText);
        List<Map<String, Object>> props = inferProps(originalText);
        List<Map<String, Object>> scenes = new ArrayList<>();
        List<Map<String, Object>> backgrounds = new ArrayList<>();
        List<Map<String, Object>> segments = new ArrayList<>();

        for (int index = 0; index < paragraphs.size(); index++) {
            String paragraph = paragraphs.get(index);
            String sceneId = "scene-" + (index + 1);
            Map<String, Object> scene = new LinkedHashMap<>();
            scene.put("id", sceneId);
            scene.put("title", "场景 " + (index + 1));
            scene.put("location", inferLocation(paragraph, index + 1));
            scene.put("time", inferTime(paragraph));
            scene.put("atmosphere", inferMood(paragraph));
            scene.put("summary", summarizeSegment(paragraph));
            scenes.add(scene);

            Map<String, Object> background = new LinkedHashMap<>();
            background.put("id", "bg-" + (index + 1));
            background.put("name", scene.get("location"));
            background.put("description", "%s，%s，氛围%s".formatted(scene.get("location"), scene.get("time"), scene.get("atmosphere")));
            background.put("sceneId", sceneId);
            backgrounds.add(background);

            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("id", "segment-" + (index + 1));
            segment.put("title", "分段 " + (index + 1));
            segment.put("scriptText", paragraph);
            segment.put("actionSummary", summarizeSegment(paragraph));
            segment.put("cameraMovement", suggestCameraMovement(index));
            segment.put("characterIds", characters.stream().limit(2).map(item -> String.valueOf(item.get("id"))).toList());
            segment.put("backgroundIds", List.of(background.get("id")));
            segment.put("propIds", props.stream().limit(2).map(item -> String.valueOf(item.get("id"))).toList());
            segments.add(segment);
        }

        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("title", title);
        structured.put("summary", summarizeSegment(originalText));
        structured.put("characters", characters);
        structured.put("backgrounds", backgrounds);
        structured.put("props", props);
        structured.put("scenes", scenes);
        structured.put("segments", segments);
        return structured;
    }

    private Map<String, Object> refineStructuredScript(
            Map<String, Object> parsed,
            Map<String, Object> fallback,
            String originalText
    ) {
        Map<String, Object> result = new LinkedHashMap<>(parsed);

        List<Map<String, Object>> sanitizedCharacters = sanitizeCharacters(
                asMapList(result.get("characters")),
                asMapList(fallback.get("characters")),
                originalText
        );
        result.put("characters", sanitizedCharacters);

        if (isLowQualityText(stringValue(result.get("summary"), ""))) {
            result.put("summary", fallback.get("summary"));
        }

        if (isLowQualitySegments(asMapList(result.get("segments")))) {
            result.put("segments", fallback.get("segments"));
        }

        if (isLowQualityScenes(asMapList(result.get("scenes")))) {
            result.put("scenes", fallback.get("scenes"));
        }

        if (isLowQualityBackgrounds(asMapList(result.get("backgrounds")))) {
            result.put("backgrounds", fallback.get("backgrounds"));
        }

        if (isLowQualityProps(asMapList(result.get("props")))) {
            result.put("props", fallback.get("props"));
        }

        normalizeSegmentRefs(result);
        return result;
    }

    private List<Map<String, Object>> sanitizeCharacters(
            List<Map<String, Object>> modelCharacters,
            List<Map<String, Object>> fallbackCharacters,
            String originalText
    ) {
        List<Map<String, Object>> candidates = modelCharacters.isEmpty() ? fallbackCharacters : modelCharacters;
        List<Map<String, Object>> sanitized = new ArrayList<>();
        Set<String> usedNames = new LinkedHashSet<>();
        for (Map<String, Object> character : candidates) {
            String name = stringValue(character.get("name"), "").trim();
            if (name.isBlank() || isInvalidCharacterName(name)) {
                continue;
            }
            if (!usedNames.add(name)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(character);
            item.put("name", name);
            item.put("id", stringValue(character.get("id"), "char-" + (sanitized.size() + 1)));
            item.put("description", stringValue(character.get("description"), name + "，需保持角色一致性。"));
            sanitized.add(item);
        }

        if (!sanitized.isEmpty()) {
            return sanitized;
        }
        return inferCharacters(originalText);
    }

    private boolean isInvalidCharacterName(String name) {
        String normalized = name.replaceAll("\\s+", "");
        if (normalized.length() < 2) {
            return true;
        }
        String[] blocked = {"同人剧本", "剧本", "风格", "登场角色", "场景", "时长", "基础信息", "第一幕", "第二幕", "第三幕", "第四幕", "第五幕", "幕"};
        for (String token : blocked) {
            if (normalized.equals(token) || normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLowQualitySegments(List<Map<String, Object>> segments) {
        if (segments.isEmpty()) {
            return true;
        }
        long badCount = segments.stream().filter(segment -> {
            String scriptText = stringValue(segment.get("scriptText"), "");
            return scriptText.isBlank() || isLowQualityText(scriptText);
        }).count();
        return badCount >= Math.max(1, segments.size() / 2);
    }

    private boolean isLowQualityScenes(List<Map<String, Object>> scenes) {
        if (scenes.isEmpty()) {
            return true;
        }
        long badCount = scenes.stream().filter(scene -> {
            String title = stringValue(scene.get("title"), "");
            String summary = stringValue(scene.get("summary"), "");
            return title.isBlank() || isLowQualityText(summary);
        }).count();
        return badCount >= Math.max(1, scenes.size() / 2);
    }

    private boolean isLowQualityBackgrounds(List<Map<String, Object>> backgrounds) {
        if (backgrounds.isEmpty()) {
            return true;
        }
        long badCount = backgrounds.stream().filter(background -> {
            String name = stringValue(background.get("name"), "");
            String desc = stringValue(background.get("description"), "");
            return name.isBlank() || desc.isBlank() || "场景".equals(name);
        }).count();
        return badCount >= Math.max(1, backgrounds.size() / 2);
    }

    private boolean isLowQualityProps(List<Map<String, Object>> props) {
        if (props.isEmpty()) {
            return true;
        }
        long badCount = props.stream().filter(prop -> {
            String name = stringValue(prop.get("name"), "");
            return name.isBlank() || name.length() <= 1;
        }).count();
        return badCount >= Math.max(1, props.size() / 2);
    }

    private boolean isLowQualityText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            return true;
        }
        String[] genericSignals = {"承担核心叙事动作", "形象需保持统一风格", "场景 1", "分段 1", "电影感"};
        for (String signal : genericSignals) {
            if (normalized.contains(signal)) {
                return true;
            }
        }
        return false;
    }

    private void normalizeSegmentRefs(Map<String, Object> structured) {
        List<Map<String, Object>> segments = asMapList(structured.get("segments"));
        if (segments.isEmpty()) {
            return;
        }
        List<String> characterIds = asMapList(structured.get("characters")).stream()
                .map(item -> stringValue(item.get("id"), ""))
                .filter(value -> !value.isBlank())
                .toList();
        List<String> backgroundIds = asMapList(structured.get("backgrounds")).stream()
                .map(item -> stringValue(item.get("id"), ""))
                .filter(value -> !value.isBlank())
                .toList();
        List<String> propIds = asMapList(structured.get("props")).stream()
                .map(item -> stringValue(item.get("id"), ""))
                .filter(value -> !value.isBlank())
                .toList();

        for (Map<String, Object> segment : segments) {
            List<String> segmentCharacterIds = stringList(segment.get("characterIds")).stream()
                    .filter(characterIds::contains)
                    .toList();
            if (segmentCharacterIds.isEmpty() && !characterIds.isEmpty()) {
                segmentCharacterIds = characterIds.stream().limit(2).toList();
            }
            segment.put("characterIds", segmentCharacterIds);

            List<String> segmentBackgroundIds = stringList(segment.get("backgroundIds")).stream()
                    .filter(backgroundIds::contains)
                    .toList();
            if (segmentBackgroundIds.isEmpty() && !backgroundIds.isEmpty()) {
                segmentBackgroundIds = List.of(backgroundIds.get(0));
            }
            segment.put("backgroundIds", segmentBackgroundIds);

            List<String> segmentPropIds = stringList(segment.get("propIds")).stream()
                    .filter(propIds::contains)
                    .toList();
            if (segmentPropIds.isEmpty() && !propIds.isEmpty()) {
                segmentPropIds = propIds.stream().limit(2).toList();
            }
            segment.put("propIds", segmentPropIds);
        }
        structured.put("segments", segments);
    }

    private List<Map<String, Object>> inferCharacters(String originalText) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(?:：|说|看向|走向|来到|站在)").matcher(originalText);
        while (matcher.find() && names.size() < 5) {
            names.add(matcher.group(1));
        }
        if (names.isEmpty()) {
            names.add("主角");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 1;
        for (String name : names) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "char-" + index);
            item.put("name", name);
            item.put("description", name + "，承担核心叙事动作，形象需保持统一风格。");
            item.put("tags", List.of("角色", "主视觉"));
            result.add(item);
            index++;
        }
        return result;
    }

    private List<Map<String, Object>> inferProps(String originalText) {
        String[] keywords = {"手机", "电脑", "书", "信", "咖啡", "汽车", "门", "灯", "伞", "花", "剑", "相机"};
        List<Map<String, Object>> result = new ArrayList<>();
        int index = 1;
        for (String keyword : keywords) {
            if (originalText.contains(keyword)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", "prop-" + index);
                item.put("name", keyword);
                item.put("description", keyword + " 是推动画面叙事的重要道具，需要保持清晰质感。");
                item.put("tags", List.of("道具"));
                result.add(item);
                index++;
            }
        }
        if (result.isEmpty()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "prop-1");
            item.put("name", "核心道具");
            item.put("description", "用于承接关键剧情动作的通用道具。");
            item.put("tags", List.of("道具"));
            result.add(item);
        }
        return result;
    }

    private String buildRefinedMarkdown(Map<String, Object> structuredScript) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(stringValue(structuredScript.get("title"), "未命名剧本")).append("\n\n");
        builder.append("## 故事摘要\n");
        builder.append(stringValue(structuredScript.get("summary"), "暂无摘要")).append("\n\n");

        builder.append("## 角色设定\n");
        for (Map<String, Object> character : asMapList(structuredScript.get("characters"))) {
            builder.append("- **").append(stringValue(character.get("name"), "角色"))
                    .append("**：").append(stringValue(character.get("description"), "暂无描述")).append("\n");
            appendIfPresent(builder, "  - 人设", character.get("persona"));
            appendIfPresent(builder, "  - 性格特质", character.get("traits"));
            appendIfPresent(builder, "  - 记忆点", character.get("quirks"));
        }
        builder.append("\n## 场景与背景\n");
        for (Map<String, Object> background : resolveBackgroundItems(structuredScript)) {
            builder.append("- **").append(stringValue(background.get("name"), "背景"))
                    .append("**：").append(stringValue(background.get("description"), "暂无描述")).append("\n");
        }
        builder.append("\n## 场次与场景（拍摄向）\n");
        for (Map<String, Object> scene : asMapList(structuredScript.get("scenes"))) {
            builder.append("- **").append(stringValue(scene.get("title"), "场景"))
                    .append("**（").append(stringValue(scene.get("location"), "")).append(" / ")
                    .append(stringValue(scene.get("time"), "")).append("）\n");
            appendIfPresent(builder, "  - 拍摄备注", scene.get("shootingNotes"));
            appendIfPresent(builder, "  - 走位调度", scene.get("blocking"));
            appendIfPresent(builder, "  - 估算时长(秒)", scene.get("estimatedDurationSec"));
        }
        builder.append("\n## 视频分段\n");
        int index = 1;
        for (Map<String, Object> segment : asMapList(structuredScript.get("segments"))) {
            builder.append("### ").append(index).append(". ")
                    .append(stringValue(segment.get("title"), "分段 " + index)).append("\n");
            builder.append("- 画面内容：").append(stringValue(segment.get("scriptText"), "")).append("\n");
            builder.append("- 动作摘要：").append(stringValue(segment.get("actionSummary"), "")).append("\n");
            builder.append("- 镜头建议：").append(stringValue(segment.get("cameraMovement"), "")).append("\n");
            appendIfPresent(builder, "- 拍摄备注", segment.get("shootingNotes"));
            appendIfPresent(builder, "- 走位调度", segment.get("blocking"));
            appendIfPresent(builder, "- 估算时长(秒)", segment.get("estimatedDurationSec"));
            builder.append("\n");
            index++;
        }
        builder.append("\n## 道具创意\n");
        for (Map<String, Object> prop : asMapList(structuredScript.get("props"))) {
            builder.append("- **").append(stringValue(prop.get("name"), "道具"))
                    .append("**：").append(stringValue(prop.get("description"), "")).append("\n");
            appendIfPresent(builder, "  - 巧思用法", prop.get("creativeUse"));
            appendIfPresent(builder, "  - 重要性", prop.get("importance"));
        }
        return builder.toString().trim();
    }

    private void appendIfPresent(StringBuilder builder, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return;
        }
        builder.append(label).append("：").append(text).append("\n");
    }

    private Map<String, Object> loadStructuredScript(ScriptProjectAggregate aggregate) {
        StoredFileRecord file = scriptProjectService.findFile(aggregate, aggregate.project.refinedScriptJsonFileId);
        return file == null ? new LinkedHashMap<>() : localAssetFileService.readJson(file);
    }

    private ExtractedAsset buildFallbackAsset(String projectId, AssetType assetType, ScriptProjectAggregate aggregate, Instant now) {
        ExtractedAsset asset = new ExtractedAsset();
        asset.assetId = nextId(prefixForAssetType(assetType));
        asset.projectId = projectId;
        asset.assetType = assetType;
        asset.name = defaultAssetName(assetType, 1);
        asset.description = switch (assetType) {
            case CHARACTER -> "根据当前剧本补出的主角视觉设定。";
            case BACKGROUND -> "根据剧本氛围补出的核心场景背景。";
            case PROP -> "根据剧情动作补出的关键道具。";
        };
        asset.promptDraft = buildAssetPromptDraft(aggregate, assetType, Map.of(), asset.description);
        asset.tags = List.of(assetType.name().toLowerCase(Locale.ROOT));
        asset.status = AssetStatus.EXTRACTED;
        asset.metadata = new LinkedHashMap<>();
        asset.createdAt = now;
        asset.updatedAt = now;
        return asset;
    }

    private String buildAssetPromptDraft(ScriptProjectAggregate aggregate, AssetType assetType, Map<String, Object> source, String description) {
        return switch (assetType) {
            case CHARACTER -> "%s风格角色设定，人物名：%s，视觉重点：%s".formatted(
                    resolveVisualStyleAnchor(aggregate.project.visualStyle),
                    stringValue(source.get("name"), "主角"),
                    description
            );
            case BACKGROUND -> "%s风格视频背景，场景：%s，氛围：%s".formatted(
                    resolveVisualStyleAnchor(aggregate.project.visualStyle),
                    stringValue(source.get("name"), "核心场景"),
                    stringValue(source.get("description"), description)
            );
            case PROP -> "%s风格视频道具，名称：%s，材质/视觉重点：%s".formatted(
                    resolveVisualStyleAnchor(aggregate.project.visualStyle),
                    stringValue(source.get("name"), "关键道具"),
                    stringValue(source.get("description"), description)
            );
        };
    }

    private String buildKeyframePrompt(ScriptProjectAggregate aggregate, ExtractedAsset asset, int index) {
        String templatePath = switch (asset.assetType) {
            case CHARACTER -> "prompts/keyframe/character-keyframe.md";
            case BACKGROUND -> "prompts/keyframe/background-keyframe.md";
            case PROP -> "prompts/keyframe/prop-keyframe.md";
        };
        String consistencyPrefix = "";
        if (aggregate.project.artDirectionJson != null && !aggregate.project.artDirectionJson.isBlank()) {
            try {
                Map<String, Object> ad = objectMapper.readValue(aggregate.project.artDirectionJson, new TypeReference<Map<String, Object>>() {});
                String anchors = stringValue(ad.get("consistencyAnchors"), "");
                if (!anchors.isBlank()) {
                    consistencyPrefix = anchors + " ";
                }
            } catch (Exception ignored) {
                // ignore parse errors for keyframe prompt
            }
        }
        String visualBody = (asset.visualPrompt != null && !asset.visualPrompt.isBlank())
                ? asset.visualPrompt
                : "";
        return promptTemplateService.renderForProject(aggregate.project, 
                templatePath,
                Map.of(
                        "consistencyPrefix", consistencyPrefix,
                        "projectName", stringValue(aggregate.project.name, "未命名项目"),
                        "visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle),
                        "aspectRatio", stringValue(aggregate.project.aspectRatio, "16:9"),
                        "assetName", stringValue(asset.name, "资产"),
                        "assetDescription", stringValue(asset.description, "暂无描述"),
                        "visualPromptBody", visualBody.isBlank() ? "" : "视觉提示词：" + visualBody + "。",
                        "promptDraft", stringValue(asset.promptDraft, ""),
                        "variationIndex", String.valueOf(index)
                ),
                "{{visualStyle}}，{{assetName}}，{{assetDescription}}，画面比例 {{aspectRatio}}，保持统一叙事风格，关键帧版本 {{variationIndex}}。"
        );
    }

    private Map<String, Object> buildChatPayload(
            AiCapabilityRoutingService.ResolvedAiModel resolvedModel,
            String systemPrompt,
            String userPrompt
    ) {
        if ("ark".equalsIgnoreCase(resolvedModel.provider().key())) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", resolvedModel.modelName());
            payload.put("input", List.of(
                    Map.of(
                            "role", "system",
                            "content", List.of(
                                    Map.of(
                                            "type", "input_text",
                                            "text", systemPrompt
                                    )
                            )
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of(
                                            "type", "input_text",
                                            "text", userPrompt
                                    )
                            )
                    )
            ));
            payload.put("temperature", 0.4);
            payload.put("max_output_tokens", 4000);
            return payload;
        }
        if ("anthropic".equalsIgnoreCase(resolvedModel.provider().apiFormat())) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", resolvedModel.modelName());
            payload.put("system", systemPrompt);
            payload.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));
            payload.put("max_tokens", 4000);
            payload.put("temperature", 0.4);
            return payload;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolvedModel.modelName());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        payload.put("temperature", 0.4);
        payload.put("max_tokens", 4000);
        return payload;
    }

    private String extractTextContent(Map<String, Object> response, String apiFormat, String providerKey) {
        if ("ark".equalsIgnoreCase(providerKey)) {
            String text = extractArkOutputText(response);
            if (!text.isBlank()) {
                return text;
            }
        }
        if ("anthropic".equalsIgnoreCase(apiFormat)) {
            Object contentNode = response.get("content");
            if (contentNode instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                Object text = first.get("text");
                return text == null ? "" : String.valueOf(text);
            }
            return "";
        }
        Object choicesNode = response.get("choices");
        if (choicesNode instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object messageNode = first.get("message");
            if (messageNode instanceof Map<?, ?> message) {
                Object content = message.get("content");
                return content == null ? "" : String.valueOf(content);
            }
        }
        return "";
    }

    private String extractArkOutputText(Map<String, Object> response) {
        Object outputNode = response.get("output");
        if (!(outputNode instanceof List<?> outputs) || outputs.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object output : outputs) {
            if (!(output instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object contentNode = outputMap.get("content");
            if (!(contentNode instanceof List<?> contents)) {
                continue;
            }
            for (Object content : contents) {
                if (!(content instanceof Map<?, ?> contentMap)) {
                    continue;
                }
                Object type = contentMap.get("type");
                if (!"output_text".equals(String.valueOf(type))) {
                    continue;
                }
                Object text = contentMap.get("text");
                if (text != null) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(String.valueOf(text));
                }
            }
        }
        return builder.toString().trim();
    }

    private Map<String, Object> parseJsonContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```json", "").replaceFirst("^```", "");
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3);
            }
        }
        normalized = normalized.trim();
        try {
            return objectMapper.readValue(normalized, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private StoredFileRecord createPlaceholderImage(String projectId, String relativePath, String prompt, String title) {
        String safeTitle = escapeXml(title == null ? "关键帧" : title);
        String safePrompt = escapeXml(prompt == null ? "" : prompt);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%%" stop-color="#1f2937"/>
                      <stop offset="100%%" stop-color="#7c3aed"/>
                    </linearGradient>
                  </defs>
                  <rect width="1024" height="1024" fill="url(#bg)"/>
                  <rect x="72" y="72" width="880" height="880" rx="36" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.18)"/>
                  <text x="96" y="180" font-size="54" fill="#f9fafb" font-family="Arial, sans-serif">%s</text>
                  <foreignObject x="96" y="240" width="832" height="640">
                    <div xmlns="http://www.w3.org/1999/xhtml" style="color:#e5e7eb;font-family:Arial,sans-serif;font-size:28px;line-height:1.6;white-space:pre-wrap;">%s</div>
                  </foreignObject>
                </svg>
                """.formatted(safeTitle, safePrompt);
        return localAssetFileService.storeText(projectId, relativePath, "image/svg+xml", svg);
    }

    private String parseImageUrl(Map<String, Object> body) {
        Object dataNode = body.get("data");
        if (!(dataNode instanceof List<?> dataList) || dataList.isEmpty()) {
            return null;
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object url = firstMap.get("url");
        return url == null ? null : String.valueOf(url);
    }

    private String parseBase64Image(Map<String, Object> body) {
        Object dataNode = body.get("data");
        if (!(dataNode instanceof List<?> dataList) || dataList.isEmpty()) {
            return null;
        }
        Object first = dataList.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object value = firstMap.get("b64_json");
        return value == null ? null : String.valueOf(value);
    }

    private PipelineRun beginPipeline(ScriptProjectAggregate aggregate, PipelineType pipelineType, String stage, int totalCount) {
        PipelineRun pipelineRun = new PipelineRun();
        pipelineRun.pipelineRunId = nextId("run");
        pipelineRun.projectId = aggregate.project.projectId;
        pipelineRun.pipelineType = pipelineType;
        pipelineRun.status = PipelineStatus.RUNNING;
        pipelineRun.currentStage = stage;
        pipelineRun.totalCount = totalCount;
        pipelineRun.successCount = 0;
        pipelineRun.failedCount = 0;
        pipelineRun.createdAt = Instant.now();
        pipelineRun.updatedAt = pipelineRun.createdAt;
        aggregate.pipelineRuns.add(pipelineRun);
        return pipelineRun;
    }

    private void finishPipeline(PipelineRun pipelineRun, PipelineStatus status, int successCount, int failedCount, String errorMessage) {
        pipelineRun.status = status;
        pipelineRun.successCount = successCount;
        pipelineRun.failedCount = failedCount;
        pipelineRun.errorMessage = errorMessage;
        pipelineRun.updatedAt = Instant.now();
    }

    private String buildCharacterLinesForArtDirection(Map<String, Object> structuredScript) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> ch : asMapList(structuredScript.get("characters"))) {
            sb.append(stringValue(ch.get("id"), "id"))
                    .append(" | ")
                    .append(stringValue(ch.get("name"), "角色"))
                    .append(" | ")
                    .append(stringValue(ch.get("gender"), ""))
                    .append(" | ")
                    .append(stringValue(ch.get("age"), ""))
                    .append(" | ")
                    .append(stringValue(ch.get("personality"), stringValue(ch.get("traits"), "")))
                    .append("\n");
        }
        String s = sb.toString().trim();
        return s.isBlank() ? "（剧本中暂无结构化角色列表）" : s;
    }

    private String buildSceneLinesForArtDirection(Map<String, Object> structuredScript) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> sc : asMapList(structuredScript.get("scenes"))) {
            sb.append(stringValue(sc.get("title"), "场景"))
                    .append(" | ")
                    .append(stringValue(sc.get("location"), ""))
                    .append(" | ")
                    .append(stringValue(sc.get("time"), ""))
                    .append(" | ")
                    .append(stringValue(sc.get("atmosphere"), ""))
                    .append("\n");
        }
        String s = sb.toString().trim();
        return s.isBlank() ? "（剧本中暂无结构化场景列表）" : s;
    }

    private String invokeTextModelOrThrow(ScriptProjectAggregate aggregate, String systemPrompt, String userPrompt) {
        return invokeTextModelOrThrow(aggregate, null, systemPrompt, userPrompt);
    }

    /**
     * Resolve text model via per-function override (if set) then project default, and invoke.
     */
    private String invokeTextModelOrThrow(ScriptProjectAggregate aggregate, String functionKey, String systemPrompt, String userPrompt) {
        String effectiveModel = scriptProjectService.resolveWorkflowModel(aggregate.project, functionKey, "text");
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(effectiveModel);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String reason = effectiveModel == null || effectiveModel.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + effectiveModel;
            throw new BizException(400, reason);
        }
        try {
            Map<String, Object> payload = buildChatPayload(resolvedModel, systemPrompt, userPrompt);
            Map<String, Object> response = providerHttpGateway.invokeChat(
                    resolvedModel.provider(),
                    resolvedModel.connection().getBaseUrl(),
                    resolvedModel.apiKey(),
                    resolvedModel.metadataPlain(),
                    payload,
                    Duration.ofSeconds(180)
            );
            return extractTextContent(response, resolvedModel.provider().apiFormat(), resolvedModel.provider().key());
        } catch (Exception ex) {
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用文本模型失败：" + safeError(ex));
        }
    }

    private String stripJsonFence(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "");
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private Map<String, Object> parseJsonObject(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new BizException(502, "JSON 解析失败");
        }
    }

    private Map<String, Object> nestedMap(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, val) -> out.put(String.valueOf(k), val));
            return out;
        }
        return Map.of();
    }

    private String formatMoodKeywords(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(", "));
        }
        return raw == null ? "" : String.valueOf(raw);
    }

    private String formatCharacterLineForBatch(ExtractedAsset asset) {
        String sourceId = stringValue(asset.metadata.get("sourceId"), asset.assetId);
        return sourceId + " | " + stringValue(asset.name, "角色")
                + " | " + stringValue(asset.metadata.get("gender"), "未知")
                + " | " + stringValue(asset.metadata.get("age"), "未知")
                + " | " + stringValue(asset.metadata.get("personality"), stringValue(asset.metadata.get("traits"), ""));
    }

    private ExtractedAsset findCharacterAssetBySourceOrId(
            ScriptProjectAggregate aggregate,
            String sourceId,
            List<ExtractedAsset> characters
    ) {
        for (ExtractedAsset a : characters) {
            if (Objects.equals(stringValue(a.metadata.get("sourceId"), a.assetId), sourceId)
                    || Objects.equals(a.assetId, sourceId)) {
                return a;
            }
        }
        return null;
    }

    private String buildArtDirectionAnchorBlock(ScriptProjectAggregate aggregate) {
        if (aggregate.project.artDirectionJson == null || aggregate.project.artDirectionJson.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> ad = parseJsonObject(aggregate.project.artDirectionJson);
            String anchors = stringValue(ad.get("consistencyAnchors"), "");
            return anchors.isBlank() ? "" : anchors;
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalizeSingleLinePrompt(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:text|markdown)?\\s*", "");
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private int intValue(Object o, int def) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private String shortenWords(String text, int maxLen) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        if (t.length() <= maxLen) {
            return t;
        }
        return t.substring(0, maxLen) + "…";
    }

    private Map<String, Object> buildStoryboardCommonVars(ScriptProjectAggregate aggregate, ExtractedAsset asset, int panelCount) {
        int lastIndex = panelCount - 1;
        String gridLayout = "3×3";
        String layoutInstruction = "3 columns × 3 rows";
        String layoutExample = "1 2 3 / 4 5 6 / 7 8 9";
        String location = stringValue(asset.metadata.get("location"), stringValue(asset.name, "未命名场景"));
        String time = stringValue(asset.metadata.get("time"), "白天");
        String atmosphere = stringValue(asset.metadata.get("atmosphere"), "电影感");
        String characters = stringValue(asset.metadata.get("characters"), stringValue(asset.name, "主体角色"));
        String actionSummary = stringValue(asset.description, stringValue(asset.promptDraft, "角色动作连续变化"));
        String cameraMovement = stringValue(asset.metadata.get("cameraMovement"), "静态分镜");
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("panelCount", panelCount);
        vars.put("gridLayout", gridLayout);
        vars.put("layoutInstruction", layoutInstruction);
        vars.put("layoutExample", layoutExample);
        vars.put("layoutSpecificConstraint", "No cell may span multiple grid positions");
        vars.put("lastIndex", lastIndex);
        vars.put("midIndex", Math.max(1, lastIndex - 1));
        vars.put("actionSummary", actionSummary);
        vars.put("cameraMovement", cameraMovement);
        vars.put("location", location);
        vars.put("time", time);
        vars.put("atmosphere", atmosphere);
        vars.put("characters", characters);
        vars.put("visualStyle", resolveVisualStyleAnchor(aggregate.project.visualStyle));
        vars.put("sceneContext", shortenWords(stringValue(asset.visualPrompt, actionSummary), 120));
        vars.put("sceneLocation", location);
        vars.put("sceneTime", time);
        vars.put("sceneAtmosphere", atmosphere);
        return vars;
    }

    private List<Map<String, Object>> parseStoryboardPanels(String json, int expectedCount) {
        Map<String, Object> root = parseJsonObject(json);
        Object panelsNode = root.get("panels");
        if (!(panelsNode instanceof List<?> panelList)) {
            throw new BizException(400, "九宫格规划缺少 panels 数组");
        }
        List<Map<String, Object>> panels = new ArrayList<>();
        for (Object panelNode : panelList) {
            if (!(panelNode instanceof Map<?, ?> panelMap)) {
                continue;
            }
            Map<String, Object> panel = new LinkedHashMap<>();
            panelMap.forEach((k, v) -> panel.put(String.valueOf(k), v));
            panels.add(panel);
        }
        if (panels.size() != expectedCount) {
            throw new BizException(400, "九宫格面板数量不正确");
        }
        panels.sort(Comparator.comparingInt(p -> intValue(p.get("index"), 0)));
        return panels;
    }

    private void validateStoryboardPanelsJson(String json, int expectedCount) {
        List<Map<String, Object>> panels = parseStoryboardPanels(json, expectedCount);
        Set<Integer> indexSet = new LinkedHashSet<>();
        Set<String> comboSet = new LinkedHashSet<>();
        Set<String> shotSizeSet = new LinkedHashSet<>();
        for (Map<String, Object> panel : panels) {
            int index = intValue(panel.get("index"), -1);
            if (index < 0 || index >= expectedCount) {
                throw new BizException(400, "九宫格索引范围错误");
            }
            if (!indexSet.add(index)) {
                throw new BizException(400, "九宫格索引重复");
            }
            String shotSize = stringValue(panel.get("shotSize"), "").trim();
            String cameraAngle = stringValue(panel.get("cameraAngle"), "").trim();
            String description = stringValue(panel.get("description"), "").trim();
            if (shotSize.isBlank() || cameraAngle.isBlank() || description.isBlank()) {
                throw new BizException(400, "九宫格面板存在空字段");
            }
            if (description.split("\\s+").length < 8) {
                throw new BizException(400, "九宫格面板描述过短");
            }
            String combo = shotSize + "|" + cameraAngle;
            if (!comboSet.add(combo)) {
                throw new BizException(400, "九宫格存在重复视角组合");
            }
            shotSizeSet.add(shotSize);
        }
        for (int i = 0; i < expectedCount; i++) {
            if (!indexSet.contains(i)) {
                throw new BizException(400, "九宫格索引不连续");
            }
        }
        if (expectedCount >= 6 && shotSizeSet.size() < 3) {
            throw new BizException(400, "九宫格景别种类不足 3 种");
        }
    }

    private String translateStoryboardPanelsOrFallback(ScriptProjectAggregate aggregate, String panelsJson, int expectedCount) {
        try {
            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("panelsJson", panelsJson);
            vars.put("expectedCount", expectedCount);
            vars.put("expectedCountMinusOne", expectedCount - 1);
            String userPrompt = promptTemplateService.renderForProject(aggregate.project, "prompts/visual/storyboard-translate-user.md", vars, "");
            String raw = invokeTextModelOrThrow(aggregate, WorkflowModelKey.STORYBOARD_PLAN, "Output ONLY valid JSON as specified.", userPrompt);
            String json = stripJsonFence(raw);
            Map<String, Object> root = parseJsonObject(json);
            Object translationsNode = root.get("translations");
            if (!(translationsNode instanceof List<?> list) || list.size() != expectedCount) {
                throw new BizException(502, "九宫格翻译结构不完整");
            }
            return json;
        } catch (Exception ignore) {
            List<Map<String, Object>> panels = parseStoryboardPanels(panelsJson, expectedCount);
            List<Map<String, Object>> translations = new ArrayList<>();
            for (Map<String, Object> panel : panels) {
                int index = intValue(panel.get("index"), 0);
                String shotSize = stringValue(panel.get("shotSize"), "");
                String angle = stringValue(panel.get("cameraAngle"), "");
                String description = stringValue(panel.get("description"), "");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", index);
                item.put("descriptionZh", (shotSize + " / " + angle + "： " + description).trim());
                translations.add(item);
            }
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("translations", translations);
            try {
                return objectMapper.writeValueAsString(root);
            } catch (Exception ex) {
                return "{\"translations\":[]}";
            }
        }
    }

    private String panelPosition(int index) {
        int row = index / 3 + 1;
        int col = index % 3 + 1;
        return "Row " + row + " Col " + col;
    }

    private String cropStoryboardPanelInternal(ScriptProjectAggregate aggregate, ExtractedAsset asset, int panelIndex) {
        if (panelIndex < 0 || panelIndex >= STORYBOARD_PANEL_COUNT) {
            throw new BizException(400, "面板索引超出范围");
        }
        if (asset.storyboardImageFileId == null || asset.storyboardImageFileId.isBlank()) {
            throw new BizException(400, "请先生成九宫格分镜图");
        }
        for (StoredFileRecord existing : new ArrayList<>(aggregate.files)) {
            if (existing != null && existing.relativePath != null
                    && existing.relativePath.replace('\\', '/').endsWith("crop/panel-" + panelIndex + ".png")) {
                assetHistoryService.appendSnapshot(
                        aggregate.project.projectId,
                        AssetHistoryType.STORYBOARD_CROP,
                        asset.assetId + ":" + panelIndex,
                        existing.fileId,
                        null,
                        null,
                        null
                );
                break;
            }
        }
        StoredFileRecord source = scriptProjectService.findFile(aggregate, asset.storyboardImageFileId);
        if (source == null) {
            throw new BizException(404, "九宫格分镜图文件不存在");
        }
        try {
            byte[] bytes = localAssetFileService.readBytes(source);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BizException(400, "九宫格图像解析失败");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            int panelWidth = Math.max(1, width / 3);
            int panelHeight = Math.max(1, height / 3);
            int row = panelIndex / 3;
            int col = panelIndex % 3;
            int x = col * panelWidth;
            int y = row * panelHeight;
            int cropW = (col == 2) ? width - x : panelWidth;
            int cropH = (row == 2) ? height - y : panelHeight;
            BufferedImage cropped = image.getSubimage(x, y, cropW, cropH);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "png", bos);
            StoredFileRecord cropFile = localAssetFileService.storeBytes(
                    aggregate.project.projectId,
                    "storyboard/" + asset.assetId + "/crop/panel-" + panelIndex + ".png",
                    "image/png",
                    bos.toByteArray()
            );
            scriptProjectService.upsertFile(aggregate, cropFile);
            return cropFile.fileId;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(500, "裁剪九宫格面板失败");
        }
    }

    private StoryboardShot findShot(ScriptProjectAggregate aggregate, String shotId) {
        return aggregate.shots.stream()
                .filter(item -> Objects.equals(item.shotId, shotId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "分镜不存在"));
    }

    private Map<String, String> resolveSceneContextForShot(
            ScriptProjectAggregate aggregate,
            StoryboardShot shot,
            Map<String, Object> structuredScript
    ) {
        Map<String, String> ctx = new HashMap<>();
        if (shot.backgroundRefs != null && !shot.backgroundRefs.isEmpty()) {
            String bgId = shot.backgroundRefs.get(0);
            for (ExtractedAsset a : aggregate.assets) {
                if (Objects.equals(a.assetId, bgId) && a.assetType == AssetType.BACKGROUND) {
                    ctx.put("location", stringValue(a.metadata.get("location"), a.name));
                    ctx.put("time", stringValue(a.metadata.get("time"), ""));
                    ctx.put("atmosphere", stringValue(a.metadata.get("atmosphere"), ""));
                    return ctx;
                }
            }
        }
        List<Map<String, Object>> scenes = asMapList(structuredScript.get("scenes"));
        if (!scenes.isEmpty()) {
            Map<String, Object> sc = scenes.get(0);
            ctx.put("location", stringValue(sc.get("location"), ""));
            ctx.put("time", stringValue(sc.get("time"), ""));
            ctx.put("atmosphere", stringValue(sc.get("atmosphere"), ""));
            return ctx;
        }
        ctx.put("location", "");
        ctx.put("time", "");
        ctx.put("atmosphere", "");
        return ctx;
    }

    private String buildShotCharacterLines(ScriptProjectAggregate aggregate, StoryboardShot shot) {
        if (shot.characterRefs == null || shot.characterRefs.isEmpty()) {
            return "（本镜头无角色，纯场景）";
        }
        StringBuilder sb = new StringBuilder();
        for (String assetId : shot.characterRefs) {
            ExtractedAsset a = aggregate.assets.stream()
                    .filter(x -> Objects.equals(x.assetId, assetId))
                    .findFirst()
                    .orElse(null);
            if (a == null) {
                continue;
            }
            String anchor = shortenWords(stringValue(a.visualPrompt, a.description), 80);
            sb.append(stringValue(a.name, "角色"))
                    .append(" | ")
                    .append(anchor)
                    .append(" | ")
                    .append(stringValue(shot.actionSummary, ""))
                    .append(" | 情绪跟随镜头\n");
        }
        String s = sb.toString().trim();
        return s.isBlank() ? "（角色引用未解析）" : s;
    }

    private String inferShotType(StoryboardShot shot) {
        String text = (shot.title == null ? "" : shot.title)
                + " "
                + (shot.actionSummary == null ? "" : shot.actionSummary)
                + " "
                + (shot.scriptText == null ? "" : shot.scriptText);
        if (text.contains("特写") || text.contains("close")) {
            return "CU特写";
        }
        if (text.contains("全景") || text.contains("远景")) {
            return "LS全景";
        }
        if (text.contains("大远景")) {
            return "ELS大远景";
        }
        return "MCU中近景";
    }

    private ExtractedAsset findAsset(ScriptProjectAggregate aggregate, String assetId) {
        return aggregate.assets.stream()
                .filter(item -> Objects.equals(item.assetId, assetId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "资产不存在"));
    }

    private KeyframeRecord findKeyframe(ScriptProjectAggregate aggregate, String keyframeId) {
        return aggregate.keyframes.stream()
                .filter(item -> Objects.equals(item.keyframeId, keyframeId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "关键帧不存在"));
    }

    private List<Map<String, Object>> resolveBackgroundItems(Map<String, Object> structuredScript) {
        List<Map<String, Object>> items = asMapList(structuredScript.get("backgrounds"));
        if (!items.isEmpty()) {
            return items;
        }
        List<Map<String, Object>> scenes = asMapList(structuredScript.get("scenes"));
        List<Map<String, Object>> derived = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> scene : scenes) {
            Map<String, Object> background = new LinkedHashMap<>();
            background.put("id", "bg-" + index);
            background.put("name", stringValue(scene.get("location"), "场景背景 " + index));
            background.put("description", "%s，%s，氛围%s".formatted(
                    stringValue(scene.get("location"), "未知地点"),
                    stringValue(scene.get("time"), "未知时间"),
                    stringValue(scene.get("atmosphere"), "平稳")
            ));
            derived.add(background);
            index++;
        }
        return derived;
    }

    private List<Map<String, Object>> asMapList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(converted);
            }
        }
        return result;
    }

    private List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private List<String> splitTags(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        if (raw instanceof String text) {
            return List.of(text.split("[,，、]")).stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private List<Map<String, Object>> fallbackSegments(Map<String, Object> structuredScript, String refinedMarkdown) {
        List<String> paragraphs = splitParagraphs(refinedMarkdown);
        List<Map<String, Object>> segments = new ArrayList<>();
        for (int index = 0; index < paragraphs.size(); index++) {
            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("id", "segment-" + (index + 1));
            segment.put("title", "分段 " + (index + 1));
            segment.put("scriptText", paragraphs.get(index));
            segment.put("actionSummary", summarizeSegment(paragraphs.get(index)));
            segment.put("cameraMovement", suggestCameraMovement(index));
            segments.add(segment);
        }
        return segments;
    }

    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = java.util.Arrays.stream((text == null ? "" : text).split("(\\r?\\n){2,}"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(8)
                .toList();
        if (!paragraphs.isEmpty()) {
            return paragraphs;
        }
        String single = text == null ? "" : text.trim();
        return single.isBlank() ? List.of("故事片段待补充") : List.of(single);
    }

    private String summarizeSegment(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 100) {
            return normalized;
        }
        return normalized.substring(0, 100) + "…";
    }

    private String inferLocation(String paragraph, int index) {
        String[] candidates = {"书房", "客厅", "街道", "咖啡馆", "办公室", "夜色天台", "公园", "车站"};
        for (String candidate : candidates) {
            if (paragraph.contains(candidate)) {
                return candidate;
            }
        }
        return "场景 " + index;
    }

    private String inferTime(String paragraph) {
        if (paragraph.contains("夜")) {
            return "夜晚";
        }
        if (paragraph.contains("晨") || paragraph.contains("早")) {
            return "清晨";
        }
        if (paragraph.contains("黄昏") || paragraph.contains("傍晚")) {
            return "黄昏";
        }
        return "白天";
    }

    private String inferMood(String paragraph) {
        if (paragraph.contains("紧张") || paragraph.contains("追")) {
            return "紧张";
        }
        if (paragraph.contains("温柔") || paragraph.contains("微笑")) {
            return "温暖";
        }
        if (paragraph.contains("悬疑") || paragraph.contains("黑暗")) {
            return "悬疑";
        }
        return "电影感";
    }

    private String suggestCameraMovement(int index) {
        String[] values = {"推镜头", "跟拍镜头", "中景静止镜头", "环绕镜头", "慢速摇镜头", "特写推进"};
        return values[index % values.length];
    }

    private String defaultAssetName(AssetType assetType, int index) {
        return switch (assetType) {
            case CHARACTER -> "角色 " + index;
            case BACKGROUND -> "背景 " + index;
            case PROP -> "道具 " + index;
        };
    }

    private String prefixForAssetType(AssetType assetType) {
        return switch (assetType) {
            case CHARACTER -> "char";
            case BACKGROUND -> "bg";
            case PROP -> "prop";
        };
    }

    private boolean hasAnyConfirmedKeyframe(ScriptProjectAggregate aggregate) {
        return aggregate.keyframes.stream().anyMatch(item -> item.selected);
    }

    private List<String> mapSourceIdsToAssetIds(ScriptProjectAggregate aggregate, AssetType assetType, List<String> sourceIds) {
        Map<String, String> bySourceId = aggregate.assets.stream()
                .filter(asset -> asset.assetType == assetType)
                .collect(Collectors.toMap(
                        asset -> stringValue(asset.metadata.get("sourceId"), asset.assetId),
                        asset -> asset.assetId,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<String> mapped = new ArrayList<>();
        for (String sourceId : sourceIds) {
            String assetId = bySourceId.get(sourceId);
            if (assetId != null) {
                mapped.add(assetId);
            }
        }
        if (!mapped.isEmpty()) {
            return mapped;
        }
        return aggregate.assets.stream()
                .filter(asset -> asset.assetType == assetType)
                .limit(assetType == AssetType.BACKGROUND ? 1 : 2)
                .map(asset -> asset.assetId)
                .toList();
    }

    private List<String> resolveSelectedKeyframesForShot(ScriptProjectAggregate aggregate, StoryboardShot shot) {
        return resolveSelectedKeyframesForAssetRefs(aggregate, shot.characterRefs, shot.backgroundRefs, shot.propRefs);
    }

    private List<String> resolveSelectedKeyframesForAssetRefs(
            ScriptProjectAggregate aggregate,
            List<String> characterRefs,
            List<String> backgroundRefs,
            List<String> propRefs
    ) {
        Set<String> assetIds = new LinkedHashSet<>();
        assetIds.addAll(characterRefs == null ? List.of() : characterRefs);
        assetIds.addAll(backgroundRefs == null ? List.of() : backgroundRefs);
        assetIds.addAll(propRefs == null ? List.of() : propRefs);
        return aggregate.keyframes.stream()
                .filter(item -> item.selected && assetIds.contains(item.assetId))
                .map(item -> item.keyframeId)
                .toList();
    }

    private String firstNonBlankLine(String text, String fallback) {
        return java.util.Arrays.stream((text == null ? "" : text).split("\\r?\\n"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private String resolveVisualStyleAnchor(String rawStyleKey) {
        return videoStylePresetRegistry.resolveAnchorForRead(rawStyleKey);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private String nextId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
