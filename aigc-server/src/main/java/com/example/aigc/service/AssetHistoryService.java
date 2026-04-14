package com.example.aigc.service;

import com.example.aigc.entity.AssetGenerationHistory;
import com.example.aigc.entity.ExtractedAsset;
import com.example.aigc.entity.KeyframeRecord;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.enums.AssetHistoryType;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.jpa.AssetGenerationHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class AssetHistoryService {

    private final AssetGenerationHistoryRepository historyRepository;
    private final ScriptProjectService scriptProjectService;
    private final LocalAssetFileService localAssetFileService;

    public AssetHistoryService(
            AssetGenerationHistoryRepository historyRepository,
            ScriptProjectService scriptProjectService,
            LocalAssetFileService localAssetFileService
    ) {
        this.historyRepository = historyRepository;
        this.scriptProjectService = scriptProjectService;
        this.localAssetFileService = localAssetFileService;
    }

    @Transactional
    public AssetGenerationHistory appendSnapshot(
            String projectId,
            AssetHistoryType assetType,
            String referenceId,
            String fileId,
            String promptText,
            String modelName,
            String generationParamsJson
    ) {
        if (projectId == null || fileId == null || fileId.isBlank()) {
            return null;
        }
        AssetGenerationHistory row = new AssetGenerationHistory();
        row.setProjectId(projectId);
        row.setAssetType(assetType);
        row.setReferenceId(referenceId);
        row.setFileId(fileId);
        row.setPromptText(promptText);
        row.setModelName(modelName);
        row.setGenerationParamsJson(generationParamsJson);
        row.setCreatedAt(Instant.now());
        return historyRepository.save(row);
    }

    public List<AssetGenerationHistory> list(String projectId, AssetHistoryType assetType, String referenceId) {
        if (referenceId != null && !referenceId.isBlank() && assetType != null) {
            return historyRepository.findAllByProjectIdAndAssetTypeAndReferenceIdOrderByCreatedAtDesc(projectId, assetType, referenceId);
        }
        if (assetType != null) {
            return historyRepository.findAllByProjectIdAndAssetTypeOrderByCreatedAtDesc(projectId, assetType);
        }
        return historyRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional
    public AssetGenerationHistory restore(String projectId, long historyId) {
        AssetGenerationHistory hist = historyRepository.findById(historyId)
                .orElseThrow(() -> new BizException(404, "历史记录不存在"));
        if (!Objects.equals(projectId, hist.getProjectId())) {
            throw new BizException(403, "无权恢复该记录");
        }
        StoredFileRecord file = scriptProjectService.findStoredRecordByFileId(hist.getFileId());
        if (file == null) {
            throw new BizException(404, "历史文件不存在");
        }
        if (!localAssetFileService.exists(file)) {
            throw new BizException(404, "历史文件已丢失");
        }
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId);
        scriptProjectService.upsertFile(aggregate, file);

        switch (hist.getAssetType()) {
            case TURNAROUND -> {
                ExtractedAsset asset = findAsset(aggregate, hist.getReferenceId());
                asset.turnaroundImageFileId = file.fileId;
                asset.updatedAt = Instant.now();
            }
            case STORYBOARD -> {
                ExtractedAsset asset = findAsset(aggregate, hist.getReferenceId());
                asset.storyboardImageFileId = file.fileId;
                asset.updatedAt = Instant.now();
            }
            case THREE_VIEW -> {
                ExtractedAsset asset = findAsset(aggregate, hist.getReferenceId());
                asset.threeViewImageFileId = file.fileId;
                asset.updatedAt = Instant.now();
            }
            case KEYFRAME -> {
                KeyframeRecord kf = findKeyframe(aggregate, hist.getReferenceId());
                kf.imageFileId = file.fileId;
                kf.updatedAt = Instant.now();
                kf.status = "SUCCESS";
            }
            case VIDEO -> {
                VideoSegmentTask task = findVideoTask(aggregate, hist.getReferenceId());
                task.resultVideoFileId = file.fileId;
            }
            case STORYBOARD_CROP -> {
                String[] parts = parseCropReference(hist.getReferenceId());
                String assetId = parts[0];
                int panelIndex = Integer.parseInt(parts[1]);
                for (StoryboardShot shot : aggregate.shots) {
                    if (Objects.equals(shot.storyboardAssetId, assetId)
                            && shot.storyboardCropIndex != null
                            && shot.storyboardCropIndex == panelIndex
                            && "CROPPED_PANEL".equalsIgnoreCase(String.valueOf(shot.firstFrameMode))) {
                        shot.storyboardCropFileId = file.fileId;
                        shot.updatedAt = Instant.now();
                    }
                }
            }
            case GROUP_SCENE -> throw new BizException(400, "群像暂不支持从历史版本恢复");
        }
        aggregate.project.updatedAt = Instant.now();
        scriptProjectService.save(aggregate);
        return hist;
    }

    private ExtractedAsset findAsset(ScriptProjectAggregate aggregate, String assetId) {
        return aggregate.assets.stream()
                .filter(a -> Objects.equals(a.assetId, assetId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "资产不存在"));
    }

    private KeyframeRecord findKeyframe(ScriptProjectAggregate aggregate, String keyframeId) {
        return aggregate.keyframes.stream()
                .filter(k -> Objects.equals(k.keyframeId, keyframeId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "关键帧不存在"));
    }

    private VideoSegmentTask findVideoTask(ScriptProjectAggregate aggregate, String segmentTaskId) {
        return aggregate.videoTasks.stream()
                .filter(t -> Objects.equals(t.segmentTaskId, segmentTaskId))
                .findFirst()
                .orElseThrow(() -> new BizException(404, "视频任务不存在"));
    }

    private String[] parseCropReference(String referenceId) {
        if (referenceId == null || !referenceId.contains(":")) {
            throw new BizException(400, "历史记录 referenceId 无效");
        }
        int idx = referenceId.lastIndexOf(':');
        String assetId = referenceId.substring(0, idx);
        String panel = referenceId.substring(idx + 1);
        return new String[]{assetId, panel};
    }
}
