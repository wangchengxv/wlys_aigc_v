package com.example.aigc.dto;

import java.time.Instant;

public record SocialLinkItemResponse(
        String provider,
        String providerUserId,
        Instant linkedAt
) {
}
