package com.example.aigc.repository;

import com.example.aigc.model.RouterRequestLog;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileRouterRequestLogRepository implements RouterRequestLogRepository {

    private static final String FILE_NAME = "router-request-logs.json";

    private final JsonFileStorageSupport storageSupport;
    private final List<RouterRequestLog> logs;

    public FileRouterRequestLogRepository(JsonFileStorageSupport storageSupport) {
        this.storageSupport = storageSupport;
        this.logs = new ArrayList<>(storageSupport.readValue(
                FILE_NAME,
                new TypeReference<List<RouterRequestLog>>() {
                },
                ArrayList::new
        ));
    }

    @Override
    public synchronized RouterRequestLog save(RouterRequestLog log) {
        logs.add(log);
        persist();
        return log;
    }

    @Override
    public synchronized List<RouterRequestLog> findAll() {
        return logs.stream()
                .sorted(Comparator.comparing(RouterRequestLog::getTimestamp, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
    }

    private void persist() {
        storageSupport.writeValue(FILE_NAME, findAll());
    }
}
