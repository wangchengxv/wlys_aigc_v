package com.example.aigc.repository;

import com.example.aigc.entity.AppUser;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository {
    AppUser save(AppUser user);

    Optional<AppUser> findById(String userId);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<AppUser> findAll();

    List<AppUser> findAllByIds(List<String> userIds);
}
