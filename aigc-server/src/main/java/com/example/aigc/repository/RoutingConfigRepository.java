package com.example.aigc.repository;

import com.example.aigc.model.RoutingConfig;

public interface RoutingConfigRepository {
    RoutingConfig get();

    RoutingConfig save(RoutingConfig config);
}
