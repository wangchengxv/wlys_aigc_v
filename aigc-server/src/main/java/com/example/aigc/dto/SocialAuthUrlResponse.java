package com.example.aigc.dto;

public record SocialAuthUrlResponse(
        String provider,
        String authUrl
) {
}
