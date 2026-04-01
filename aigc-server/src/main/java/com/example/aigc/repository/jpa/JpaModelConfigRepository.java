package com.example.aigc.repository.jpa;

import com.example.aigc.model.ModelConfig;
import com.example.aigc.repository.ModelConfigRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaModelConfigRepository implements ModelConfigRepository {
    private final SpringDataModelConfigRepository delegate;

    public JpaModelConfigRepository(SpringDataModelConfigRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ModelConfig save(ModelConfig config) {
        return delegate.save(config);
    }

    @Override
    public Optional<ModelConfig> findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public List<ModelConfig> findAll() {
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
