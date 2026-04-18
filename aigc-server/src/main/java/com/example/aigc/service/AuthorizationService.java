package com.example.aigc.service;

import com.example.aigc.entity.AssignmentSubmission;
import com.example.aigc.entity.ScriptProject;
import com.example.aigc.entity.TeachingAssignment;
import com.example.aigc.entity.TeachingCourse;
import com.example.aigc.enums.UserRole;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AssignmentSubmissionRepository;
import com.example.aigc.repository.TeachingAssignmentRepository;
import com.example.aigc.repository.UserPermissionRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AuthorizationService {
    private static final Set<String> ADMIN_PERMISSIONS = Set.of("*");
    private static final EnumMap<UserRole, Set<String>> ROLE_PERMISSIONS = buildRolePermissions();

    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final UserPermissionRepository userPermissionRepository;

    public AuthorizationService(
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            TeachingAssignmentRepository teachingAssignmentRepository,
            UserPermissionRepository userPermissionRepository
    ) {
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    public void requireTeachingManager(RequestUserContext userContext, String message) {
        if (userContext == null || !userContext.canManageTeaching()) {
            throw new BizException(403, message);
        }
    }

    public void requireAdmin(RequestUserContext userContext, String message) {
        if (userContext == null || !userContext.isAdmin()) {
            throw new BizException(403, message);
        }
    }

    public void requirePermission(RequestUserContext userContext, String permission, String message) {
        if (!hasPermission(userContext, permission)) {
            throw new BizException(403, message);
        }
    }

    public boolean hasPermission(RequestUserContext userContext, String permission) {
        if (userContext == null || userContext.role() == null || permission == null || permission.isBlank()) {
            return false;
        }
        Set<String> permissions = listPermissions(userContext);
        return permissions.contains("*") || permissions.contains(permission);
    }

    public Set<String> listPermissions(RequestUserContext userContext) {
        if (userContext == null || userContext.role() == null) {
            return Set.of();
        }
        Set<String> dbPermissions = userPermissionRepository.findPermissionCodesByUserId(userContext.userId());
        if (!dbPermissions.isEmpty()) {
            return dbPermissions;
        }
        return ROLE_PERMISSIONS.getOrDefault(userContext.role(), Set.of());
    }

    public boolean canViewCourse(TeachingCourse course, RequestUserContext userContext) {
        if (course == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        if (Objects.equals(course.ownerId, userContext.userId())) {
            return true;
        }
        String userOrgUnitId = blankToNull(userContext.orgUnitId());
        if (userOrgUnitId != null && Objects.equals(userOrgUnitId, blankToNull(course.orgUnitId))) {
            return true;
        }
        String currentCourseId = blankToNull(userContext.courseId());
        return currentCourseId != null && Objects.equals(currentCourseId, course.courseId);
    }

    public boolean canManageCourse(TeachingCourse course, RequestUserContext userContext) {
        if (course == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        return userContext.canManageTeaching() && Objects.equals(course.ownerId, userContext.userId());
    }

    public boolean canReviewAssignment(TeachingAssignment assignment, RequestUserContext userContext) {
        if (assignment == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        return userContext.canManageTeaching() && Objects.equals(assignment.ownerId, userContext.userId());
    }

    public boolean canViewSubmission(TeachingAssignment assignment, AssignmentSubmission submission, RequestUserContext userContext) {
        if (assignment == null || submission == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        if (canReviewAssignment(assignment, userContext)) {
            return true;
        }
        return Objects.equals(submission.studentUserId, userContext.userId());
    }

    public boolean canReadProject(ScriptProject project, RequestUserContext userContext) {
        if (project == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        if (Objects.equals(project.ownerId, userContext.userId())) {
            return true;
        }
        if (!userContext.canManageTeaching()) {
            return false;
        }
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findAllByProjectId(project.projectId);
        for (AssignmentSubmission submission : submissions) {
            TeachingAssignment assignment = teachingAssignmentRepository.findById(submission.assignmentId).orElse(null);
            if (assignment != null && canReviewAssignment(assignment, userContext)) {
                return true;
            }
        }
        return false;
    }

    public boolean canWriteProject(ScriptProject project, RequestUserContext userContext) {
        if (project == null || userContext == null) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        return Objects.equals(project.ownerId, userContext.userId());
    }

    public boolean canDeleteProject(ScriptProject project, RequestUserContext userContext) {
        return canWriteProject(project, userContext);
    }

    public boolean canSubmitProjectReview(ScriptProject project, RequestUserContext userContext) {
        return canWriteProject(project, userContext);
    }

    public boolean canProcessProjectReview(ScriptProject project, RequestUserContext userContext) {
        if (project == null || userContext == null) {
            return false;
        }
        if (Objects.equals(project.ownerId, userContext.userId())) {
            return false;
        }
        if (userContext.isAdmin()) {
            return true;
        }
        return userContext.canManageTeaching() && canReadProject(project, userContext);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static EnumMap<UserRole, Set<String>> buildRolePermissions() {
        EnumMap<UserRole, Set<String>> mapping = new EnumMap<>(UserRole.class);
        mapping.put(UserRole.ADMIN, ADMIN_PERMISSIONS);
        mapping.put(UserRole.TEACHER, Set.of(
                AccountPermissionPoints.MENU_ACCOUNT_DIRECTORY,
                AccountPermissionPoints.API_ORG_UNIT_LIST,
                AccountPermissionPoints.API_USER_LIST,
                AccountPermissionPoints.API_USER_IMPORT_TEMPLATE,
                AccountPermissionPoints.API_USER_EXPORT,
                AccountPermissionPoints.API_USER_IMPORT_TASK_QUERY,
                AccountPermissionPoints.API_USER_BATCH_STATS
        ));
        mapping.put(UserRole.STUDENT, Set.of());
        return mapping;
    }
}
