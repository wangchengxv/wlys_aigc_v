package com.example.aigc.service;

import com.example.aigc.dto.ScriptDocumentPayload;
import com.example.aigc.dto.UpdateAssetRequest;
import com.example.aigc.dto.UpdateScriptRequest;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.PipelineRun;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.enums.AssetStatus;
import com.example.aigc.enums.AssetType;
import com.example.aigc.enums.DocumentVersionType;
import com.example.aigc.enums.PipelineStatus;
import com.example.aigc.enums.PipelineType;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.enums.RevisionKind;
import com.example.aigc.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScriptWorkflowService {

    private final ScriptProjectService scriptProjectService;
    private final PromptTemplateService promptTemplateService;
    private final AiCapabilityRoutingService aiCapabilityRoutingService;
    private final ProviderHttpGateway providerHttpGateway;
    private final LocalAssetFileService localAssetFileService;
    private final ObjectMapper objectMapper;

    public ScriptWorkflowService(
            ScriptProjectService scriptProjectService,
            PromptTemplateService promptTemplateService,
            AiCapabilityRoutingService aiCapabilityRoutingService,
            ProviderHttpGateway providerHttpGateway,
            LocalAssetFileService localAssetFileService,
            ObjectMapper objectMapper
    ) {
        this.scriptProjectService = scriptProjectService;
        this.promptTemplateService = promptTemplateService;
        this.aiCapabilityRoutingService = aiCapabilityRoutingService;
        this.providerHttpGateway = providerHttpGateway;
        this.localAssetFileService = localAssetFileService;
        this.objectMapper = objectMapper;
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
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveText(aggregate.project.explicitTextModel);
        if (resolvedModel.systemFallback() || !resolvedModel.hasProvider()) {
            String requestedModel = stringValue(aggregate.project.explicitTextModel, "");
            String reason = requestedModel.isBlank()
                    ? "未配置可用的文本模型，请先在模型配置中启用支持 text 能力的模型"
                    : "未命中已配置的文本模型：" + requestedModel;
            throw new BizException(400, reason);
        }
        String systemPrompt = promptTemplateService.render(
                systemPath,
                Map.of("language", stringValue(aggregate.project.language, "中文")),
                "系统提示"
        );
        String userPrompt = promptTemplateService.render(
                userPath,
                Map.of(
                        "projectName", stringValue(aggregate.project.name, "未命名项目"),
                        "visualStyle", stringValue(aggregate.project.visualStyle, "电影感写实"),
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

        String systemPrompt = promptTemplateService.render(
                "prompts/script/refine-system.md",
                Map.of("language", stringValue(aggregate.project.language, "中文")),
                "你是一名专业的影视编剧与分镜策划师，请把用户原始剧本整理为适合视频生产的结构化 JSON。"
        );
        String userPath = briefPrompt == null ? "prompts/script/refine-user.md" : "prompts/script/refine-user-with-brief.md";
        Map<String, Object> userVars = new LinkedHashMap<>();
        userVars.put("projectName", stringValue(aggregate.project.name, "未命名项目"));
        userVars.put("visualStyle", stringValue(aggregate.project.visualStyle, "电影感写实"));
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
        String userPrompt = promptTemplateService.render(
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
        AiCapabilityRoutingService.ResolvedAiModel resolvedModel = aiCapabilityRoutingService.resolveImage(aggregate.project.explicitImageModel);
        String basePath = "keyframes/" + asset.assetId + "/" + generationBatchId + "/keyframe-" + index;
        if (!resolvedModel.hasProvider() || resolvedModel.apiKey() == null || resolvedModel.apiKey().isBlank()) {
            return createPlaceholderImage(aggregate.project.projectId, basePath + ".svg", prompt, asset.name);
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
                        Duration.ofSeconds(60)
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
                        Duration.ofSeconds(60)
                );
            }
            String url = parseImageUrl(responseBody);
            String b64 = parseBase64Image(responseBody);
            if (url != null) {
                return localAssetFileService.storeRemote(aggregate.project.projectId, basePath + ".png", "image/png", url);
            }
            if (b64 != null) {
                return localAssetFileService.storeBase64(aggregate.project.projectId, basePath + ".png", "image/png", b64);
            }
            if (resolvedModel.systemFallback()) {
                return createPlaceholderImage(aggregate.project.projectId, basePath + ".svg", prompt, asset.name);
            }
            throw new BizException(502, "图片模型未返回可用图片");
        } catch (Exception ex) {
            if (resolvedModel.systemFallback()) {
                return createPlaceholderImage(aggregate.project.projectId, basePath + ".svg", prompt, asset.name);
            }
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(502, "调用图片模型失败");
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
                    aggregate.project.visualStyle,
                    stringValue(source.get("name"), "主角"),
                    description
            );
            case BACKGROUND -> "%s风格视频背景，场景：%s，氛围：%s".formatted(
                    aggregate.project.visualStyle,
                    stringValue(source.get("name"), "核心场景"),
                    stringValue(source.get("description"), description)
            );
            case PROP -> "%s风格视频道具，名称：%s，材质/视觉重点：%s".formatted(
                    aggregate.project.visualStyle,
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
        return promptTemplateService.render(
                templatePath,
                Map.of(
                        "projectName", stringValue(aggregate.project.name, "未命名项目"),
                        "visualStyle", stringValue(aggregate.project.visualStyle, "电影感写实"),
                        "aspectRatio", stringValue(aggregate.project.aspectRatio, "16:9"),
                        "assetName", stringValue(asset.name, "资产"),
                        "assetDescription", stringValue(asset.description, "暂无描述"),
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
