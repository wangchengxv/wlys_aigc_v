package com.example.aigc.repository;

import com.example.aigc.entity.SocialAccount;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository {
    SocialAccount save(SocialAccount socialAccount);

    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<SocialAccount> findAllByUserId(String userId);

    Optional<SocialAccount> findByUserIdAndProvider(String userId, String provider);

    long countByUserId(String userId);

    void delete(SocialAccount socialAccount);
}
