package com.example.aigc.service;

import com.example.aigc.dto.CurrentUserResponse;
import com.example.aigc.dto.LoginResponse;
import com.example.aigc.dto.SocialAuthUrlResponse;
import com.example.aigc.dto.SocialLinkItemResponse;
import com.example.aigc.dto.SocialUserInfo;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.SocialAccount;
import com.example.aigc.enums.UserRole;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.SocialAccountRepository;
import com.example.aigc.service.social.SocialProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SocialAuthService {
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("onelinkai", "wechat");

    private final AppUserRepository appUserRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordCodec passwordCodec;
    private final Map<String, SocialProvider> socialProviders;
    private final Map<String, Instant> stateStore = new ConcurrentHashMap<>();

    public SocialAuthService(
            AppUserRepository appUserRepository,
            SocialAccountRepository socialAccountRepository,
            JwtTokenService jwtTokenService,
            PasswordCodec passwordCodec,
            List<SocialProvider> socialProviders
    ) {
        this.appUserRepository = appUserRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordCodec = passwordCodec;
        this.socialProviders = socialProviders.stream()
                .collect(Collectors.toMap(provider -> provider.provider().toLowerCase(Locale.ROOT), Function.identity(), (left, right) -> left));
    }

    public SocialAuthUrlResponse buildAuthUrl(String provider) {
        SocialProvider socialProvider = resolveProvider(provider);
        cleanupExpiredStates(Instant.now());
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.put(state, Instant.now().plus(Duration.ofMinutes(10)));
        String authUrl = socialProvider.getAuthUrl(state);
        return new SocialAuthUrlResponse(socialProvider.provider(), authUrl);
    }

    public LoginResponse handleCallback(String provider, String code, String state, String clientIp) {
        SocialProvider socialProvider = resolveProvider(provider);
        validateState(state);
        String accessToken = socialProvider.exchangeCodeForToken(code);
        SocialUserInfo userInfo = socialProvider.getUserInfo(accessToken);
        AppUser user = resolveOrCreateUser(userInfo, socialProvider.provider(), clientIp);
        JwtTokenService.TokenPayload tokenPayload = jwtTokenService.createToken(user);
        return new LoginResponse(
                tokenPayload.accessToken(),
                "Bearer",
                tokenPayload.expiresAt(),
                toCurrentUser(user)
        );
    }

    public List<SocialLinkItemResponse> getLinks(String userId) {
        return socialAccountRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing((SocialAccount account) -> account.linkedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(account -> new SocialLinkItemResponse(account.provider, account.providerUserId, account.linkedAt))
                .toList();
    }

    public void unbind(String userId, String provider) {
        String normalizedProvider = resolveProvider(provider).provider();
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        SocialAccount linked = socialAccountRepository.findByUserIdAndProvider(userId, normalizedProvider)
                .orElseThrow(() -> new BizException(404, "未找到已绑定的第三方账号"));
        long linkCount = socialAccountRepository.countByUserId(userId);
        boolean socialOnlyUser = normalizedProvider.equals(normalizeText(user.provider).toLowerCase(Locale.ROOT))
                && normalizeText(user.providerUserId).equals(linked.providerUserId);
        if (socialOnlyUser && linkCount <= 1) {
            throw new BizException(400, "当前账号仅绑定该第三方登录，请先设置密码后再解绑");
        }
        socialAccountRepository.delete(linked);
        if (normalizedProvider.equals(normalizeText(user.provider).toLowerCase(Locale.ROOT))
                && normalizeText(user.providerUserId).equals(linked.providerUserId)) {
            user.provider = null;
            user.providerUserId = null;
            user.linkedAt = null;
            user.updatedAt = Instant.now();
            appUserRepository.save(user);
        }
    }

    private AppUser resolveOrCreateUser(SocialUserInfo socialUserInfo, String provider, String clientIp) {
        Instant now = Instant.now();
        String providerUserId = normalizeText(socialUserInfo.id());
        if (providerUserId.isBlank()) {
            throw new BizException(502, "第三方用户信息缺少用户标识");
        }
        AppUser existing = appUserRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElse(null);
        if (existing == null) {
            SocialAccount linked = socialAccountRepository
                    .findByProviderAndProviderUserId(provider, providerUserId)
                    .orElse(null);
            if (linked != null) {
                existing = appUserRepository.findById(linked.userId).orElse(null);
            }
        }
        if (existing != null) {
            existing.lastLoginAt = now;
            existing.lastLoginIp = clientIp;
            existing.updatedAt = now;
            return appUserRepository.save(existing);
        }

        AppUser user = new AppUser();
        user.userId = "user-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        user.username = buildUniqueUsername(socialUserInfo, provider, providerUserId);
        user.passwordHash = passwordCodec.encode("social-only-" + UUID.randomUUID());
        user.displayName = !socialUserInfo.displayName().isBlank() ? socialUserInfo.displayName() : user.username;
        user.role = UserRole.STUDENT;
        user.enabled = true;
        user.locked = false;
        user.failedLoginCount = 0;
        user.forcePasswordChange = false;
        user.sessionVersion = 0L;
        user.provider = provider;
        user.providerUserId = providerUserId;
        user.linkedAt = now;
        user.lastLoginAt = now;
        user.lastLoginIp = clientIp;
        user.passwordUpdatedAt = now;
        user.createdAt = now;
        user.updatedAt = now;
        AppUser saved = appUserRepository.save(user);

        SocialAccount account = new SocialAccount();
        account.userId = saved.userId;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.linkedAt = now;
        socialAccountRepository.save(account);

        return saved;
    }

    private String buildUniqueUsername(SocialUserInfo socialUserInfo, String provider, String providerUserId) {
        String base = !socialUserInfo.username().isBlank()
                ? sanitizeUsername(socialUserInfo.username())
                : (provider + "_" + sanitizeUsername(providerUserId));
        if (base.isBlank()) {
            base = provider + "_user";
        }
        String candidate = base;
        int suffix = 1;
        while (appUserRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private void validateState(String state) {
        if (state == null || state.isBlank()) {
            throw new BizException(400, "缺少 OAuth state 参数");
        }
        Instant now = Instant.now();
        cleanupExpiredStates(now);
        Instant expiresAt = stateStore.remove(state);
        if (expiresAt == null || expiresAt.isBefore(now)) {
            throw new BizException(400, "OAuth state 无效或已过期");
        }
    }

    private void cleanupExpiredStates(Instant now) {
        stateStore.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }

    private SocialProvider resolveProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new BizException(400, "不支持的第三方登录渠道");
        }
        SocialProvider socialProvider = socialProviders.get(normalized);
        if (socialProvider == null) {
            throw new BizException(500, "第三方登录组件未注册: " + normalized);
        }
        return socialProvider;
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

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sanitizeUsername(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String safe = normalized.replaceAll("[^a-z0-9_\\-\\.]", "_");
        return safe.replaceAll("_+", "_");
    }

}
