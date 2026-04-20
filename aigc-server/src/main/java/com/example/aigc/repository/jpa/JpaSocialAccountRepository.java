package com.example.aigc.repository.jpa;

import com.example.aigc.entity.SocialAccount;
import com.example.aigc.repository.SocialAccountRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaSocialAccountRepository implements SocialAccountRepository {
    private final SpringDataSocialAccountRepository repository;

    public JpaSocialAccountRepository(SpringDataSocialAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public SocialAccount save(SocialAccount socialAccount) {
        return repository.save(socialAccount);
    }

    @Override
    public Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return repository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public List<SocialAccount> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId);
    }

    @Override
    public Optional<SocialAccount> findByUserIdAndProvider(String userId, String provider) {
        return repository.findByUserIdAndProvider(userId, provider);
    }

    @Override
    public long countByUserId(String userId) {
        return repository.countByUserId(userId);
    }

    @Override
    public void delete(SocialAccount socialAccount) {
        repository.delete(socialAccount);
    }
}
