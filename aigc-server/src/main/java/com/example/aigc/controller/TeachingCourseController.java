package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.CourseCreateRequest;
import com.example.aigc.dto.TeachingCourseArchiveRequest;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.TeachingCourseService;
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
@RequestMapping("/api/v1/courses")
public class TeachingCourseController {
    private final TeachingCourseService teachingCourseService;
    private final RequestAuthService requestAuthService;

    public TeachingCourseController(
            TeachingCourseService teachingCourseService,
            RequestAuthService requestAuthService
    ) {
        this.teachingCourseService = teachingCourseService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping
    public ApiResponse<List<TeachingCourse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingCourseService.listVisible(userContext));
    }

    @PostMapping
    public ApiResponse<TeachingCourse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(teachingCourseService.create(userContext, request));
    }

    @PostMapping("/{courseId}/archive")
    public ApiResponse<TeachingCourse> archive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String courseId,
            @RequestBody(required = false) TeachingCourseArchiveRequest request
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        boolean archived = request == null || request.archived() == null || request.archived();
        return ApiResponse.ok(teachingCourseService.updateArchiveStatus(courseId, userContext, archived));
    }
}
