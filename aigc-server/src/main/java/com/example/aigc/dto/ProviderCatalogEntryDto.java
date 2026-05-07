package com.example.aigc.dto;

import java.util.List;

/**
 * Public catalog entry for UI (add-provider wizard). Paths are not exposed.
 */
public record ProviderCatalogEntryDto(
        String key,
        String displayName,
        String defaultBaseUrl,
        String authMode,
        String apiFormat,
        String gatewayKind,
        boolean textProxySupported,
        boolean imageProxySupported,
        boolean videoProxySupported,
        List<String> staticModels
) {
}
