package com.example.aigc.service.social;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.dto.SocialUserInfo;
import com.example.aigc.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WechatSocialProvider implements SocialProvider {
    private static final String PROVIDER = "wechat";

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatSocialProvider(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String getAuthUrl(String state) {
        AuthProperties.WechatProperties config = requireWechatConfig();
        return config.getAuthorizeUri()
                + "?appid=" + encode(config.getAppId())
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(config.getScope())
                + "&state=" + encode(state)
                + "#wechat_redirect";
    }

    @Override
    public String exchangeCodeForToken(String code) {
        AuthProperties.WechatProperties config = requireWechatConfig();
        String tokenUrl = config.getTokenUri()
                + "?appid=" + encode(config.getAppId())
                + "&secret=" + encode(config.getAppSecret())
                + "&code=" + encode(code)
                + "&grant_type=authorization_code";
        JsonNode payload = executeJson(HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build(), "微信 token 交换失败");
        assertWechatSuccess(payload, "微信 token 交换失败");
        String accessToken = firstText(payload, "access_token");
        String openId = firstText(payload, "openid");
        String unionId = firstText(payload, "unionid");
        if (isBlank(accessToken) || isBlank(openId)) {
            throw new BizException(502, "微信 token 响应缺少 access_token 或 openid");
        }
        return accessToken + "|" + openId + "|" + normalizeText(unionId);
    }

    @Override
    public SocialUserInfo getUserInfo(String accessToken) {
        AuthProperties.WechatProperties config = requireWechatConfig();
        String[] tokenParts = parseToken(accessToken);
        String userInfoUrl = config.getUserInfoUri()
                + "?access_token=" + encode(tokenParts[0])
                + "&openid=" + encode(tokenParts[1])
                + "&lang=zh_CN";
        JsonNode payload = executeJson(HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build(), "微信用户信息拉取失败");
        assertWechatSuccess(payload, "微信用户信息拉取失败");
        String openId = firstText(payload, "openid");
        String unionId = firstText(payload, "unionid");
        String providerUserId = !isBlank(unionId) ? unionId : (!isBlank(tokenParts[2]) ? tokenParts[2] : openId);
        if (isBlank(providerUserId)) {
            providerUserId = tokenParts[1];
        }
        return new SocialUserInfo(
                providerUserId,
                normalizeText(firstText(payload, "nickname")),
                normalizeText(firstText(payload, "nickname")),
                "",
                normalizeText(firstText(payload, "headimgurl")),
                PROVIDER
        );
    }

    private String[] parseToken(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\|", -1);
        if (parts.length < 2 || isBlank(parts[0]) || isBlank(parts[1])) {
            throw new BizException(502, "微信 token 格式非法");
        }
        String unionId = parts.length >= 3 ? parts[2] : "";
        return new String[]{parts[0], parts[1], unionId};
    }

    private AuthProperties.WechatProperties requireWechatConfig() {
        AuthProperties.WechatProperties config = authProperties.getSocial().getWechat();
        if (!config.isEnabled()) {
            throw new BizException(403, "微信第三方登录未启用");
        }
        if (isBlank(config.getAppId()) || isBlank(config.getAppSecret())) {
            throw new BizException(500, "微信第三方登录配置不完整");
        }
        if (isBlank(config.getAuthorizeUri()) || isBlank(config.getTokenUri()) || isBlank(config.getUserInfoUri()) || isBlank(config.getRedirectUri())) {
            throw new BizException(500, "微信 OAuth 地址配置不完整");
        }
        return config;
    }

    private void assertWechatSuccess(JsonNode payload, String fallbackMessage) {
        JsonNode errCodeNode = payload.get("errcode");
        if (errCodeNode != null && !errCodeNode.isNull() && errCodeNode.asInt() != 0) {
            String errMsg = firstText(payload, "errmsg");
            throw new BizException(502, errMsg == null ? fallbackMessage : fallbackMessage + "：" + errMsg);
        }
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
