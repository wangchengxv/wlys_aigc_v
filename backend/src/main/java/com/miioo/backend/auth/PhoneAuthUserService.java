package com.miioo.backend.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PhoneAuthUserService {
    private final AtomicLong userIdSeed = new AtomicLong(10_000);
    private final Map<String, Long> phoneUserIds = new ConcurrentHashMap<>();

    public Long getOrCreateUserId(String phone) {
        return phoneUserIds.computeIfAbsent(phone, key -> userIdSeed.getAndIncrement());
    }
}
