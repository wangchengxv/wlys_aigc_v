package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.AdminUserBatchStatsItem;
import com.example.aigc.dto.AdminUserBatchStatsResponse;
import com.example.aigc.dto.AdminUserBatchOperationResponse;
import com.example.aigc.dto.AdminUserCreateRequest;
import com.example.aigc.dto.AdminUserImportErrorItem;
import com.example.aigc.dto.AdminUserImportResultResponse;
import com.example.aigc.dto.AdminUserImportTaskResponse;
import com.example.aigc.dto.AdminUserLockUpdateRequest;
import com.example.aigc.dto.AdminUserPasswordResetRequest;
import com.example.aigc.dto.AdminUserProfileUpdateRequest;
import com.example.aigc.dto.AdminUserResponse;
import com.example.aigc.dto.AdminUserRoleUpdateRequest;
import com.example.aigc.dto.AdminUserStatusUpdateRequest;
import com.example.aigc.dto.AdminUserUpdateRequest;
import com.example.aigc.dto.OrgUnitCreateRequest;
import com.example.aigc.dto.PagedResult;
import com.example.aigc.entity.AccountImportTask;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.AuditLogRecord;
import com.example.aigc.entity.OrgUnit;
import com.example.aigc.enums.UserRole;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AccountImportTaskRepository;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.OrgUnitRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class AdminDirectoryService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final AppUserRepository appUserRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PasswordCodec passwordCodec;
    private final AuthorizationService authorizationService;
    private final AuditLogService auditLogService;
    private final AccountImportTaskRepository accountImportTaskRepository;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public AdminDirectoryService(
            AppUserRepository appUserRepository,
            OrgUnitRepository orgUnitRepository,
            PasswordCodec passwordCodec,
            AuthorizationService authorizationService,
            AuditLogService auditLogService,
            AccountImportTaskRepository accountImportTaskRepository,
            ObjectMapper objectMapper,
            AuthProperties authProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.passwordCodec = passwordCodec;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
        this.accountImportTaskRepository = accountImportTaskRepository;
        this.objectMapper = objectMapper;
        this.authProperties = authProperties;
    }

    public List<OrgUnit> listOrgUnits(RequestUserContext actor) {
        requirePermission(actor, AccountPermissionPoints.API_ORG_UNIT_LIST, "只有具备组织目录查看权限的用户可以访问");
        return orgUnitRepository.findAll().stream()
                .sorted(Comparator.comparing((OrgUnit unit) -> unit.type.name()).thenComparing(unit -> unit.name))
                .toList();
    }

    public OrgUnit createOrgUnit(RequestUserContext actor, OrgUnitCreateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_ORG_UNIT_CREATE, "只有具备组织目录创建权限的用户可以操作");
        String parentUnitId = blankToNull(request.parentUnitId());
        if (request.type().name().equals("CLASSROOM") && parentUnitId == null) {
            throw new BizException(400, "班级必须选择上级组织");
        }
        if (parentUnitId != null) {
            orgUnitRepository.findById(parentUnitId)
                    .orElseThrow(() -> new BizException(400, "上级组织不存在"));
        }
        OrgUnit unit = new OrgUnit();
        unit.unitId = generateId(request.type().name().toLowerCase());
        unit.name = request.name().trim();
        unit.code = blankToNull(request.code());
        unit.type = request.type();
        unit.parentUnitId = parentUnitId;
        unit.createdAt = Instant.now();
        unit.updatedAt = unit.createdAt;
        OrgUnit saved = orgUnitRepository.save(unit);
        auditLogService.record(actor, "ORG_UNIT_CREATED", "ORG_UNIT", saved.unitId, java.util.Map.of(
                "name", saved.name,
                "type", saved.type.name()
        ));
        return saved;
    }

    public PagedResult<AdminUserResponse> listUsers(
            RequestUserContext actor,
            int page,
            int pageSize,
            String keyword,
            String role,
            Boolean enabled,
            Boolean locked,
            String orgUnitId,
            String classroomId,
            String sortBy,
            String sortOrder
    ) {
        requirePermission(actor, AccountPermissionPoints.API_USER_LIST, "只有具备账号目录查看权限的用户可以访问");
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String normalizedKeyword = blankToNull(keyword);
        String normalizedOrgUnitId = blankToNull(orgUnitId);
        String normalizedClassroomId = blankToNull(classroomId);
        UserRole roleFilter = parseRole(role);

        List<AppUser> filtered = appUserRepository.findAll().stream()
                .filter(user -> matchesKeyword(user, normalizedKeyword))
                .filter(user -> roleFilter == null || roleFilter == user.role)
                .filter(user -> enabled == null || enabled.equals(user.enabled))
                .filter(user -> locked == null || locked.equals(user.locked))
                .filter(user -> normalizedOrgUnitId == null || normalizedOrgUnitId.equals(user.orgUnitId))
                .filter(user -> normalizedClassroomId == null || normalizedClassroomId.equals(user.classroomId))
                .sorted(resolveComparator(sortBy, sortOrder))
                .toList();

        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, filtered.size());
        List<AdminUserResponse> list = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toAdminUserResponse)
                .toList();
        return new PagedResult<>(list, filtered.size());
    }

    public AdminUserResponse createUser(RequestUserContext actor, AdminUserCreateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_CREATE, "只有具备账号创建权限的用户可以操作");
        String username = request.username().trim();
        appUserRepository.findByUsername(username).ifPresent(existing -> {
            throw new BizException(400, "用户名已存在");
        });
        validateUnitAssignment(request.orgUnitId(), request.classroomId());
        AppUser user = new AppUser();
        user.userId = generateId("user");
        user.username = username;
        user.passwordHash = passwordCodec.encode(request.password().trim());
        user.displayName = request.displayName().trim();
        user.role = request.role();
        user.orgUnitId = blankToNull(request.orgUnitId());
        user.classroomId = blankToNull(request.classroomId());
        user.enabled = request.enabled() == null || request.enabled();
        user.locked = false;
        user.lockReason = null;
        user.lockedAt = null;
        user.failedLoginCount = 0;
        user.lastLoginAt = null;
        user.lastLoginIp = null;
        user.forcePasswordChange = false;
        user.sessionVersion = 0;
        user.createdAt = Instant.now();
        user.passwordUpdatedAt = user.createdAt;
        user.updatedAt = user.createdAt;
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_CREATED", "APP_USER", saved.userId, java.util.Map.of(
                "username", saved.username,
                "role", saved.role.name()
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse updateUser(RequestUserContext actor, String userId, AdminUserUpdateRequest request) {
        AdminUserResponse response = updateUserProfile(actor, userId, new AdminUserProfileUpdateRequest(
                request.displayName(),
                request.orgUnitId(),
                request.classroomId()
        ));
        if (request.role() != null) {
            response = updateUserRole(actor, userId, new AdminUserRoleUpdateRequest(request.role()));
        }
        if (request.enabled() != null) {
            response = updateUserStatus(actor, userId, new AdminUserStatusUpdateRequest(request.enabled()));
        }
        if (request.password() != null && !request.password().isBlank()) {
            response = resetUserPassword(actor, userId, new AdminUserPasswordResetRequest(request.password(), null));
        }
        return response;
    }

    public AdminUserResponse updateUserProfile(RequestUserContext actor, String userId, AdminUserProfileUpdateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_PROFILE_UPDATE, "只有具备账号资料更新权限的用户可以操作");
        AppUser user = requireUser(userId);
        String targetDisplayName = request.displayName() == null || request.displayName().isBlank()
                ? user.displayName
                : request.displayName().trim();
        String targetOrgUnitId = request.orgUnitId() == null ? user.orgUnitId : blankToNull(request.orgUnitId());
        String targetClassroomId = request.classroomId() == null ? user.classroomId : blankToNull(request.classroomId());
        validateUnitAssignment(targetOrgUnitId, targetClassroomId);
        user.displayName = targetDisplayName;
        user.orgUnitId = targetOrgUnitId;
        user.classroomId = targetClassroomId;
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_PROFILE_UPDATED", "APP_USER", saved.userId, Map.of(
                "displayName", safeValue(saved.displayName)
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse updateUserStatus(RequestUserContext actor, String userId, AdminUserStatusUpdateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_STATUS_UPDATE, "只有具备账号状态变更权限的用户可以操作");
        AppUser user = requireUser(userId);
        user.enabled = request.enabled();
        if (!user.enabled) {
            user.sessionVersion = user.sessionVersion + 1;
        }
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_STATUS_UPDATED", "APP_USER", saved.userId, Map.of(
                "enabled", saved.enabled
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse resetUserPassword(RequestUserContext actor, String userId, AdminUserPasswordResetRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_PASSWORD_RESET, "只有具备账号密码重置权限的用户可以操作");
        AppUser user = requireUser(userId);
        user.passwordHash = passwordCodec.encode(request.password().trim());
        user.passwordUpdatedAt = Instant.now();
        user.failedLoginCount = 0;
        user.locked = false;
        user.lockReason = null;
        user.lockedAt = null;
        user.sessionVersion = user.sessionVersion + 1;
        if (request.forcePasswordChange() != null) {
            user.forcePasswordChange = request.forcePasswordChange();
        } else {
            user.forcePasswordChange = authProperties.isForcePasswordChangeOnReset();
        }
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_PASSWORD_RESET", "APP_USER", saved.userId, Map.of(
                "forcePasswordChange", saved.forcePasswordChange
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse updateUserRole(RequestUserContext actor, String userId, AdminUserRoleUpdateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_ROLE_UPDATE, "只有具备账号角色调整权限的用户可以操作");
        AppUser user = requireUser(userId);
        user.role = request.role();
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_ROLE_UPDATED", "APP_USER", saved.userId, Map.of(
                "role", saved.role.name()
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse updateUserLock(RequestUserContext actor, String userId, AdminUserLockUpdateRequest request) {
        requirePermission(actor, AccountPermissionPoints.API_USER_LOCK_UPDATE, "只有具备账号锁定权限的用户可以操作");
        AppUser user = requireUser(userId);
        applyLock(user, request.locked(), request.reason());
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, request.locked() ? "USER_LOCKED" : "USER_UNLOCKED", "APP_USER", saved.userId, Map.of(
                "locked", saved.locked,
                "reason", safeValue(saved.lockReason)
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserResponse forceLogout(RequestUserContext actor, String userId) {
        requirePermission(actor, AccountPermissionPoints.API_USER_FORCE_LOGOUT, "只有具备强制下线权限的用户可以操作");
        AppUser user = requireUser(userId);
        user.sessionVersion = user.sessionVersion + 1;
        user.updatedAt = Instant.now();
        AppUser saved = appUserRepository.save(user);
        auditLogService.record(actor, "USER_FORCE_LOGOUT", "APP_USER", saved.userId, Map.of(
                "sessionVersion", saved.sessionVersion
        ));
        return toAdminUserResponse(saved);
    }

    public AdminUserBatchOperationResponse batchUpdateStatus(RequestUserContext actor, List<String> userIds, boolean enabled) {
        requirePermission(actor, AccountPermissionPoints.API_USER_BATCH_STATUS, "只有具备批量状态变更权限的用户可以操作");
        AdminUserBatchOperationResponse result = batchOperateUsers(userIds, user -> {
            user.enabled = enabled;
            if (!enabled) {
                user.sessionVersion = user.sessionVersion + 1;
            }
            user.updatedAt = Instant.now();
            appUserRepository.save(user);
            auditLogService.record(actor, "USER_STATUS_BATCH_UPDATED", "APP_USER", user.userId, Map.of("enabled", enabled));
        });
        recordBatchSummary(actor, "USER_BATCH_STATUS_SUMMARY", result);
        return result;
    }

    public AdminUserBatchOperationResponse batchUpdateLock(RequestUserContext actor, List<String> userIds, boolean locked, String reason) {
        requirePermission(actor, AccountPermissionPoints.API_USER_BATCH_LOCK, "只有具备批量锁定权限的用户可以操作");
        AdminUserBatchOperationResponse result = batchOperateUsers(userIds, user -> {
            applyLock(user, locked, reason);
            user.updatedAt = Instant.now();
            appUserRepository.save(user);
            auditLogService.record(actor, locked ? "USER_BATCH_LOCKED" : "USER_BATCH_UNLOCKED", "APP_USER", user.userId, Map.of(
                    "locked", locked,
                    "reason", safeValue(user.lockReason)
            ));
        });
        recordBatchSummary(actor, locked ? "USER_BATCH_LOCK_SUMMARY" : "USER_BATCH_UNLOCK_SUMMARY", result);
        return result;
    }

    public AdminUserBatchOperationResponse batchUpdateRole(RequestUserContext actor, List<String> userIds, UserRole role) {
        requirePermission(actor, AccountPermissionPoints.API_USER_BATCH_ROLE, "只有具备批量角色调整权限的用户可以操作");
        AdminUserBatchOperationResponse result = batchOperateUsers(userIds, user -> {
            user.role = role;
            user.updatedAt = Instant.now();
            appUserRepository.save(user);
            auditLogService.record(actor, "USER_BATCH_ROLE_UPDATED", "APP_USER", user.userId, Map.of("role", role.name()));
        });
        recordBatchSummary(actor, "USER_BATCH_ROLE_SUMMARY", result);
        return result;
    }

    public byte[] buildImportTemplate(RequestUserContext actor) {
        requirePermission(actor, AccountPermissionPoints.API_USER_IMPORT_TEMPLATE, "只有具备账号导入模板下载权限的用户可以操作");
        StringBuilder csv = new StringBuilder();
        csv.append("username,displayName,role,password,orgUnitId,classroomId,enabled\n");
        csv.append("student001,张三,STUDENT,Pass@123,org-001,class-001,true\n");
        csv.append("teacher001,李老师,TEACHER,Pass@123,org-001,,true\n");
        csv.append("admin001,系统管理员,ADMIN,Pass@123,,,true\n");
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public AdminUserImportResultResponse importUsers(RequestUserContext actor, MultipartFile file) {
        requirePermission(actor, AccountPermissionPoints.API_USER_IMPORT, "只有具备账号导入权限的用户可以操作");
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "导入文件不能为空");
        }
        List<String> lines = readCsvLines(file);
        if (lines.isEmpty()) {
            throw new BizException(400, "导入文件内容为空");
        }
        int startIndex = detectHeaderOffset(lines);
        List<AdminUserImportErrorItem> errors = new ArrayList<>();
        int success = 0;
        int total = 0;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            total++;
            int rowNumber = i + 1;
            try {
                ImportRow row = parseRow(line, rowNumber);
                createUser(actor, new AdminUserCreateRequest(
                        row.username,
                        row.password,
                        row.displayName,
                        row.role,
                        row.orgUnitId,
                        row.classroomId,
                        row.enabled
                ));
                success++;
            } catch (BizException ex) {
                errors.add(new AdminUserImportErrorItem(rowNumber, tryExtractUsername(line), ex.getMessage()));
            } catch (Exception ex) {
                errors.add(new AdminUserImportErrorItem(rowNumber, tryExtractUsername(line), "导入失败，数据格式错误"));
            }
        }
        int failed = total - success;
        String taskId = generateId("acct-import");
        AccountImportTask task = new AccountImportTask();
        task.taskId = taskId;
        task.status = failed > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";
        task.sourceFileName = file.getOriginalFilename();
        task.operatorUserId = actor == null ? null : actor.userId();
        task.operatorUserName = actor == null ? null : actor.userName();
        task.totalRows = total;
        task.successRows = success;
        task.failedRows = failed;
        task.errorDetailsJson = writeImportErrors(errors);
        task.createdAt = Instant.now();
        task.finishedAt = task.createdAt;
        accountImportTaskRepository.save(task);
        auditLogService.record(actor, "ACCOUNT_IMPORT_FINISHED", "ACCOUNT_IMPORT_TASK", task.taskId, Map.of(
                "totalRows", total,
                "successRows", success,
                "failedRows", failed
        ));
        return new AdminUserImportResultResponse(taskId, total, success, failed, errors);
    }

    public byte[] exportUsers(
            RequestUserContext actor,
            String keyword,
            String role,
            Boolean enabled,
            Boolean locked,
            String orgUnitId,
            String classroomId,
            String sortBy,
            String sortOrder
    ) {
        requirePermission(actor, AccountPermissionPoints.API_USER_EXPORT, "只有具备账号导出权限的用户可以操作");
        PagedResult<AdminUserResponse> data = listUsers(actor, 1, MAX_PAGE_SIZE, keyword, role, enabled, locked, orgUnitId, classroomId, sortBy, sortOrder);
        StringBuilder csv = new StringBuilder();
        csv.append("userId,username,displayName,role,orgUnitId,classroomId,enabled,locked,lastLoginAt,lastLoginIp,createdAt\n");
        for (AdminUserResponse item : data.list()) {
            csv.append(escapeCsv(item.userId())).append(',')
                    .append(escapeCsv(maskUsername(item.username()))).append(',')
                    .append(escapeCsv(maskDisplayName(item.displayName()))).append(',')
                    .append(escapeCsv(item.role() == null ? null : item.role().name())).append(',')
                    .append(escapeCsv(item.orgUnitId())).append(',')
                    .append(escapeCsv(item.classroomId())).append(',')
                    .append(item.enabled()).append(',')
                    .append(item.locked()).append(',')
                    .append(escapeCsv(item.lastLoginAt())).append(',')
                    .append(escapeCsv(maskIp(item.lastLoginIp()))).append(',')
                    .append(escapeCsv(item.createdAt()))
                    .append('\n');
        }
        auditLogService.record(actor, "ACCOUNT_EXPORT_FINISHED", "APP_USER", null, Map.of(
                "totalRows", data.list().size()
        ));
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public PagedResult<AdminUserImportTaskResponse> listImportTasks(RequestUserContext actor, int page, int pageSize) {
        requirePermission(actor, AccountPermissionPoints.API_USER_IMPORT_TASK_QUERY, "只有具备导入任务查询权限的用户可以访问");
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        List<AccountImportTask> tasks = accountImportTaskRepository.findRecent(100);
        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, tasks.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, tasks.size());
        List<AdminUserImportTaskResponse> items = tasks.subList(fromIndex, toIndex).stream()
                .map(this::toImportTaskResponse)
                .toList();
        return new PagedResult<>(items, tasks.size());
    }

    public AdminUserImportTaskResponse getImportTask(RequestUserContext actor, String taskId) {
        requirePermission(actor, AccountPermissionPoints.API_USER_IMPORT_TASK_QUERY, "只有具备导入任务查询权限的用户可以访问");
        AccountImportTask task = accountImportTaskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(404, "导入任务不存在"));
        return toImportTaskResponse(task);
    }

    public AdminUserBatchStatsResponse listBatchStats(RequestUserContext actor, int limit) {
        requirePermission(actor, AccountPermissionPoints.API_USER_BATCH_STATS, "只有具备批量任务统计查询权限的用户可以访问");
        int normalizedLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<AuditLogRecord> logs = auditLogService.listRecent(null, null, null).stream()
                .filter(this::isBatchAction)
                .limit(normalizedLimit)
                .toList();
        List<AdminUserBatchStatsItem> items = new ArrayList<>();
        int total = 0;
        int success = 0;
        int failed = 0;
        for (AuditLogRecord log : logs) {
            Map<String, Object> details = parseAuditDetails(log.detailsJson);
            int itemTotal = asInt(details.get("total"));
            int itemSuccess = asInt(details.get("success"));
            int itemFailed = asInt(details.get("failed"));
            List<String> failedUserIds = asStringList(details.get("failedUserIds"));
            if (itemTotal <= 0 && itemSuccess == 0 && itemFailed == 0) {
                itemSuccess = 1;
                itemTotal = 1;
            }
            total += itemTotal;
            success += itemSuccess;
            failed += itemFailed;
            items.add(new AdminUserBatchStatsItem(
                    log.action,
                    log.actorUserId,
                    log.actorUserName,
                    log.createdAt == null ? null : log.createdAt.toString(),
                    itemTotal,
                    itemSuccess,
                    itemFailed,
                    failedUserIds
            ));
        }
        return new AdminUserBatchStatsResponse(items, total, success, failed);
    }

    private void validateUnitAssignment(String orgUnitId, String classroomId) {
        String normalizedOrgUnitId = blankToNull(orgUnitId);
        String normalizedClassroomId = blankToNull(classroomId);
        if (normalizedOrgUnitId != null) {
            OrgUnit unit = orgUnitRepository.findById(normalizedOrgUnitId)
                    .orElseThrow(() -> new BizException(400, "所属组织不存在"));
            if (!"ORGANIZATION".equals(unit.type.name())) {
                throw new BizException(400, "所属组织必须选择组织类型");
            }
        }
        if (normalizedClassroomId != null) {
            OrgUnit unit = orgUnitRepository.findById(normalizedClassroomId)
                    .orElseThrow(() -> new BizException(400, "所属班级不存在"));
            if (!"CLASSROOM".equals(unit.type.name())) {
                throw new BizException(400, "所属班级必须选择班级类型");
            }
            if (normalizedOrgUnitId != null && unit.parentUnitId != null && !normalizedOrgUnitId.equals(unit.parentUnitId)) {
                throw new BizException(400, "班级与所属组织不匹配");
            }
        }
    }

    private AdminUserResponse toAdminUserResponse(AppUser user) {
        return new AdminUserResponse(
                user.userId,
                user.username,
                user.displayName,
                user.role,
                user.orgUnitId,
                user.classroomId,
                user.enabled,
                user.locked,
                user.lockReason,
                user.lockedAt == null ? null : user.lockedAt.toString(),
                user.failedLoginCount,
                user.lastLoginAt == null ? null : user.lastLoginAt.toString(),
                user.lastLoginIp,
                user.passwordUpdatedAt == null ? null : user.passwordUpdatedAt.toString(),
                user.forcePasswordChange,
                user.createdAt == null ? null : user.createdAt.toString(),
                user.updatedAt == null ? null : user.updatedAt.toString()
        );
    }

    private String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AppUser requireUser(String userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
    }

    private UserRole parseRole(String role) {
        String value = blankToNull(role);
        if (value == null) {
            return null;
        }
        try {
            return UserRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BizException(400, "用户角色参数非法");
        }
    }

    private boolean matchesKeyword(AppUser user, String keyword) {
        if (keyword == null) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(user.userId, needle)
                || containsIgnoreCase(user.username, needle)
                || containsIgnoreCase(user.displayName, needle);
    }

    private boolean containsIgnoreCase(String source, String needle) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(needle);
    }

    private Comparator<AppUser> resolveComparator(String sortBy, String sortOrder) {
        String by = blankToNull(sortBy);
        String order = blankToNull(sortOrder);
        boolean asc = "asc".equalsIgnoreCase(order);
        String field = by == null ? "createdAt" : by;
        Comparator<AppUser> comparator;
        switch (field) {
            case "createdAt" -> comparator = comparingNullable(user -> user.createdAt);
            case "updatedAt" -> comparator = comparingNullable(user -> user.updatedAt);
            case "lastLoginAt" -> comparator = comparingNullable(user -> user.lastLoginAt);
            case "username" -> comparator = comparingNullable(user -> user.username);
            case "displayName" -> comparator = comparingNullable(user -> user.displayName);
            case "failedLoginCount" -> comparator = Comparator.comparingInt(user -> user.failedLoginCount);
            default -> throw new BizException(400, "排序字段不支持");
        }
        Comparator<AppUser> withTieBreaker = comparator.thenComparing(user -> user.username, Comparator.nullsLast(String::compareTo));
        return asc ? withTieBreaker : withTieBreaker.reversed();
    }

    private <T extends Comparable<? super T>> Comparator<AppUser> comparingNullable(Function<AppUser, T> getter) {
        return Comparator.comparing(getter, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private void applyLock(AppUser user, boolean locked, String reason) {
        user.locked = locked;
        if (locked) {
            user.lockReason = blankToNull(reason);
            user.lockedAt = Instant.now();
            user.sessionVersion = user.sessionVersion + 1;
        } else {
            user.lockReason = null;
            user.lockedAt = null;
            user.failedLoginCount = 0;
        }
    }

    private AdminUserBatchOperationResponse batchOperateUsers(List<String> userIds, java.util.function.Consumer<AppUser> operation) {
        List<String> normalizedUserIds = normalizeUserIds(userIds);
        List<AppUser> users = appUserRepository.findAllByIds(normalizedUserIds);
        Set<String> hitIds = new HashSet<>();
        for (AppUser user : users) {
            hitIds.add(user.userId);
            operation.accept(user);
        }
        List<String> failedUserIds = new ArrayList<>();
        for (String userId : normalizedUserIds) {
            if (!hitIds.contains(userId)) {
                failedUserIds.add(userId);
            }
        }
        return new AdminUserBatchOperationResponse(
                normalizedUserIds.size(),
                users.size(),
                failedUserIds.size(),
                failedUserIds
        );
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BizException(400, "用户ID列表不能为空");
        }
        List<String> normalized = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (String userId : userIds) {
            String id = blankToNull(userId);
            if (id == null || !unique.add(id)) {
                continue;
            }
            normalized.add(id);
        }
        if (normalized.isEmpty()) {
            throw new BizException(400, "用户ID列表不能为空");
        }
        return normalized;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private void recordBatchSummary(RequestUserContext actor, String action, AdminUserBatchOperationResponse result) {
        auditLogService.record(actor, action, "APP_USER_BATCH", null, Map.of(
                "total", result.total(),
                "success", result.success(),
                "failed", result.failed(),
                "failedUserIds", result.failedUserIds()
        ));
    }

    private void requirePermission(RequestUserContext actor, String permission, String message) {
        if (!authorizationService.hasPermission(actor, permission)) {
            auditLogService.record(actor, "ACCOUNT_PERMISSION_DENIED", "PERMISSION_POINT", permission, Map.of(
                    "permission", permission,
                    "message", message
            ));
            throw new BizException(403, message);
        }
    }

    private List<String> readCsvLines(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            return content.lines().toList();
        } catch (IOException ex) {
            throw new BizException(400, "读取导入文件失败");
        }
    }

    private int detectHeaderOffset(List<String> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        String first = lines.get(0).trim().toLowerCase(Locale.ROOT);
        if (first.startsWith("username,")) {
            return 1;
        }
        return 0;
    }

    private ImportRow parseRow(String line, int rowNumber) {
        String[] values = line.split(",", -1);
        if (values.length < 7) {
            throw new BizException(400, "第" + rowNumber + "行字段不足");
        }
        String username = normalizeCell(values[0]);
        String displayName = normalizeCell(values[1]);
        String roleValue = normalizeCell(values[2]);
        String password = normalizeCell(values[3]);
        String orgUnitId = normalizeCell(values[4]);
        String classroomId = normalizeCell(values[5]);
        String enabledValue = normalizeCell(values[6]);
        if (username == null || password == null || roleValue == null) {
            throw new BizException(400, "第" + rowNumber + "行缺少必填字段");
        }
        UserRole role;
        try {
            role = UserRole.valueOf(roleValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BizException(400, "第" + rowNumber + "行角色非法");
        }
        boolean enabled = enabledValue == null || Boolean.parseBoolean(enabledValue);
        return new ImportRow(username, password, displayName == null ? username : displayName, role, orgUnitId, classroomId, enabled);
    }

    private String normalizeCell(String cell) {
        if (cell == null) {
            return null;
        }
        String value = cell.trim();
        return value.isEmpty() ? null : value;
    }

    private String tryExtractUsername(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String[] values = line.split(",", -1);
        if (values.length == 0) {
            return "";
        }
        String username = normalizeCell(values[0]);
        return username == null ? "" : username;
    }

    private String writeImportErrors(List<AdminUserImportErrorItem> errors) {
        if (errors == null || errors.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private AdminUserImportTaskResponse toImportTaskResponse(AccountImportTask task) {
        return new AdminUserImportTaskResponse(
                task.taskId,
                task.status,
                task.sourceFileName,
                task.operatorUserId,
                task.operatorUserName,
                task.totalRows,
                task.successRows,
                task.failedRows,
                task.createdAt == null ? null : task.createdAt.toString(),
                task.finishedAt == null ? null : task.finishedAt.toString(),
                parseImportErrors(task.errorDetailsJson)
        );
    }

    private List<AdminUserImportErrorItem> parseImportErrors(String errorDetailsJson) {
        if (errorDetailsJson == null || errorDetailsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(errorDetailsJson, new TypeReference<List<AdminUserImportErrorItem>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean isBatchAction(AuditLogRecord log) {
        if (log == null || log.action == null) {
            return false;
        }
        return log.action.startsWith("USER_")
                && log.action.contains("BATCH")
                && (isEntityType(log, "APP_USER") || isEntityType(log, "APP_USER_BATCH"));
    }

    private boolean isEntityType(AuditLogRecord log, String entityType) {
        return entityType.equals(log.entityType);
    }

    private Map<String, Object> parseAuditDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(item -> item != null).map(Object::toString).toList();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    private String maskDisplayName(String displayName) {
        if (displayName == null || displayName.length() <= 1) {
            return displayName;
        }
        return displayName.charAt(0) + "**";
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }
        String[] segments = ip.split("\\.");
        if (segments.length != 4) {
            return "***";
        }
        return segments[0] + "." + segments[1] + ".*.*";
    }

    private record ImportRow(
            String username,
            String password,
            String displayName,
            UserRole role,
            String orgUnitId,
            String classroomId,
            boolean enabled
    ) {
    }
}
