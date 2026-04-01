package com.example.aigc.repository.jpa;

import com.example.aigc.model.RouterApiKey;
import com.example.aigc.repository.RouterApiKeyRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaRouterApiKeyRepository implements RouterApiKeyRepository {
    private final SpringDataRouterApiKeyRepository delegate;

    public JpaRouterApiKeyRepository(SpringDataRouterApiKeyRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public RouterApiKey save(RouterApiKey apiKey) {
        return delegate.save(apiKey);
    }

    @Override
    public Optional<RouterApiKey> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<RouterApiKey> findByKeyValue(String keyValue) {
        return delegate.findByKeyValue(keyValue);
    }

    @Override
    public List<RouterApiKey> findAll() {
        return delegate.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
                        return 0;
                    }
                    if (a.getCreatedAt() == null) {
                        return 1;
                    }
                    if (b.getCreatedAt() == null) {
                        return -1;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
