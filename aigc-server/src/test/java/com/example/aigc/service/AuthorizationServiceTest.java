package com.example.aigc.service;

import com.example.aigc.enums.UserRole;
import com.example.aigc.repository.AssignmentSubmissionRepository;
import com.example.aigc.repository.TeachingAssignmentRepository;
import com.example.aigc.repository.UserPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private TeachingAssignmentRepository teachingAssignmentRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                assignmentSubmissionRepository,
                teachingAssignmentRepository,
                userPermissionRepository
        );
    }

    @Test
    void shouldUseDatabasePermissionsWhenExists() {
        RequestUserContext userContext = new RequestUserContext(
                "teacher-001",
                "teacher",
                UserRole.TEACHER,
                null,
                null,
                true
        );
        when(userPermissionRepository.findPermissionCodesByUserId("teacher-001"))
                .thenReturn(Set.of(AccountPermissionPoints.API_USER_CREATE));

        assertThat(authorizationService.listPermissions(userContext))
                .containsExactly(AccountPermissionPoints.API_USER_CREATE);
        assertThat(authorizationService.hasPermission(userContext, AccountPermissionPoints.API_USER_CREATE))
                .isTrue();
        assertThat(authorizationService.hasPermission(userContext, AccountPermissionPoints.API_USER_LIST))
                .isFalse();
    }

    @Test
    void shouldFallbackToStaticPermissionsWhenDatabaseIsEmpty() {
        RequestUserContext userContext = new RequestUserContext(
                "teacher-001",
                "teacher",
                UserRole.TEACHER,
                null,
                null,
                true
        );
        when(userPermissionRepository.findPermissionCodesByUserId("teacher-001"))
                .thenReturn(Set.of());

        assertThat(authorizationService.listPermissions(userContext))
                .contains(AccountPermissionPoints.API_USER_LIST, AccountPermissionPoints.API_USER_EXPORT);
        assertThat(authorizationService.hasPermission(userContext, AccountPermissionPoints.API_USER_LIST))
                .isTrue();
        assertThat(authorizationService.hasPermission(userContext, AccountPermissionPoints.API_USER_CREATE))
                .isFalse();
    }

    @Test
    void shouldFallbackAdminToWildcardWhenDatabaseIsEmpty() {
        RequestUserContext userContext = new RequestUserContext(
                "admin-001",
                "admin",
                UserRole.ADMIN,
                null,
                null,
                true
        );
        when(userPermissionRepository.findPermissionCodesByUserId("admin-001"))
                .thenReturn(Set.of());

        assertThat(authorizationService.hasPermission(userContext, AccountPermissionPoints.API_USER_CREATE))
                .isTrue();
        assertThat(authorizationService.hasPermission(userContext, "any:custom:permission"))
                .isTrue();
    }
}
