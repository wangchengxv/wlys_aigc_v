package com.example.aigc.repository.jpa;

import com.example.aigc.entity.AppUser;
import com.example.aigc.repository.AppUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaAppUserRepository implements AppUserRepository {
    private final SpringDataAppUserRepository repository;

    public JpaAppUserRepository(SpringDataAppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public AppUser save(AppUser user) {
        return repository.save(user);
    }

    @Override
    public Optional<AppUser> findById(String userId) {
        return repository.findById(userId);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Optional<AppUser> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return repository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<AppUser> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AppUser> findAllByIds(List<String> userIds) {
        return repository.findAllByUserIdIn(userIds);
    }
}
