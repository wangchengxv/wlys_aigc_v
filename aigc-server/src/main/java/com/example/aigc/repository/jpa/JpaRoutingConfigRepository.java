package com.example.aigc.repository.jpa;

import com.example.aigc.model.RoutingConfig;
import com.example.aigc.repository.RoutingConfigRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class JpaRoutingConfigRepository implements RoutingConfigRepository {
    private static final long SINGLE_ID = 1L;
    private final SpringDataRoutingConfigRepository delegate;

    public JpaRoutingConfigRepository(SpringDataRoutingConfigRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public RoutingConfig get() {
        return delegate.findById(SINGLE_ID).orElseGet(RoutingConfig::createDefault);
    }

    @Override
    public RoutingConfig save(RoutingConfig config) {
        RoutingConfig target = config == null ? RoutingConfig.createDefault() : config;
        target.setId(SINGLE_ID);
        return delegate.save(target);
    }
}
