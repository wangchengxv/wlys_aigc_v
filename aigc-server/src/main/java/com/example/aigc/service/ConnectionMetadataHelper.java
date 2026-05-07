package com.example.aigc.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encrypts selected metadata values at rest and masks them for API responses.
 */
public final class ConnectionMetadataHelper {

    public static final String AZURE_API_VERSION = "apiVersion";
    public static final String AZURE_DEFAULT_API_VERSION = "2024-06-01";
    public static final String AWS_REGION = "region";
    public static final String AWS_ACCESS_KEY_ID = "awsAccessKeyId";
    public static final String AWS_SESSION_TOKEN = "awsSessionToken";
    public static final String VERTEX_PROJECT = "vertexProjectId";
    public static final String VERTEX_LOCATION = "vertexLocation";
    public static final String VERTEX_SA_JSON = "vertexServiceAccountJson";
    public static final String LM_STUDIO_HINT = "lmStudio";
    /** JSON array of {"name","value"} for outbound HTTP headers. */
    public static final String CUSTOM_HEADERS_JSON = "customHeadersJson";
    /** JSON object of query param name → value appended to gateway URLs. */
    public static final String CUSTOM_QUERY_PARAMS_JSON = "customQueryParamsJson";
    /** Newline-separated additional API keys (rotation). */
    public static final String EXTRA_API_KEYS = "extraApiKeys";

    private static final Set<String> ENCRYPTED_KEYS = Set.of(
            VERTEX_SA_JSON,
            AWS_SESSION_TOKEN,
            CUSTOM_HEADERS_JSON,
            CUSTOM_QUERY_PARAMS_JSON,
            EXTRA_API_KEYS
    );

    private ConnectionMetadataHelper() {
    }

    public static Map<String, Object> normalizeIncoming(Map<String, Object> raw, ApiKeyCryptoService crypto) {
        if (raw == null || raw.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            if (ENCRYPTED_KEYS.contains(k)) {
                String plain = String.valueOf(v).trim();
                if (plain.isEmpty()) {
                    continue;
                }
                if (plain.startsWith("__ENC__")) {
                    out.put(k, plain);
                } else {
                    out.put(k, "__ENC__" + crypto.encrypt(plain));
                }
            } else {
                out.put(k, v instanceof String ? ((String) v).trim() : v);
            }
        }
        return out;
    }

    public static String decryptValue(String stored, ApiKeyCryptoService crypto) {
        if (stored == null || stored.isBlank()) {
            return "";
        }
        if (stored.startsWith("__ENC__")) {
            return crypto.decrypt(stored.substring("__ENC__".length()));
        }
        return stored;
    }

    public static Map<String, Object> decryptForUse(Map<String, Object> stored, ApiKeyCryptoService crypto) {
        if (stored == null || stored.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : stored.entrySet()) {
            if (!ENCRYPTED_KEYS.contains(e.getKey())) {
                out.put(e.getKey(), e.getValue());
                continue;
            }
            String val = e.getValue() == null ? "" : String.valueOf(e.getValue());
            out.put(e.getKey(), decryptValue(val, crypto));
        }
        return out;
    }

    public static Map<String, Object> maskForResponse(Map<String, Object> stored) {
        if (stored == null || stored.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : stored.entrySet()) {
            if (ENCRYPTED_KEYS.contains(e.getKey()) && e.getValue() != null && !String.valueOf(e.getValue()).isBlank()) {
                out.put(e.getKey(), "********");
            } else {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    public static Map<String, Object> merge(Map<String, Object> existing, Map<String, Object> patch, ApiKeyCryptoService crypto) {
        Map<String, Object> base = existing == null ? new HashMap<>() : new HashMap<>(existing);
        if (patch == null) {
            return base;
        }
        for (Map.Entry<String, Object> e : patch.entrySet()) {
            String k = e.getKey();
            if (e.getValue() == null) {
                base.remove(k);
                continue;
            }
            if (ENCRYPTED_KEYS.contains(k)) {
                String plain = String.valueOf(e.getValue()).trim();
                if (plain.isEmpty() || "********".equals(plain)) {
                    continue;
                }
                if (plain.startsWith("__ENC__")) {
                    base.put(k, plain);
                } else {
                    base.put(k, "__ENC__" + crypto.encrypt(plain));
                }
            } else {
                base.put(k, e.getValue());
            }
        }
        return base;
    }
}
