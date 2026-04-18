package com.example.aigc.repository.jpa;

import com.example.aigc.repository.UserPermissionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
@Primary
public class JpaUserPermissionRepository implements UserPermissionRepository {
    private static final String FIND_PERMISSION_CODES_SQL = """
            SELECT DISTINCT p.permission_code
            FROM auth_user_role ur
            JOIN auth_role r ON r.role_id = ur.role_id
            JOIN auth_role_permission rp ON rp.role_id = r.role_id
            JOIN auth_permission p ON p.permission_id = rp.permission_id
            WHERE ur.user_id = :userId
              AND r.enabled = TRUE
              AND p.enabled = TRUE
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JpaUserPermissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Set<String> findPermissionCodesByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        List<String> rows;
        try {
            rows = jdbcTemplate.queryForList(
                    FIND_PERMISSION_CODES_SQL,
                    new MapSqlParameterSource("userId", userId.trim()),
                    String.class
            );
        } catch (DataAccessException ex) {
            return Set.of();
        }
        if (rows.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new LinkedHashSet<>(rows));
    }
}
