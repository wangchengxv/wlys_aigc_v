package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.CurrentUserResponse;
import com.example.aigc.dto.LoginResponse;
import com.example.aigc.dto.SocialAuthUrlResponse;
import com.example.aigc.entity.AppUser;
import com.example.aigc.entity.SocialAccount;
import com.example.aigc.enums.UserRole;
import com.example.aigc.exception.BizException;
import com.example.aigc.repository.AppUserRepository;
import com.example.aigc.repository.SocialAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocialAuthService {
    private static final String PROVIDER_ONELINKAI = "onelinkai";

    private final AppUserRepository appUserRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordCodec passwordCodec;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, Instant> stateStore = new ConcurrentHashMap<>();

    public SocialAuthService(
            AppUserRepository appUserRepository,
            SocialAccountRepository socialAccountRepository,
            JwtTokenService jwtTokenService,
            PasswordCodec passwordCodec,
            AuthProperties authProperties,
            ObjectMapper objectMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordCodec = passwordCodec;
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public SocialAuthUrlResponse buildAuthUrl(String provider) {
        String normalized = normalizeProvider(provider);
        AuthProperties.OnelinkaiProperties config = requireOnelinkaiConfig();
        cleanupExpiredStates(Instant.now());
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.put(state, Instant.now().plus(Duration.ofMinutes(10)));
        String authUrl = config.getAuthorizeUri()
                + "?response_type=code"
                + "&client_id=" + encode(config.getClientId())
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&scope=" + encode(config.getScope())
                + "&state=" + encode(state);
        return new SocialAuthUrlResponse(normalized, authUrl);
    }

    public LoginResponse handleCallback(String provider, String code, String state, String clientIp) {
        normalizeProvider(provider);
        AuthProperties.OnelinkaiProperties config = requireOnelinkaiConfig();
        validateState(state);
        String accessToken = exchangeCodeForToken(config, code);
        SocialUserInfo userInfo = fetchSocialUserInfo(config, accessToken);
        AppUser user = resolveOrCreateUser(userInfo, clientIp);
        JwtTokenService.TokenPayload tokenPayload = jwtTokenService.createToken(user);
        return new LoginResponse(
                tokenPayload.accessToken(),
                "Bearer",
                tokenPayload.expiresAt(),
                toCurrentUser(user)
        );
    }

    private String exchangeCodeForToken(AuthProperties.OnelinkaiProperties config, String code) {
        String formBody = "grant_type=authorization_code"
                + "&client_id=" + encode(config.getClientId())
                + "&client_secret=" + encode(config.getClientSecret())
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(config.getRedirectUri());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getTokenUri()))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        JsonNode payload = executeJson(request, "OneLinkAI token 交换失败");
        String token = firstText(payload, "access_token", "token", "data.access_token");
        if (token == null || token.isBlank()) {
            throw new BizException(502, "OneLinkAI token 响应缺少 access_token");
        }
        return token;
    }

    private SocialUserInfo fetchSocialUserInfo(AuthProperties.OnelinkaiProperties config, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getUserInfoUri()))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonNode payload = executeJson(request, "OneLinkAI 用户信息拉取失败");
        String providerUserId = firstText(payload, "sub", "id", "user_id", "data.id", "data.user_id");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BizException(502, "OneLinkAI 用户信息缺少用户标识");
        }
        String username = firstText(payload, "preferred_username", "username", "name", "data.username", "data.name");
        String displayName = firstText(payload, "nickname", "display_name", "name", "data.nickname", "data.display_name");
        return new SocialUserInfo(providerUserId.trim(), normalizeText(username), normalizeText(displayName));
    }

    private AppUser resolveOrCreateUser(SocialUserInfo socialUserInfo, String clientIp) {
        Instant now = Instant.now();
        AppUser existing = appUserRepository.findByProviderAndProviderUserId(PROVIDER_ONELINKAI, socialUserInfo.providerUserId())
                .orElse(null);
        if (existing == null) {
            SocialAccount linked = socialAccountRepository
                    .findByProviderAndProviderUserId(PROVIDER_ONELINKAI, socialUserInfo.providerUserId())
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
        user.username = buildUniqueUsername(socialUserInfo);
        user.passwordHash = passwordCodec.encode("social-only-" + UUID.randomUUID());
        user.displayName = !socialUserInfo.displayName().isBlank() ? socialUserInfo.displayName() : user.username;
        user.role = UserRole.STUDENT;
        user.enabled = true;
        user.locked = false;
        user.failedLoginCount = 0;
        user.forcePasswordChange = false;
        user.sessionVersion = 0L;
        user.provider = PROVIDER_ONELINKAI;
        user.providerUserId = socialUserInfo.providerUserId();
        user.linkedAt = now;
        user.lastLoginAt = now;
        user.lastLoginIp = clientIp;
        user.passwordUpdatedAt = now;
        user.createdAt = now;
        user.updatedAt = now;
        AppUser saved = appUserRepository.save(user);

        SocialAccount account = new SocialAccount();
        account.userId = saved.userId;
        account.provider = PROVIDER_ONELINKAI;
        account.providerUserId = socialUserInfo.providerUserId();
        account.linkedAt = now;
        socialAccountRepository.save(account);

        return saved;
    }

    private String buildUniqueUsername(SocialUserInfo socialUserInfo) {
        String base = !socialUserInfo.username().isBlank()
                ? sanitizeUsername(socialUserInfo.username())
                : ("onelinkai_" + sanitizeUsername(socialUserInfo.providerUserId()));
        if (base.isBlank()) {
            base = "onelinkai_user";
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

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if (!PROVIDER_ONELINKAI.equals(normalized)) {
            throw new BizException(400, "当前仅支持 onelinkai 第三方登录");
        }
        return normalized;
    }

    private AuthProperties.OnelinkaiProperties requireOnelinkaiConfig() {
        AuthProperties.OnelinkaiProperties config = authProperties.getSocial().getOnelinkai();
        if (!config.isEnabled()) {
            throw new BizException(403, "OneLinkAI 第三方登录未启用");
        }
        if (isBlank(config.getClientId()) || isBlank(config.getClientSecret())) {
            throw new BizException(500, "OneLinkAI 第三方登录配置不完整");
        }
        if (isBlank(config.getAuthorizeUri()) || isBlank(config.getTokenUri()) || isBlank(config.getUserInfoUri()) || isBlank(config.getRedirectUri())) {
            throw new BizException(500, "OneLinkAI OAuth 地址配置不完整");
        }
        return config;
    }

    private JsonNode executeJson(HttpRequest request, String fallbackMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new BizException(502, fallbackMessage);
            }
            return objectMapper.readTree(response.body());
        } catch (BizException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(502, fallbackMessage);
        } catch (IOException ex) {
            throw new BizException(502, fallbackMessage);
        }
    }

    private String firstText(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode cursor = node;
            for (String key : path.split("\\.")) {
                if (cursor == null) {
                    break;
                }
                cursor = cursor.get(key);
            }
            if (cursor != null && !cursor.isNull()) {
                String value = cursor.asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
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

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sanitizeUsername(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String safe = normalized.replaceAll("[^a-z0-9_\\-\\.]", "_");
        return safe.replaceAll("_+", "_");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SocialUserInfo(String providerUserId, String username, String displayName) {
    }
}
