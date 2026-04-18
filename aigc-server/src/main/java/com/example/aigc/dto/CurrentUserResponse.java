package com.example.aigc.dto;

import com.example.aigc.enums.UserRole;

public record CurrentUserResponse(
        String userId,
        String username,
        String displayName,
        UserRole role,
        String orgUnitId,
        String classroomId,
        boolean enabled
) {
}
