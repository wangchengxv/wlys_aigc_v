package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.AssignmentCreateRequest;
import com.example.aigc.dto.TeachingAssignmentStatusUpdateRequest;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.TeachingAssignmentService;
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
@RequestMapping("/api/v1/courses/{courseId}/assignments")
public class TeachingAssignmentController {
    private final TeachingAssignmentService teachingAssignmentService;
    private final RequestAuthService requestAuthService;

    public TeachingAssignmentController(
            TeachingAssignmentService teachingAssignmentService,
            RequestAuthService requestAuthService
    ) {
        this.teachingAssignmentService = teachingAssignmentService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping
    public ApiResponse<List<TeachingAssignment>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String courseId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingAssignmentService.listByCourse(courseId, userContext));
    }

    @PostMapping
    public ApiResponse<TeachingAssignment> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String courseId,
            @Valid @RequestBody AssignmentCreateRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingAssignmentService.create(courseId, userContext, request));
    }

    @PostMapping("/{assignmentId}/status")
    public ApiResponse<TeachingAssignment> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String assignmentId,
            @Valid @RequestBody TeachingAssignmentStatusUpdateRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingAssignmentService.updateStatus(assignmentId, userContext, request.status()));
    }
}
