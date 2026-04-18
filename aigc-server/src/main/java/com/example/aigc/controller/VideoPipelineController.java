package com.example.aigc.controller;

import com.example.aigc.config.RequestContextAttributes;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.ContentReviewDecisionRequest;
import com.example.aigc.dto.ContentReviewStatusResponse;
import com.example.aigc.dto.ContentReviewSubmitRequest;
import com.example.aigc.dto.PipelineStatusData;
import com.example.aigc.dto.RollbackPromptRequest;
import com.example.aigc.dto.UpdateShotRequest;
import com.example.aigc.dto.VideoEditDraftResponse;
import com.example.aigc.dto.VideoEditDraftSaveRequest;
import com.example.aigc.dto.VideoEditPublishRequest;
import com.example.aigc.entity.DubbingTask;
import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.FinalCompositionTask;
import com.example.aigc.entity.LipSyncTask;
import com.example.aigc.entity.StoryboardShot;
import com.example.aigc.entity.VideoEditRenderTask;
import com.example.aigc.entity.VideoSegmentTask;
import com.example.aigc.service.ContentReviewService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.ScriptProductionOrchestrator;
import com.example.aigc.service.ScriptWorkflowService;
import com.example.aigc.service.VideoEditingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/script-projects/{projectId}")
public class VideoPipelineController {

    private final ScriptWorkflowService scriptWorkflowService;
    private final ScriptProductionOrchestrator scriptProductionOrchestrator;
    private final ContentReviewService contentReviewService;
    private final VideoEditingService videoEditingService;

    public VideoPipelineController(
            ScriptWorkflowService scriptWorkflowService,
            ScriptProductionOrchestrator scriptProductionOrchestrator,
            ContentReviewService contentReviewService,
            VideoEditingService videoEditingService
    ) {
        this.scriptWorkflowService = scriptWorkflowService;
        this.scriptProductionOrchestrator = scriptProductionOrchestrator;
        this.contentReviewService = contentReviewService;
        this.videoEditingService = videoEditingService;
    }

    @PostMapping("/shots/split")
    public ApiResponse<List<StoryboardShot>> split(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.splitShots(projectId));
    }

    @GetMapping("/shots")
    public ApiResponse<List<StoryboardShot>> shots(@PathVariable String projectId) {
        return ApiResponse.ok(scriptWorkflowService.listShots(projectId));
    }

    @PutMapping("/shots/{shotId}")
    public ApiResponse<StoryboardShot> updateShot(
            @PathVariable String projectId,
            @PathVariable String shotId,
            @Valid @RequestBody UpdateShotRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.updateShot(projectId, shotId, request));
    }

    @PostMapping("/shots/{shotId}/visual-prompt/rollback")
    public ApiResponse<StoryboardShot> rollbackShotVisualPrompt(
            @PathVariable String projectId,
            @PathVariable String shotId,
            @RequestBody RollbackPromptRequest request
    ) {
        return ApiResponse.ok(scriptWorkflowService.rollbackShotVisualPrompt(projectId, shotId, request));
    }

    @PostMapping("/video/generate")
    public ApiResponse<PipelineStatusData> generateVideo(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startVideoGeneration(projectId));
    }

    @GetMapping("/video/tasks")
    public ApiResponse<List<VideoSegmentTask>> tasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listVideoTasks(projectId));
    }

    @PostMapping("/video/tasks/{segmentTaskId}/retry")
    public ApiResponse<PipelineStatusData> retry(
            @PathVariable String projectId,
            @PathVariable String segmentTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryVideoTask(projectId, segmentTaskId));
    }

    @PostMapping("/dubbing/generate")
    public ApiResponse<PipelineStatusData> generateDubbing(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startDubbingGeneration(projectId));
    }

    @GetMapping("/dubbing/tasks")
    public ApiResponse<List<DubbingTask>> dubbingTasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listDubbingTasks(projectId));
    }

    @PostMapping("/dubbing/tasks/{dubbingTaskId}/retry")
    public ApiResponse<PipelineStatusData> retryDubbing(
            @PathVariable String projectId,
            @PathVariable String dubbingTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryDubbingTask(projectId, dubbingTaskId));
    }

    @PostMapping("/lip-sync/generate")
    public ApiResponse<PipelineStatusData> generateLipSync(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startLipSyncGeneration(projectId));
    }

    @GetMapping("/lip-sync/tasks")
    public ApiResponse<List<LipSyncTask>> lipSyncTasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listLipSyncTasks(projectId));
    }

    @PostMapping("/lip-sync/tasks/{lipSyncTaskId}/retry")
    public ApiResponse<PipelineStatusData> retryLipSync(
            @PathVariable String projectId,
            @PathVariable String lipSyncTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryLipSyncTask(projectId, lipSyncTaskId));
    }

    @PostMapping("/final-composition/generate")
    public ApiResponse<PipelineStatusData> generateFinalComposition(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startFinalComposition(projectId));
    }

    @GetMapping("/video-editing/draft")
    public ApiResponse<VideoEditDraftResponse> videoEditDraft(@PathVariable String projectId) {
        return ApiResponse.ok(videoEditingService.getDraft(projectId));
    }

    @PutMapping("/video-editing/draft")
    public ApiResponse<VideoEditDraftResponse> saveVideoEditDraft(
            @PathVariable String projectId,
            @RequestBody(required = false) VideoEditDraftSaveRequest request
    ) {
        return ApiResponse.ok(videoEditingService.saveDraft(
                projectId,
                request == null ? new VideoEditDraftSaveRequest() : request
        ));
    }

    @PostMapping("/video-editing/draft/reset")
    public ApiResponse<VideoEditDraftResponse> resetVideoEditDraft(@PathVariable String projectId) {
        return ApiResponse.ok(videoEditingService.resetDraft(projectId));
    }

    @PostMapping("/video-editing/render/preview")
    public ApiResponse<PipelineStatusData> renderVideoEditPreview(@PathVariable String projectId) {
        PipelineStatusData data = scriptProductionOrchestrator.startVideoEditPreview(projectId);
        return ApiResponse.ok(data);
    }

    @PostMapping("/video-editing/render/publish")
    public ApiResponse<PipelineStatusData> publishVideoEdit(
            @PathVariable String projectId,
            @RequestBody(required = false) VideoEditPublishRequest request
    ) {
        PipelineStatusData data = scriptProductionOrchestrator.startVideoEditPublish(
                projectId,
                request == null ? new VideoEditPublishRequest() : request
        );
        return ApiResponse.ok(data);
    }

    @GetMapping("/video-editing/render/tasks")
    public ApiResponse<List<VideoEditRenderTask>> videoEditRenderTasks(@PathVariable String projectId) {
        return ApiResponse.ok(videoEditingService.listRenderTasks(projectId));
    }

    @PostMapping("/video-editing/render/tasks/{renderTaskId}/retry")
    public ApiResponse<PipelineStatusData> retryVideoEditRender(
            @PathVariable String projectId,
            @PathVariable String renderTaskId
    ) {
        PipelineStatusData data = scriptProductionOrchestrator.retryVideoEditRenderTask(projectId, renderTaskId);
        return ApiResponse.ok(data);
    }

    @GetMapping("/final-composition/tasks")
    public ApiResponse<List<FinalCompositionTask>> finalCompositionTasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listFinalCompositionTasks(projectId));
    }

    @PostMapping("/final-composition/tasks/{finalCompositionTaskId}/retry")
    public ApiResponse<PipelineStatusData> retryFinalComposition(
            @PathVariable String projectId,
            @PathVariable String finalCompositionTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryFinalCompositionTask(projectId, finalCompositionTaskId));
    }

    @PostMapping("/export-package/generate")
    public ApiResponse<PipelineStatusData> generateExportPackage(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.startExportPackage(projectId));
    }

    @GetMapping("/export-package/tasks")
    public ApiResponse<List<ExportPackageTask>> exportPackageTasks(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.listExportPackageTasks(projectId));
    }

    @GetMapping("/content-review")
    public ApiResponse<ContentReviewStatusResponse> contentReviewStatus(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String projectId
    ) {
        return ApiResponse.ok(contentReviewService.getStatus(projectId, userContext));
    }

    @PostMapping("/content-review/submit")
    public ApiResponse<ContentReviewStatusResponse> submitContentReview(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String projectId,
            @RequestBody(required = false) ContentReviewSubmitRequest request
    ) {
        return ApiResponse.ok(contentReviewService.submit(
                projectId,
                userContext,
                request == null ? new ContentReviewSubmitRequest(null) : request
        ));
    }

    @PostMapping("/content-review/approve")
    public ApiResponse<ContentReviewStatusResponse> approveContentReview(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String projectId,
            @RequestBody(required = false) ContentReviewDecisionRequest request
    ) {
        return ApiResponse.ok(contentReviewService.approve(
                projectId,
                userContext,
                request == null ? new ContentReviewDecisionRequest(null) : request
        ));
    }

    @PostMapping("/content-review/reject")
    public ApiResponse<ContentReviewStatusResponse> rejectContentReview(
            @RequestAttribute(RequestContextAttributes.CURRENT_USER_CONTEXT) RequestUserContext userContext,
            @PathVariable String projectId,
            @RequestBody(required = false) ContentReviewDecisionRequest request
    ) {
        return ApiResponse.ok(contentReviewService.reject(
                projectId,
                userContext,
                request == null ? new ContentReviewDecisionRequest(null) : request
        ));
    }

    @PostMapping("/export-package/tasks/{exportPackageTaskId}/retry")
    public ApiResponse<PipelineStatusData> retryExportPackage(
            @PathVariable String projectId,
            @PathVariable String exportPackageTaskId
    ) {
        return ApiResponse.ok(scriptProductionOrchestrator.retryExportPackageTask(projectId, exportPackageTaskId));
    }

    @GetMapping("/pipeline-status")
    public ApiResponse<PipelineStatusData> pipelineStatus(@PathVariable String projectId) {
        return ApiResponse.ok(scriptProductionOrchestrator.getPipelineStatus(projectId));
    }
}
