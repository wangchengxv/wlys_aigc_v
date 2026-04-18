package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.entity.AppUser;
import com.example.aigc.enums.UserRole;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class RequestAuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    private final AppUserRepository appUserRepository;

    public RequestAuthService(
            AuthProperties authProperties,
            JwtTokenService jwtTokenService,
            AppUserRepository appUserRepository
    ) {
        this.authProperties = authProperties;
        this.jwtTokenService = jwtTokenService;
        this.appUserRepository = appUserRepository;
    }

    public String requireUserId(String authorization, String xAigcToken, String xUserId) {
        return requireUserContext(authorization, xAigcToken, xUserId, null, null, null).userId();
    }

    public RequestUserContext requireUserContext(
            String authorization,
            String xAigcToken,
            String xUserId,
            String xUserName,
            String xOrgUnitId,
            String xCourseId
    ) {
        String token = extractToken(authorization, xAigcToken);
        String courseId = normalizeOptionalHeader(xCourseId);
        if (!token.isBlank() && !isDevelopmentToken(token)) {
            try {
                JwtTokenService.AuthenticatedPrincipal principal = jwtTokenService.parseToken(token);
                AppUser user = appUserRepository.findById(principal.userId())
                        .orElseThrow(() -> new BizException(401, "登录态已失效，请重新登录"));
                if (!user.enabled) {
                    throw new BizException(403, "当前账号已被停用");
                }
                if (user.locked) {
                    throw new BizException(403, "当前账号已被锁定");
                }
                if (principal.sessionVersion() != user.sessionVersion) {
                    throw new BizException(401, "登录态已失效，请重新登录");
                }
                return new RequestUserContext(
                        user.userId,
                        displayName(user.displayName, user.username, user.userId),
                        user.role == null ? UserRole.STUDENT : user.role,
                        normalizeOptionalHeader(user.orgUnitId),
                        courseId,
                        true
                );
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BizException(401, "登录态已失效，请重新登录");
            }
        }

        if (authProperties.isDevelopmentHeadersEnabled() && isDevelopmentToken(token)) {
            String userId = normalizeOptionalHeader(xUserId);
            if (authProperties.isUserIdRequired() && userId == null) {
                throw new BizException(401, "缺少用户标识，请设置 x-user-id");
            }
            String effectiveUserId = userId == null ? "anonymous" : userId;
            String userName = normalizeOptionalHeader(xUserName);
            if (userName == null) {
                userName = effectiveUserId;
            }
            return new RequestUserContext(
                    effectiveUserId,
                    userName,
                    inferRoleFromUserId(effectiveUserId),
                    normalizeOptionalHeader(xOrgUnitId),
                    courseId,
                    true
            );
        }
        throw new BizException(401, "未授权访问");
    }

    public void requireAuthorized(String authorization, String xAigcToken) {
        String token = extractToken(authorization, xAigcToken);
        if (token.isBlank()) {
            throw new BizException(401, "未授权访问");
        }
        if (isDevelopmentToken(token)) {
            return;
        }
        try {
            JwtTokenService.AuthenticatedPrincipal principal = jwtTokenService.parseToken(token);
            AppUser user = appUserRepository.findById(principal.userId())
                    .orElseThrow(() -> new BizException(401, "登录态已失效，请重新登录"));
            if (!user.enabled || user.locked || principal.sessionVersion() != user.sessionVersion) {
                throw new BizException(401, "登录态已失效，请重新登录");
            }
        } catch (Exception ex) {
            throw new BizException(401, "登录态已失效，请重新登录");
        }
    }

    private String extractToken(String authorization, String xAigcToken) {
        if (authorization != null) {
            String auth = authorization.trim();
            if (auth.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                return auth.substring(BEARER_PREFIX.length()).trim();
            }
            if (!auth.isBlank()) {
                return auth;
            }
        }
        return xAigcToken == null ? "" : xAigcToken.trim();
    }

    private boolean isDevelopmentToken(String token) {
        String expected = authProperties.getAccessToken() == null ? "" : authProperties.getAccessToken().trim();
        return authProperties.isDevelopmentHeadersEnabled() && !expected.isBlank() && expected.equals(token);
    }

    private UserRole inferRoleFromUserId(String userId) {
        String normalized = userId == null ? "" : userId.trim().toLowerCase();
        if (normalized.startsWith("admin")) {
            return UserRole.ADMIN;
        }
        if (normalized.startsWith("teacher")) {
            return UserRole.TEACHER;
        }
        return UserRole.STUDENT;
    }

    private String displayName(String displayName, String username, String userId) {
        String normalizedDisplayName = normalizeOptionalHeader(displayName);
        if (normalizedDisplayName != null) {
            return normalizedDisplayName;
        }
        String normalizedUsername = normalizeOptionalHeader(username);
        if (normalizedUsername != null) {
            return normalizedUsername;
        }
        return userId;
    }

    private String normalizeOptionalHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
