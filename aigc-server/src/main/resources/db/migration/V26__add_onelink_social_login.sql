SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'provider'
        ),
        'SELECT 1',
        'ALTER TABLE app_user ADD COLUMN provider VARCHAR(64) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'provider_user_id'
        ),
        'SELECT 1',
        'ALTER TABLE app_user ADD COLUMN provider_user_id VARCHAR(128) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'linked_at'
        ),
        'SELECT 1',
        'ALTER TABLE app_user ADD COLUMN linked_at TIMESTAMP NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND INDEX_NAME = 'idx_app_user_provider_user'
        ),
        'SELECT 1',
        'CREATE INDEX idx_app_user_provider_user ON app_user(provider, provider_user_id)'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS social_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    provider_user_id VARCHAR(128) NOT NULL,
    linked_at TIMESTAMP NULL,
    UNIQUE KEY uk_social_account_provider_user (provider, provider_user_id),
    INDEX idx_social_account_user_id (user_id),
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);
