package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.CurrentUserResponse;
import com.example.aigc.dto.LoginRequest;
import com.example.aigc.dto.LoginResponse;
import com.example.aigc.entity.AppUser;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.SocialAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordCodec passwordCodec;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;

    public AuthService(
            AppUserRepository appUserRepository,
            SocialAccountRepository socialAccountRepository,
            PasswordCodec passwordCodec,
            JwtTokenService jwtTokenService,
            AuthProperties authProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.passwordCodec = passwordCodec;
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
    }

    public LoginResponse login(LoginRequest request, String clientIp) {
        Instant now = Instant.now();
        String username = request.username().trim();
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!user.enabled) {
            throw new BizException(403, "当前账号已被停用");
        }
        if (isLocked(user, now)) {
            throw new BizException(403, "当前账号已被锁定");
        }
        if (!passwordCodec.matches(request.password(), user.passwordHash)) {
            int threshold = Math.max(authProperties.getLoginFailureThreshold(), 1);
            user.failedLoginCount = user.failedLoginCount + 1;
            if (user.failedLoginCount >= threshold) {
                user.locked = true;
                user.lockReason = "LOGIN_FAILED_TOO_MANY_TIMES";
                user.lockedAt = now;
                user.sessionVersion = user.sessionVersion + 1;
            }
            user.updatedAt = now;
            appUserRepository.save(user);
            throw new BizException(401, "用户名或密码错误");
        }
        user.failedLoginCount = 0;
        user.locked = false;
        user.lockReason = null;
        user.lockedAt = null;
        user.lastLoginAt = now;
        user.lastLoginIp = clientIp;
        user.updatedAt = now;
        AppUser saved = appUserRepository.save(user);
        JwtTokenService.TokenPayload tokenPayload = jwtTokenService.createToken(saved);
        return new LoginResponse(
                tokenPayload.accessToken(),
                "Bearer",
                tokenPayload.expiresAt(),
                toCurrentUser(saved)
        );
    }

    public CurrentUserResponse getCurrentUser(String userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(401, "登录态已失效，请重新登录"));
        if (!user.enabled) {
            throw new BizException(403, "当前账号已被停用");
        }
        if (isLocked(user, Instant.now())) {
            throw new BizException(403, "当前账号已被锁定");
        }
        return toCurrentUser(user);
    }

    public LoginResponse socialLogin(String provider, String providerUserId, String clientIp) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedProviderUserId = providerUserId == null ? "" : providerUserId.trim();
        if (normalizedProviderUserId.isBlank()) {
            throw new BizException(400, "第三方用户标识不能为空");
        }
        AppUser user = appUserRepository.findByProviderAndProviderUserId(normalizedProvider, normalizedProviderUserId)
                .orElseGet(() -> socialAccountRepository.findByProviderAndProviderUserId(normalizedProvider, normalizedProviderUserId)
                        .flatMap(account -> appUserRepository.findById(account.userId))
                        .orElseThrow(() -> new BizException(401, "第三方账号未绑定本地用户")));
        return issueLoginToken(user, clientIp);
    }

    private LoginResponse issueLoginToken(AppUser user, String clientIp) {
        Instant now = Instant.now();
        if (!user.enabled) {
            throw new BizException(403, "当前账号已被停用");
        }
        if (isLocked(user, now)) {
            throw new BizException(403, "当前账号已被锁定");
        }
        user.failedLoginCount = 0;
        user.locked = false;
        user.lockReason = null;
        user.lockedAt = null;
        user.lastLoginAt = now;
        user.lastLoginIp = clientIp;
        user.updatedAt = now;
        AppUser saved = appUserRepository.save(user);
        JwtTokenService.TokenPayload tokenPayload = jwtTokenService.createToken(saved);
        return new LoginResponse(
                tokenPayload.accessToken(),
                "Bearer",
                tokenPayload.expiresAt(),
                toCurrentUser(saved)
        );
    }

    private CurrentUserResponse toCurrentUser(AppUser user) {
        return new CurrentUserResponse(
                user.userId,
                user.username,
                user.displayName == null || user.displayName.isBlank() ? user.username : user.displayName,
                user.role,
                user.orgUnitId,
                user.classroomId,
                user.enabled
        );
    }

    private boolean isLocked(AppUser user, Instant now) {
        if (!user.locked) {
            return false;
        }
        long lockDurationMinutes = Math.max(authProperties.getLockDurationMinutes(), 0);
        if (user.lockedAt == null || lockDurationMinutes == 0) {
            return true;
        }
        Instant unlockAt = user.lockedAt.plus(lockDurationMinutes, ChronoUnit.MINUTES);
        if (unlockAt.isAfter(now)) {
            return true;
        }
        user.locked = false;
        user.lockReason = null;
        user.lockedAt = null;
        user.failedLoginCount = 0;
        user.updatedAt = now;
        appUserRepository.save(user);
        return false;
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BizException(400, "第三方 provider 不能为空");
        }
        return normalized;
    }
}
