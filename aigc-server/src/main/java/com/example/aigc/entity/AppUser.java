package com.example.aigc.entity;

import com.example.aigc.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @Column(name = "user_id")
    public String userId;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "display_name")
    public String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public UserRole role;

    @Column(name = "org_unit_id")
    public String orgUnitId;

    @Column(name = "classroom_id")
    public String classroomId;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "locked", nullable = false)
    public boolean locked;

    @Column(name = "lock_reason")
    public String lockReason;

    @Column(name = "locked_at")
    public Instant lockedAt;

    @Column(name = "failed_login_count", nullable = false)
    public int failedLoginCount;

    @Column(name = "last_login_at")
    public Instant lastLoginAt;

    @Column(name = "last_login_ip")
    public String lastLoginIp;

    @Column(name = "password_updated_at")
    public Instant passwordUpdatedAt;

    @Column(name = "force_password_change", nullable = false)
    public boolean forcePasswordChange;

    @Column(name = "session_version", nullable = false)
    public long sessionVersion;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
