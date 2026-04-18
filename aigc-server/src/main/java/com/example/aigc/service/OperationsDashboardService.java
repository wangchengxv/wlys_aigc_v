package com.example.aigc.service;

import com.example.aigc.dto.OperationsDashboardActivityDto;
import com.example.aigc.dto.OperationsDashboardMetricDto;
import com.example.aigc.dto.OperationsDashboardResponse;
import com.example.aigc.dto.OperationsDashboardStatusBucketDto;
import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.entity.ExportPackageTask;
import com.example.aigc.entity.ScriptProjectAggregate;
import com.example.aigc.entity.ScriptProjectSummary;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.enums.ContentReviewStatus;
import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.enums.ProjectStatus;
import com.example.aigc.repository.AssignmentSubmissionRepository;
import com.example.aigc.repository.ScriptProjectRepository;
import com.example.aigc.repository.TeachingAssignmentRepository;
import com.example.aigc.repository.TeachingCourseRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OperationsDashboardService {
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final TeachingCourseRepository teachingCourseRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ScriptProjectRepository scriptProjectRepository;
    private final AuditLogService auditLogService;

    public OperationsDashboardService(
            TeachingCourseRepository teachingCourseRepository,
            TeachingAssignmentRepository teachingAssignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            ScriptProjectRepository scriptProjectRepository,
            AuditLogService auditLogService
    ) {
        this.teachingCourseRepository = teachingCourseRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.scriptProjectRepository = scriptProjectRepository;
        this.auditLogService = auditLogService;
    }

    public OperationsDashboardResponse getDashboard() {
        List<TeachingCourse> courses = teachingCourseRepository.findAll();
        List<TeachingAssignment> assignments = new ArrayList<>();
        List<AssignmentSubmission> submissions = new ArrayList<>();
        List<ScriptProjectSummary> projectSummaries = scriptProjectRepository.findAll(false);

        for (TeachingCourse course : courses) {
            List<TeachingAssignment> courseAssignments = teachingAssignmentRepository.findAllByCourseId(course.courseId);
            assignments.addAll(courseAssignments);
            for (TeachingAssignment assignment : courseAssignments) {
                submissions.addAll(assignmentSubmissionRepository.findAllByAssignmentId(assignment.assignmentId));
            }
        }

        long exportPackageCount = 0;
        List<OperationsDashboardActivityDto> exportActivities = new ArrayList<>();
        Map<String, String> projectNameById = new LinkedHashMap<>();
        for (ScriptProjectSummary projectSummary : projectSummaries) {
            projectNameById.put(projectSummary.projectId, fallbackProjectName(projectSummary.name, projectSummary.projectId));
            ScriptProjectAggregate aggregate = scriptProjectRepository.findById(projectSummary.projectId).orElse(null);
            if (aggregate == null || aggregate.exportPackageTasks == null) {
                continue;
            }
            exportPackageCount += aggregate.exportPackageTasks.size();
            exportActivities.addAll(toExportActivities(aggregate, projectSummary));
        }

        long pendingReviewCount = projectSummaries.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.contentReviewStatus == ContentReviewStatus.PENDING)
                .count();

        List<OperationsDashboardMetricDto> overviewCards = List.of(
                metric("courseCount", "课程数", courses.size(), "COURSE", "/settings/courses", "平台当前课程总数"),
                metric("assignmentCount", "作业数", assignments.size(), "ASSIGNMENT", "/settings/courses", "课程下已发布作业总数"),
                metric("submissionCount", "提交数", submissions.size(), "SUBMISSION", "/settings/submissions", "学生作业提交通量"),
                metric("projectCount", "剧本工程数", projectSummaries.size(), "SCRIPT_PROJECT", "/projects", "当前未删除剧本工程数"),
                metric("exportPackageCount", "导出包数", exportPackageCount, "EXPORT_PACKAGE_TASK", null, "项目导出包任务总数"),
                metric("pendingReviewCount", "待审核项目数", pendingReviewCount, "SCRIPT_PROJECT", null, "内容审核状态为待审核的项目数")
        );

        List<OperationsDashboardStatusBucketDto> statusDistribution = buildStatusDistribution(projectSummaries);
        List<OperationsDashboardActivityDto> recentActivities = buildRecentActivities(projectNameById, exportActivities);

        return new OperationsDashboardResponse(
                Instant.now(),
                overviewCards,
                statusDistribution,
                recentActivities
        );
    }

    private List<OperationsDashboardStatusBucketDto> buildStatusDistribution(List<ScriptProjectSummary> projectSummaries) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("draft", 0L);
        counts.put("generating", 0L);
        counts.put("finalReady", 0L);
        counts.put("pendingReview", 0L);
        counts.put("approved", 0L);
        counts.put("rejected", 0L);
        counts.put("failed", 0L);

        for (ScriptProjectSummary projectSummary : projectSummaries) {
            String bucket = statusBucket(projectSummary);
            counts.computeIfPresent(bucket, (key, value) -> value + 1);
        }

        return List.of(
                statusBucket("draft", "草稿", counts.get("draft"), "SCRIPT_PROJECT", null, "项目仍停留在草稿阶段"),
                statusBucket("generating", "生成中", counts.get("generating"), "SCRIPT_PROJECT", null, "项目处于创作或生产处理中"),
                statusBucket("finalReady", "成片完成", counts.get("finalReady"), "SCRIPT_PROJECT", null, "项目已完成成片或导出准备"),
                statusBucket("pendingReview", "待审核", counts.get("pendingReview"), "SCRIPT_PROJECT", null, "项目已提交内容审核，等待处理"),
                statusBucket("approved", "审核通过", counts.get("approved"), "SCRIPT_PROJECT", null, "项目内容审核已通过"),
                statusBucket("rejected", "审核驳回", counts.get("rejected"), "SCRIPT_PROJECT", null, "项目内容审核被驳回"),
                statusBucket("failed", "异常失败", counts.get("failed"), "SCRIPT_PROJECT", null, "项目在生产链路中出现失败")
        );
    }

    private List<OperationsDashboardActivityDto> buildRecentActivities(
            Map<String, String> projectNameById,
            List<OperationsDashboardActivityDto> exportActivities
    ) {
        List<OperationsDashboardActivityDto> activities = new ArrayList<>();
        for (AuditLogRecord record : auditLogService.listRecent(null, null, null)) {
            OperationsDashboardActivityDto activity = toAuditActivity(record, projectNameById);
            if (activity != null) {
                activities.add(activity);
            }
        }
        activities.addAll(exportActivities);
        return activities.stream()
                .sorted(Comparator.comparing(
                        OperationsDashboardActivityDto::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(RECENT_ACTIVITY_LIMIT)
                .toList();
    }

    private OperationsDashboardActivityDto toAuditActivity(
            AuditLogRecord record,
            Map<String, String> projectNameById
    ) {
        if (record == null || record.action == null || record.action.isBlank()) {
            return null;
        }
        String action = record.action.trim();
        String label = switch (action) {
            case "PROJECT_REVIEW_SUBMITTED" -> "项目提审";
            case "PROJECT_REVIEW_RESUBMITTED" -> "项目重新提审";
            case "PROJECT_REVIEW_APPROVED" -> "审核通过";
            case "PROJECT_REVIEW_REJECTED" -> "审核驳回";
            default -> null;
        };
        if (label == null) {
            return null;
        }
        String projectName = projectNameById.getOrDefault(record.entityId, fallbackProjectName(null, record.entityId));
        String actorName = blankToNull(record.actorUserName);
        String summary = actorName == null
                ? projectName + "：" + label
                : actorName + "处理项目《" + projectName + "》：" + label;
        return new OperationsDashboardActivityDto(
                action + ":" + safeId(record.entityId) + ":" + safeInstant(record.createdAt),
                action,
                label,
                summary,
                blankToNull(record.entityType),
                blankToNull(record.entityId),
                record.entityId == null ? null : "/projects/" + record.entityId,
                record.createdAt
        );
    }

    private List<OperationsDashboardActivityDto> toExportActivities(
            ScriptProjectAggregate aggregate,
            ScriptProjectSummary summary
    ) {
        List<OperationsDashboardActivityDto> activities = new ArrayList<>();
        if (aggregate.exportPackageTasks == null) {
            return activities;
        }
        String projectName = fallbackProjectName(summary == null ? null : summary.name, aggregate.project.projectId);
        for (ExportPackageTask task : aggregate.exportPackageTasks) {
            if (task == null || task.status != ExportPackageTaskStatus.SUCCESS || task.finishedAt == null) {
                continue;
            }
            activities.add(new OperationsDashboardActivityDto(
                    "EXPORT_PACKAGE_COMPLETED:" + safeId(task.exportPackageTaskId),
                    "EXPORT_PACKAGE_COMPLETED",
                    "导出包完成",
                    "项目《" + projectName + "》导出包已生成完成",
                    "EXPORT_PACKAGE_TASK",
                    task.exportPackageTaskId,
                    "/projects/" + aggregate.project.projectId,
                    task.finishedAt
            ));
        }
        return activities;
    }

    private String statusBucket(ScriptProjectSummary projectSummary) {
        if (projectSummary == null) {
            return "draft";
        }
        ContentReviewStatus reviewStatus = projectSummary.contentReviewStatus;
        if (reviewStatus == ContentReviewStatus.PENDING) {
            return "pendingReview";
        }
        if (reviewStatus == ContentReviewStatus.APPROVED) {
            return "approved";
        }
        if (reviewStatus == ContentReviewStatus.REJECTED) {
            return "rejected";
        }

        ProjectStatus status = projectSummary.status == null ? ProjectStatus.DRAFT : projectSummary.status;
        return switch (status) {
            case DRAFT -> "draft";
            case FINAL_COMPOSITION_READY, EXPORT_PACKAGE_READY, COMPLETED -> "finalReady";
            case PARTIAL_FAILED, FAILED -> "failed";
            default -> "generating";
        };
    }

    private OperationsDashboardMetricDto metric(
            String key,
            String label,
            long value,
            String entityType,
            String link,
            String summary
    ) {
        return new OperationsDashboardMetricDto(key, label, value, entityType, link, summary);
    }

    private OperationsDashboardStatusBucketDto statusBucket(
            String key,
            String label,
            long count,
            String entityType,
            String link,
            String summary
    ) {
        return new OperationsDashboardStatusBucketDto(key, label, count, entityType, link, summary);
    }

    private String fallbackProjectName(String name, String projectId) {
        String normalized = blankToNull(name);
        return normalized != null ? normalized : "项目 " + safeId(projectId);
    }

    private String safeId(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private String safeInstant(Instant value) {
        return value == null ? "unknown" : value.toString();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
