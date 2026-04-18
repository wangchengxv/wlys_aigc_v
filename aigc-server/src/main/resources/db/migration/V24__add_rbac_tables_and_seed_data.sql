CREATE TABLE IF NOT EXISTS auth_role (
    role_id VARCHAR(64) PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    UNIQUE KEY uk_auth_role_code (role_code)
);

CREATE TABLE IF NOT EXISTS auth_permission (
    permission_id VARCHAR(64) PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    UNIQUE KEY uk_auth_permission_code (permission_code)
);

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_auth_role_permission_role FOREIGN KEY (role_id) REFERENCES auth_role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_role_permission_permission FOREIGN KEY (permission_id) REFERENCES auth_permission (permission_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_auth_user_role_role_id (role_id),
    CONSTRAINT fk_auth_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_user_role_role FOREIGN KEY (role_id) REFERENCES auth_role (role_id) ON DELETE CASCADE
);

INSERT IGNORE INTO auth_role (role_id, role_code, role_name, enabled, created_at, updated_at) VALUES
    ('role-admin', 'ADMIN', '平台管理员', TRUE, NOW(), NOW()),
    ('role-teacher', 'TEACHER', '教师', TRUE, NOW(), NOW()),
    ('role-student', 'STUDENT', '学生', TRUE, NOW(), NOW());

INSERT IGNORE INTO auth_permission (permission_id, permission_code, permission_name, enabled, created_at, updated_at) VALUES
    ('perm-all', '*', '全部权限', TRUE, NOW(), NOW()),
    ('perm-menu-account-directory', 'menu:account:directory:view', '查看账号目录菜单', TRUE, NOW(), NOW()),
    ('perm-api-org-unit-list', 'api:account:org-unit:list', '查询组织列表', TRUE, NOW(), NOW()),
    ('perm-api-org-unit-create', 'api:account:org-unit:create', '创建组织', TRUE, NOW(), NOW()),
    ('perm-api-user-list', 'api:account:user:list', '查询用户列表', TRUE, NOW(), NOW()),
    ('perm-api-user-create', 'api:account:user:create', '创建用户', TRUE, NOW(), NOW()),
    ('perm-api-user-profile-update', 'api:account:user:profile:update', '更新用户资料', TRUE, NOW(), NOW()),
    ('perm-api-user-status-update', 'api:account:user:status:update', '更新用户状态', TRUE, NOW(), NOW()),
    ('perm-api-user-password-reset', 'api:account:user:password:reset', '重置用户密码', TRUE, NOW(), NOW()),
    ('perm-api-user-role-update', 'api:account:user:role:update', '更新用户角色', TRUE, NOW(), NOW()),
    ('perm-api-user-lock-update', 'api:account:user:lock:update', '锁定或解锁用户', TRUE, NOW(), NOW()),
    ('perm-api-user-force-logout', 'api:account:user:force-logout', '强制用户下线', TRUE, NOW(), NOW()),
    ('perm-api-user-batch-status', 'api:account:user:batch:status', '批量更新用户状态', TRUE, NOW(), NOW()),
    ('perm-api-user-batch-lock', 'api:account:user:batch:lock', '批量锁定用户', TRUE, NOW(), NOW()),
    ('perm-api-user-batch-role', 'api:account:user:batch:role', '批量更新用户角色', TRUE, NOW(), NOW()),
    ('perm-api-user-import-template', 'api:account:user:import-template:download', '下载用户导入模板', TRUE, NOW(), NOW()),
    ('perm-api-user-import', 'api:account:user:import', '导入用户', TRUE, NOW(), NOW()),
    ('perm-api-user-import-task-query', 'api:account:user:import-task:query', '查询导入任务', TRUE, NOW(), NOW()),
    ('perm-api-user-export', 'api:account:user:export', '导出用户', TRUE, NOW(), NOW()),
    ('perm-api-user-batch-stats', 'api:account:user:batch:stats', '查询批量任务统计', TRUE, NOW(), NOW());

INSERT IGNORE INTO auth_role_permission (role_id, permission_id, created_at)
SELECT 'role-admin', 'perm-all', NOW()
FROM DUAL;

INSERT IGNORE INTO auth_role_permission (role_id, permission_id, created_at)
SELECT 'role-teacher', p.permission_id, NOW()
FROM auth_permission p
WHERE p.permission_code IN (
    'menu:account:directory:view',
    'api:account:org-unit:list',
    'api:account:user:list',
    'api:account:user:import-template:download',
    'api:account:user:import-task:query',
    'api:account:user:export',
    'api:account:user:batch:stats'
);

INSERT IGNORE INTO auth_user_role (user_id, role_id, created_at)
SELECT u.user_id, CONCAT('role-', LOWER(u.role)), NOW()
FROM app_user u
WHERE u.role IN ('ADMIN', 'TEACHER', 'STUDENT');
