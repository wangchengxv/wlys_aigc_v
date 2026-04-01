package com.example.aigc.repository.jpa;

import com.example.aigc.model.ConnectionConfig;
import com.example.aigc.repository.ConnectionConfigRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaConnectionConfigRepository implements ConnectionConfigRepository {
    private final SpringDataConnectionConfigRepository delegate;

    public JpaConnectionConfigRepository(SpringDataConnectionConfigRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ConnectionConfig save(ConnectionConfig config) {
        return delegate.save(config);
    }

    @Override
    public Optional<ConnectionConfig> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public List<ConnectionConfig> findAll() {
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
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .toList();
    }

    @Override
    public boolean existsById(String id) {
        return delegate.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        delegate.deleteById(id);
    }
}
