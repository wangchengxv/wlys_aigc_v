package com.example.aigc.dto;

public record SocialUserInfo(
        String id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String provider
) {
    public SocialUserInfo {
        id = trim(id);
        username = trim(username);
        displayName = trim(displayName);
        email = trim(email);
        avatarUrl = trim(avatarUrl);
        provider = trim(provider);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
