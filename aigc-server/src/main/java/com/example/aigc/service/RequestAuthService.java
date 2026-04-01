package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.exception.BizException;
import org.springframework.stereotype.Service;

@Service
public class RequestAuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;

    public RequestAuthService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String requireUserId(String authorization, String xAigcToken, String xUserId) {
        requireAuthorized(authorization, xAigcToken);
        String userId = xUserId == null ? "" : xUserId.trim();
        if (authProperties.isUserIdRequired() && userId.isBlank()) {
            throw new BizException(401, "缺少用户标识，请设置 x-user-id");
        }
        return userId.isBlank() ? "anonymous" : userId;
    }

    public void requireAuthorized(String authorization, String xAigcToken) {
        String expected = authProperties.getAccessToken() == null ? "" : authProperties.getAccessToken().trim();
        if (expected.isBlank()) {
            throw new BizException(500, "服务未配置访问令牌，请联系管理员");
        }
        String token = extractToken(authorization, xAigcToken);
        if (!expected.equals(token)) {
            throw new BizException(401, "未授权访问");
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
}
