package com.example.aigc.controller;

import com.example.aigc.dto.AdminUserBatchLockUpdateRequest;
import com.example.aigc.dto.AdminUserBatchOperationResponse;
import com.example.aigc.dto.AdminUserBatchRoleUpdateRequest;
import com.example.aigc.dto.AdminUserBatchStatsResponse;
import com.example.aigc.dto.AdminUserBatchStatusUpdateRequest;
import com.example.aigc.dto.AdminUserCreateRequest;
import com.example.aigc.dto.AdminUserImportResultResponse;
import com.example.aigc.dto.AdminUserImportTaskResponse;
import com.example.aigc.dto.AdminUserLockUpdateRequest;
import com.example.aigc.dto.AdminUserPasswordResetRequest;
import com.example.aigc.dto.AdminUserProfileUpdateRequest;
import com.example.aigc.dto.AdminUserResponse;
import com.example.aigc.dto.AdminUserRoleUpdateRequest;
import com.example.aigc.dto.AdminUserStatusUpdateRequest;
import com.example.aigc.dto.AdminUserUpdateRequest;
import com.example.aigc.dto.ApiResponse;
import com.example.aigc.dto.OrgUnitCreateRequest;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.entity.OrgUnit;
import com.example.aigc.service.AdminDirectoryService;
import com.example.aigc.service.RequestAuthService;
import com.example.aigc.service.RequestUserContext;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/directory")
public class AdminDirectoryController {
    private final AdminDirectoryService adminDirectoryService;
    private final RequestAuthService requestAuthService;

    public AdminDirectoryController(
            AdminDirectoryService adminDirectoryService,
            RequestAuthService requestAuthService
    ) {
        this.adminDirectoryService = adminDirectoryService;
        this.requestAuthService = requestAuthService;
    }

    @GetMapping("/org-units")
    public ApiResponse<List<OrgUnit>> listOrgUnits(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId
    ) {
        return ApiResponse.ok(adminDirectoryService.listOrgUnits(resolveUserContext(
                authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId
        )));
    }

    @PostMapping("/org-units")
    public ApiResponse<OrgUnit> createOrgUnit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody OrgUnitCreateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.createOrgUnit(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                request
        ));
    }

    @GetMapping("/users")
    public ApiResponse<PagedResult<AdminUserResponse>> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String classroomId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        return ApiResponse.ok(adminDirectoryService.listUsers(resolveUserContext(
                authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId
        ), page, pageSize, keyword, role, enabled, locked, orgUnitId, classroomId, sortBy, sortOrder));
    }

    @PostMapping("/users")
    public ApiResponse<AdminUserResponse> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.createUser(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                request
        ));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.updateUser(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PutMapping("/users/{userId}/profile")
    public ApiResponse<AdminUserResponse> updateUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserProfileUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.updateUserProfile(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.updateUserStatus(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PutMapping("/users/{userId}/password")
    public ApiResponse<AdminUserResponse> resetUserPassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserPasswordResetRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.resetUserPassword(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PutMapping("/users/{userId}/role")
    public ApiResponse<AdminUserResponse> updateUserRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.updateUserRole(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PutMapping("/users/{userId}/lock")
    public ApiResponse<AdminUserResponse> updateUserLock(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId,
            @Valid @RequestBody AdminUserLockUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.updateUserLock(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId,
                request
        ));
    }

    @PostMapping("/users/{userId}/force-logout")
    public ApiResponse<AdminUserResponse> forceLogout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String userId
    ) {
        return ApiResponse.ok(adminDirectoryService.forceLogout(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                userId
        ));
    }

    @PostMapping("/users/batch/status")
    public ApiResponse<AdminUserBatchOperationResponse> batchUpdateStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody AdminUserBatchStatusUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.batchUpdateStatus(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                request.userIds(),
                request.enabled()
        ));
    }

    @PostMapping("/users/batch/lock")
    public ApiResponse<AdminUserBatchOperationResponse> batchUpdateLock(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody AdminUserBatchLockUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.batchUpdateLock(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                request.userIds(),
                request.locked(),
                request.reason()
        ));
    }

    @PostMapping("/users/batch/role")
    public ApiResponse<AdminUserBatchOperationResponse> batchUpdateRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @Valid @RequestBody AdminUserBatchRoleUpdateRequest request
    ) {
        return ApiResponse.ok(adminDirectoryService.batchUpdateRole(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                request.userIds(),
                request.role()
        ));
    }

    @GetMapping("/users/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId
    ) {
        byte[] content = adminDirectoryService.buildImportTemplate(resolveUserContext(
                authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("account-import-template.csv").build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminUserImportResultResponse> importUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(adminDirectoryService.importUsers(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                file
        ));
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) String orgUnitId,
            @RequestParam(required = false) String classroomId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        byte[] content = adminDirectoryService.exportUsers(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                keyword, role, enabled, locked, orgUnitId, classroomId, sortBy, sortOrder
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("accounts-export.csv").build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/users/import/tasks")
    public ApiResponse<PagedResult<AdminUserImportTaskResponse>> listImportTasks(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminDirectoryService.listImportTasks(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                page,
                pageSize
        ));
    }

    @GetMapping("/users/import/tasks/{taskId}")
    public ApiResponse<AdminUserImportTaskResponse> getImportTask(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @PathVariable String taskId
    ) {
        return ApiResponse.ok(adminDirectoryService.getImportTask(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                taskId
        ));
    }

    @GetMapping("/users/batch/stats")
    public ApiResponse<AdminUserBatchStatsResponse> listBatchStats(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "x-aigc-token", required = false) String xAigcToken,
            @RequestHeader(value = "x-user-id", required = false) String xUserId,
            @RequestHeader(value = "x-user-name", required = false) String xUserName,
            @RequestHeader(value = "x-org-unit-id", required = false) String xOrgUnitId,
            @RequestHeader(value = "x-course-id", required = false) String xCourseId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(adminDirectoryService.listBatchStats(
                resolveUserContext(authorization, xAigcToken, xUserId, xUserName, xOrgUnitId, xCourseId),
                limit
        ));
    }

    private RequestUserContext resolveUserContext(
            String authorization,
            String xAigcToken,
            String xUserId,
            String xUserName,
            String xOrgUnitId,
            String xCourseId
    ) {
        return requestAuthService.requireUserContext(
                authorization,
                xAigcToken,
                xUserId,
                xUserName,
                xOrgUnitId,
                xCourseId
        );
    }
}
