package com.example.aigc.enums;

public enum UserRole {
    ADMIN,
    TEACHER,
    STUDENT;

    public boolean canManageTeaching() {
        return this == ADMIN || this == TEACHER;
    }
}
