package com.example.aigc.repository;

import com.example.aigc.model.RouterRequestLog;

import java.util.List;

public interface RouterRequestLogRepository {
    RouterRequestLog save(RouterRequestLog log);

    List<RouterRequestLog> findAll();
}
