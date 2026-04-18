package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.service.AuditLogService;
import com.example.aigc.service.AuthorizationService;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final RequestAuthService requestAuthService;
    private final AuthorizationService authorizationService;

    public AuditLogController(
            AuditLogService auditLogService,
            RequestAuthService requestAuthService,
            AuthorizationService authorizationService
    ) {
        this.auditLogService = auditLogService;
        this.requestAuthService = requestAuthService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogRecord>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actorUserId
    ) {
        RequestUserContext userContext = requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
        authorizationService.requireTeachingManager(userContext, "只有教师或管理员可以查看审计日志");
        return ApiResponse.ok(auditLogService.listRecent(entityType, entityId, actorUserId));
    }
}
