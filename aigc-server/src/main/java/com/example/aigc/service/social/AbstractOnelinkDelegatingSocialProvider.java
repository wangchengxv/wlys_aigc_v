package com.example.aigc.service.social;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.SocialUserInfo;
import com.example.aigc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 当前阶段仅保留 OneLinkAI 统一 OAuth 网关，因此 GitHub/Google/Wecom 通过同一组配置代理接入。
 */
abstract class AbstractOnelinkDelegatingSocialProvider implements SocialProvider {
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    protected AbstractOnelinkDelegatingSocialProvider(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String getAuthUrl(String state) {
        AuthProperties.OnelinkaiProperties config = requireOnelinkaiConfig();
        return config.getAuthorizeUri()
                + "?response_type=code"
                + "&client_id=" + encode(config.getClientId())
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&scope=" + encode(config.getScope())
                + "&state=" + encode(state);
    }

    @Override
    public String exchangeCodeForToken(String code) {
        AuthProperties.OnelinkaiProperties config = requireOnelinkaiConfig();
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
        JsonNode payload = executeJson(request, providerDisplayName() + " token 交换失败");
        String token = firstText(payload, "access_token", "token", "data.access_token");
        if (token == null || token.isBlank()) {
            throw new BizException(502, providerDisplayName() + " token 响应缺少 access_token");
        }
        return token;
    }

    @Override
    public SocialUserInfo getUserInfo(String accessToken) {
        AuthProperties.OnelinkaiProperties config = requireOnelinkaiConfig();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getUserInfoUri()))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        JsonNode payload = executeJson(request, providerDisplayName() + " 用户信息拉取失败");
        String providerUserId = firstText(payload, "sub", "id", "user_id", "data.id", "data.user_id");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BizException(502, providerDisplayName() + " 用户信息缺少用户标识");
        }
        String username = firstText(payload, "preferred_username", "username", "name", "data.username", "data.name");
        String displayName = firstText(payload, "nickname", "display_name", "name", "data.nickname", "data.display_name");
        String email = firstText(payload, "email", "data.email");
        String avatar = firstText(payload, "avatar_url", "avatar", "picture", "data.avatar_url", "data.avatar");
        return new SocialUserInfo(
                providerUserId,
                normalizeText(username),
                normalizeText(displayName),
                normalizeText(email),
                normalizeText(avatar),
                provider()
        );
    }

    protected abstract String providerDisplayName();

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

    private static String firstText(JsonNode node, String... paths) {
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

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
