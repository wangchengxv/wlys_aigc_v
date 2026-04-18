package com.example.aigc.config;

import com.example.aigc.enums.UserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "aigc.auth")
public class AuthProperties {
    private String accessToken = "dev-local-token";
    private boolean userIdRequired = true;
    private boolean developmentHeadersEnabled = true;
    private String jwtSecret = "change-this-jwt-secret-change-this-jwt-secret";
    private String jwtIssuer = "aigc-server";
    private long jwtExpireMinutes = 720;
    private int loginFailureThreshold = 5;
    private long lockDurationMinutes = 30;
    private boolean forcePasswordChangeOnReset = false;
    private List<SeedUser> seedUsers = new ArrayList<>();

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public boolean isUserIdRequired() {
        return userIdRequired;
    }

    public void setUserIdRequired(boolean userIdRequired) {
        this.userIdRequired = userIdRequired;
    }

    public boolean isDevelopmentHeadersEnabled() {
        return developmentHeadersEnabled;
    }

    public void setDevelopmentHeadersEnabled(boolean developmentHeadersEnabled) {
        this.developmentHeadersEnabled = developmentHeadersEnabled;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public long getJwtExpireMinutes() {
        return jwtExpireMinutes;
    }

    public void setJwtExpireMinutes(long jwtExpireMinutes) {
        this.jwtExpireMinutes = jwtExpireMinutes;
    }

    public int getLoginFailureThreshold() {
        return loginFailureThreshold;
    }

    public void setLoginFailureThreshold(int loginFailureThreshold) {
        this.loginFailureThreshold = loginFailureThreshold;
    }

    public long getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(long lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public boolean isForcePasswordChangeOnReset() {
        return forcePasswordChangeOnReset;
    }

    public void setForcePasswordChangeOnReset(boolean forcePasswordChangeOnReset) {
        this.forcePasswordChangeOnReset = forcePasswordChangeOnReset;
    }

    public List<SeedUser> getSeedUsers() {
        return seedUsers;
    }

    public void setSeedUsers(List<SeedUser> seedUsers) {
        this.seedUsers = seedUsers == null ? new ArrayList<>() : seedUsers;
    }

    public static class SeedUser {
        private String userId;
        private String username;
        private String password;
        private String displayName;
        private UserRole role = UserRole.STUDENT;
        private String orgUnitId;
        private String classroomId;
        private boolean enabled = true;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }

        public String getOrgUnitId() {
            return orgUnitId;
        }

        public void setOrgUnitId(String orgUnitId) {
            this.orgUnitId = orgUnitId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClassroomId() {
            return classroomId;
        }

        public void setClassroomId(String classroomId) {
            this.classroomId = classroomId;
        }
    }
}
