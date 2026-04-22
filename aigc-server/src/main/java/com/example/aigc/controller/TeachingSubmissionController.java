package com.example.aigc.controller;

import com.example.aigc.dto.*;
import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.ReviewRecord;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.TeachingSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class TeachingSubmissionController {
    private final TeachingSubmissionService teachingSubmissionService;
    private final RequestAuthService requestAuthService;

    public TeachingSubmissionController(
            TeachingSubmissionService teachingSubmissionService,
            RequestAuthService requestAuthService
    ) {
        this.teachingSubmissionService = teachingSubmissionService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/submissions")
    public ApiResponse<List<AssignmentSubmission>> listByAssignment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.listByAssignment(assignmentId, userContext));
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/submissions")
    public ApiResponse<AssignmentSubmission> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId,
            @Valid @RequestBody SubmissionCreateRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.create(assignmentId, userContext, request));
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/batch-review")
    public ApiResponse<BatchReviewSubmissionsResponse> batchReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId,
            @Valid @RequestBody BatchReviewSubmissionsRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.batchReview(assignmentId, userContext, request));
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/stats")
    public ApiResponse<AssignmentStatsResponse> getStats(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.getAssignmentStats(assignmentId, userContext));
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/export")
    public ResponseEntity<byte[]> exportGrades(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );

        AssignmentStatsResponse stats = teachingSubmissionService.getAssignmentStats(assignmentId, userContext);
        List<AssignmentSubmission> submissions = teachingSubmissionService.listByAssignment(assignmentId, userContext);

        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF");
        csv.append("学生姓名,学生ID,班级,项目ID,提交说明,提交时间,评分状态,分数,评语,评审时间\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        for (AssignmentSubmission s : submissions) {
            csv.append(escapeCsv(s.studentUserName != null ? s.studentUserName : "")).append(",");
            csv.append(escapeCsv(s.studentUserId != null ? s.studentUserId : "")).append(",");
            csv.append(escapeCsv("")).append(",");
            csv.append(escapeCsv(s.projectId != null ? s.projectId : "")).append(",");
            csv.append(escapeCsv(s.note != null ? s.note : "")).append(",");
            csv.append(escapeCsv(s.submittedAt != null ? formatter.format(s.submittedAt) : "")).append(",");
            csv.append(escapeCsv(statusLabel(s.status))).append(",");
            csv.append(s.score != null ? s.score : "").append(",");
            csv.append(escapeCsv(s.reviewComment != null ? s.reviewComment : "")).append(",");
            csv.append(escapeCsv(s.reviewedAt != null ? formatter.format(s.reviewedAt) : "")).append("\n");
        }

        String filename = "assignment-" + assignmentId + "-grades-" + System.currentTimeMillis() + ".csv";
        String encodedFilename = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/api/v1/submissions/{submissionId}/review")
    public ApiResponse<AssignmentSubmission> review(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String submissionId,
            @Valid @RequestBody SubmissionReviewRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.review(submissionId, userContext, request));
    }

    @GetMapping("/api/v1/submissions/{submissionId}/reviews")
    public ApiResponse<List<ReviewRecord>> listReviews(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String submissionId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingSubmissionService.listReviews(submissionId, userContext));
    }

    private String statusLabel(com.example.aigc.enums.SubmissionStatus status) {
        if (status == null) return "";
        return switch (status) {
            case SUBMITTED -> "待评分";
            case RETURNED -> "退回修改";
            case REVIEWED -> "已评分";
        };
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}