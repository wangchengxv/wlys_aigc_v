package com.example.aigc.controller;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.StyleTemplateCreateRequest;
import com.example.aigc.dto.StyleTemplateUpdateRequest;
import com.example.aigc.entity.StyleTemplate;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import com.example.aigc.service.StyleTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/style-templates")
public class StyleTemplateController {
    private final StyleTemplateService styleTemplateService;
    private final RequestAuthService requestAuthService;

    public StyleTemplateController(
            StyleTemplateService styleTemplateService,
            RequestAuthService requestAuthService
    ) {
        this.styleTemplateService = styleTemplateService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping
    public ApiResponse<List<StyleTemplate>> list(
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
        return ApiResponse.ok(styleTemplateService.listVisible(userContext));
    }

    @PostMapping
    public ApiResponse<StyleTemplate> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody StyleTemplateCreateRequest createRequest
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(styleTemplateService.create(userContext, createRequest));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<StyleTemplate> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String templateId,
            @Valid @RequestBody StyleTemplateUpdateRequest updateRequest
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        return ApiResponse.ok(styleTemplateService.update(userContext, templateId, updateRequest));
    }
}
