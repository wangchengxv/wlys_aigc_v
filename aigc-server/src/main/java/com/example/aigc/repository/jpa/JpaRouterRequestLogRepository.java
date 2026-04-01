package com.example.aigc.repository.jpa;

import com.example.aigc.model.RouterRequestLog;
import com.example.aigc.repository.RouterRequestLogRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class JpaRouterRequestLogRepository implements RouterRequestLogRepository {
    private final SpringDataRouterRequestLogRepository delegate;

    public JpaRouterRequestLogRepository(SpringDataRouterRequestLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public RouterRequestLog save(RouterRequestLog log) {
        return delegate.save(log);
    }

    @Override
    public List<RouterRequestLog> findAll() {
        return delegate.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) {
                        return 0;
                    }
                    if (a.getTimestamp() == null) {
                        return 1;
                    }
                    if (b.getTimestamp() == null) {
                        return -1;
                    }
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .toList();
    }
}
