package com.example.aigc.repository;

import java.util.Set;

public interface UserPermissionRepository {
    Set<String> findPermissionCodesByUserId(String userId);
}
