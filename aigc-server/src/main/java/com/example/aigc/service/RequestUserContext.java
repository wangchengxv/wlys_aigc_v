package com.example.aigc.service;

import com.example.aigc.enums.UserRole;

public record RequestUserContext(
        String userId,
        String userName,
        UserRole role,
        String orgUnitId,
        String courseId,
        boolean authenticated
) {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean canManageTeaching() {
        return role != null && role.canManageTeaching();
    }
}
