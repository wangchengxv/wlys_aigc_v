package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.SubmissionCreateRequest;
import com.example.aigc.dto.SubmissionReviewRequest;
import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.ReviewRecord;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.TeachingSubmissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
