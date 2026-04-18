package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;

public record AdminUserResponse(
        String userId,
        String username,
        String displayName,
        UserRole role,
        String orgUnitId,
        String classroomId,
        boolean enabled,
        boolean locked,
        String lockReason,
        String lockedAt,
        int failedLoginCount,
        String lastLoginAt,
        String lastLoginIp,
        String passwordUpdatedAt,
        boolean forcePasswordChange,
        String createdAt,
        String updatedAt
) {
}
