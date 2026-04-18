package com.example.aigc.controller;

import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.OperationsDashboardResponse;
import com.example.aigc.service.AuthorizationService;
import com.example.aigc.service.OperationsDashboardService;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations/dashboard")
public class OperationsDashboardController {
    private final OperationsDashboardService operationsDashboardService;
    private final RequestAuthService requestAuthService;
    private final AuthorizationService authorizationService;

    public OperationsDashboardController(
            OperationsDashboardService operationsDashboardService,
            RequestAuthService requestAuthService,
            AuthorizationService authorizationService
    ) {
        this.operationsDashboardService = operationsDashboardService;
        this.requestAuthService = requestAuthService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ApiResponse<OperationsDashboardResponse> dashboard(
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
        authorizationService.requireTeachingManager(userContext, "只有教师或管理员可以查看统计看板");
        return ApiResponse.ok(operationsDashboardService.getDashboard());
    }
}
