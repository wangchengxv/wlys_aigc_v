package com.example.aigc.service;

import com.example.aigc.dto.ContentReviewDecisionRequest;
import com.example.aigc.dto.ContentReviewStatusResponse;
import com.example.aigc.dto.ContentReviewSubmitRequest;
import com.example.aigc.entity.ContentReviewRecord;
import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.StoredFileRecord;
import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContentReviewService {
    private final ScriptProjectService scriptProjectService;
    private final AuthorizationService authorizationService;
    private final AuditLogService auditLogService;
    private final LocalAssetFileService localAssetFileService;

    public ContentReviewService(
            ScriptProjectService scriptProjectService,
            AuthorizationService authorizationService,
            AuditLogService auditLogService,
            LocalAssetFileService localAssetFileService
    ) {
        this.scriptProjectService = scriptProjectService;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
        this.localAssetFileService = localAssetFileService;
    }

    public ContentReviewStatusResponse getStatus(String projectId, RequestUserContext actor) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId, actor);
        return toResponse(aggregate, actor);
    }

    public ContentReviewStatusResponse submit(String projectId, RequestUserContext actor, ContentReviewSubmitRequest request) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId, actor);
        ScriptProject project = aggregate.project;
        if (!authorizationService.canSubmitProjectReview(project, actor)) {
            throw new BizException(403, "无权提交该项目审核");
        }
        if (!hasAvailableExportPackage(aggregate)) {
            throw new BizException(400, "当前项目缺少可用导出包，请先完成导出包生成后再提交审核");
        }

        ContentReviewStatus currentStatus = normalizedStatus(project);
        if (currentStatus == ContentReviewStatus.PENDING) {
            throw new BizException(400, "当前项目已在审核中，无需重复提交");
        }
        if (currentStatus == ContentReviewStatus.APPROVED) {
            throw new BizException(400, "当前项目已审核通过，如需重新审核请先生成新的导出包");
        }

        Instant now = Instant.now();
        int nextResubmitCount = currentStatus == ContentReviewStatus.REJECTED
                ? safeInt(project.reviewResubmitCount) + 1
                : 0;

        ContentReviewRecord record = new ContentReviewRecord();
        record.reviewId = nextReviewId();
        record.projectId = project.projectId;
        record.status = ContentReviewStatus.PENDING;
        record.submitterUserId = actor.userId();
        record.submitterUserName = firstNonBlank(actor.userName(), actor.userId());
        record.submissionComment = blankToNull(request == null ? null : request.comment());
        record.reviewerUserId = null;
        record.reviewerUserName = null;
        record.reviewComment = null;
        record.resubmitCount = nextResubmitCount;
        record.submittedAt = now;
        record.reviewedAt = null;
        record.createdAt = now;
        record.updatedAt = now;

        aggregate.contentReviewRecords.add(record);
        applyProjectReviewSnapshot(project, record);
        project.contentReviewStatus = ContentReviewStatus.PENDING;
        project.reviewResubmitCount = nextResubmitCount;
        project.latestReviewComment = null;
        project.reviewSubmittedAt = now;
        project.reviewedAt = null;
        project.reviewerUserId = null;
        project.reviewerUserName = null;
        scriptProjectService.save(aggregate);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reviewId", record.reviewId);
        details.put("status", record.status.name());
        details.put("resubmitCount", nextResubmitCount);
        details.put("comment", record.submissionComment);
        auditLogService.record(
                actor,
                currentStatus == ContentReviewStatus.REJECTED ? "PROJECT_REVIEW_RESUBMITTED" : "PROJECT_REVIEW_SUBMITTED",
                "SCRIPT_PROJECT",
                project.projectId,
                details
        );
        return toResponse(aggregate, actor);
    }

    public ContentReviewStatusResponse approve(String projectId, RequestUserContext actor, ContentReviewDecisionRequest request) {
        return decide(projectId, actor, request, ContentReviewStatus.APPROVED);
    }

    public ContentReviewStatusResponse reject(String projectId, RequestUserContext actor, ContentReviewDecisionRequest request) {
        String comment = blankToNull(request == null ? null : request.comment());
        if (comment == null) {
            throw new BizException(400, "驳回审核时必须填写审核意见");
        }
        return decide(projectId, actor, new ContentReviewDecisionRequest(comment), ContentReviewStatus.REJECTED);
    }

    private ContentReviewStatusResponse decide(
            String projectId,
            RequestUserContext actor,
            ContentReviewDecisionRequest request,
            ContentReviewStatus targetStatus
    ) {
        ScriptProjectAggregate aggregate = scriptProjectService.require(projectId, actor);
        ScriptProject project = aggregate.project;
        if (!authorizationService.canProcessProjectReview(project, actor)) {
            if (Objects.equals(project.ownerId, actor.userId())) {
                throw new BizException(403, "不能审核自己提交的项目");
            }
            throw new BizException(403, "无权处理该项目审核");
        }
        ContentReviewRecord currentRecord = requirePendingRecord(aggregate);
        Instant now = Instant.now();
        currentRecord.status = targetStatus;
        currentRecord.reviewerUserId = actor.userId();
        currentRecord.reviewerUserName = firstNonBlank(actor.userName(), actor.userId());
        currentRecord.reviewComment = blankToNull(request == null ? null : request.comment());
        currentRecord.reviewedAt = now;
        currentRecord.updatedAt = now;

        applyProjectReviewSnapshot(project, currentRecord);
        project.contentReviewStatus = targetStatus;
        project.latestReviewComment = currentRecord.reviewComment;
        project.reviewedAt = now;
        project.reviewerUserId = currentRecord.reviewerUserId;
        project.reviewerUserName = currentRecord.reviewerUserName;
        scriptProjectService.save(aggregate);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reviewId", currentRecord.reviewId);
        details.put("status", currentRecord.status.name());
        details.put("reviewerUserId", currentRecord.reviewerUserId);
        details.put("comment", currentRecord.reviewComment);
        auditLogService.record(
                actor,
                targetStatus == ContentReviewStatus.APPROVED ? "PROJECT_REVIEW_APPROVED" : "PROJECT_REVIEW_REJECTED",
                "SCRIPT_PROJECT",
                project.projectId,
                details
        );
        return toResponse(aggregate, actor);
    }

    private ContentReviewStatusResponse toResponse(ScriptProjectAggregate aggregate, RequestUserContext actor) {
        ScriptProject project = aggregate.project;
        List<ContentReviewRecord> records = aggregate.contentReviewRecords == null
                ? List.of()
                : aggregate.contentReviewRecords.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((ContentReviewRecord item) -> item.createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        ContentReviewStatus status = normalizedStatus(project);
        boolean exportPackageReady = hasAvailableExportPackage(aggregate);
        boolean canSubmit = authorizationService.canSubmitProjectReview(project, actor)
                && exportPackageReady
                && status != ContentReviewStatus.PENDING
                && status != ContentReviewStatus.APPROVED;
        boolean canProcess = authorizationService.canProcessProjectReview(project, actor)
                && status == ContentReviewStatus.PENDING;
        return new ContentReviewStatusResponse(
                project.projectId,
                status,
                exportPackageReady,
                project.currentReviewId,
                safeInt(project.reviewResubmitCount),
                project.latestReviewComment,
                project.reviewSubmittedAt,
                project.reviewedAt,
                project.reviewerUserId,
                project.reviewerUserName,
                canSubmit,
                canProcess,
                new ArrayList<>(records)
        );
    }

    private void applyProjectReviewSnapshot(ScriptProject project, ContentReviewRecord record) {
        project.currentReviewId = record.reviewId;
        project.reviewSubmittedAt = record.submittedAt;
        project.reviewedAt = record.reviewedAt;
        project.reviewerUserId = record.reviewerUserId;
        project.reviewerUserName = record.reviewerUserName;
        project.latestReviewComment = record.reviewComment;
        project.reviewResubmitCount = safeInt(record.resubmitCount);
    }

    private ContentReviewRecord requirePendingRecord(ScriptProjectAggregate aggregate) {
        return aggregate.contentReviewRecords.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.reviewId, aggregate.project.currentReviewId))
                .findFirst()
                .filter(item -> item.status == ContentReviewStatus.PENDING)
                .orElseThrow(() -> new BizException(400, "当前项目没有待处理的审核记录"));
    }

    private boolean hasAvailableExportPackage(ScriptProjectAggregate aggregate) {
        for (int index = aggregate.exportPackageTasks.size() - 1; index >= 0; index--) {
            ExportPackageTask task = aggregate.exportPackageTasks.get(index);
            if (task == null || task.status != ExportPackageTaskStatus.SUCCESS) {
                continue;
            }
            StoredFileRecord archiveFile = scriptProjectService.findFile(aggregate, task.resultArchiveFileId);
            if (archiveFile != null && localAssetFileService.exists(archiveFile)) {
                return true;
            }
        }
        return false;
    }

    private ContentReviewStatus normalizedStatus(ScriptProject project) {
        return project == null || project.contentReviewStatus == null
                ? ContentReviewStatus.NOT_SUBMITTED
                : project.contentReviewStatus;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        String a = blankToNull(first);
        if (a != null) {
            return a;
        }
        return blankToNull(second);
    }

    private String nextReviewId() {
        return "project-review-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
