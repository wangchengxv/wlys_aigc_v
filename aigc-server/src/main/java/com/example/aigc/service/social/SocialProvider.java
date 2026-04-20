package com.example.aigc.service.social;

import com.example.aigc.dto.SocialUserInfo;

public interface SocialProvider {
    String provider();

    String getAuthUrl(String state);

    String exchangeCodeForToken(String code);

    SocialUserInfo getUserInfo(String accessToken);
}
