CREATE TABLE IF NOT EXISTS app_user (
    user_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NULL,
    role VARCHAR(32) NOT NULL,
    org_unit_id VARCHAR(64) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    UNIQUE KEY uk_app_user_username (username),
    INDEX idx_app_user_role (role),
    INDEX idx_app_user_org_unit (org_unit_id)
);
