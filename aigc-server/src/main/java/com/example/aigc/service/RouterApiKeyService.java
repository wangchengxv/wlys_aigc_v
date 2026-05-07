package com.example.aigc.service;

import com.example.aigc.dto.RouterApiKeyResponse;
import com.example.aigc.exception.BizException;
import com.example.aigc.exception.ErrorCode;
import com.example.aigc.model.RouterApiKey;
import com.example.aigc.repository.RouterApiKeyRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class RouterApiKeyService {

    private static final String KEY_PREFIX = "lmr-";
    private static final int KEY_RANDOM_LENGTH = 32;

    private final RouterApiKeyRepository repository;

    public RouterApiKeyService(RouterApiKeyRepository repository) {
        this.repository = repository;
    }

    public RouterApiKeyResponse create(String name) {
        RouterApiKey apiKey = RouterApiKey.create(name, generateApiKey());
        repository.save(apiKey);
        return toResponse(apiKey, true);
    }

    public List<RouterApiKeyResponse> list() {
        return repository.findAll().stream().map(apiKey -> toResponse(apiKey, false)).toList();
    }

    public void delete(String id) {
        repository.findById(id).orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "路由 API Key 不存在"));
        repository.deleteById(id);
    }

    public RouterApiKeyResponse toggle(String id, boolean active) {
        RouterApiKey apiKey = repository.findById(id).orElseThrow(() -> new BizException(404, ErrorCode.NOT_FOUND, "路由 API Key 不存在"));
        apiKey.setActive(active);
        repository.save(apiKey);
        return toResponse(apiKey, false);
    }

    public RouterApiKey requireActive(String authorization, String xApiKey) {
        String presentedKey = extractPresentedKey(authorization, xApiKey);
        RouterApiKey apiKey = repository.findByKeyValue(presentedKey)
                .orElseThrow(() -> new BizException(401, ErrorCode.BAD_REQUEST, "无效的路由 API Key"));
        if (!apiKey.isActive()) {
            throw new BizException(401, ErrorCode.BAD_REQUEST, "路由 API Key 已被禁用");
        }
        apiKey.touchLastUsedAt();
        repository.save(apiKey);
        return apiKey;
    }

    public String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        if (key.length() <= 12) {
            return "****";
        }
        return key.substring(0, 7) + "****" + key.substring(key.length() - 4);
    }

    public RouterApiKeyResponse toResponse(RouterApiKey apiKey, boolean includeFullKey) {
        return new RouterApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                includeFullKey ? apiKey.getKeyValue() : null,
                maskKey(apiKey.getKeyValue()),
                apiKey.isActive(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt()
        );
    }

    private String extractPresentedKey(String authorization, String xApiKey) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        if (xApiKey != null && !xApiKey.isBlank()) {
            return xApiKey.trim();
        }
        throw new BizException(401, ErrorCode.BAD_REQUEST, "缺少 Authorization 或 x-api-key");
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (randomPart.length() > KEY_RANDOM_LENGTH) {
            randomPart = randomPart.substring(0, KEY_RANDOM_LENGTH);
        }
        return KEY_PREFIX + randomPart;
    }
}
