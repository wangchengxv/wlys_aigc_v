CREATE TABLE IF NOT EXISTS org_unit (
    unit_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(128) NULL,
    unit_type VARCHAR(32) NOT NULL,
    parent_unit_id VARCHAR(64) NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_org_unit_type (unit_type),
    INDEX idx_org_unit_parent (parent_unit_id)
);

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user' AND COLUMN_NAME = 'classroom_id'
        ),
        'SELECT 1',
        'ALTER TABLE app_user ADD COLUMN classroom_id VARCHAR(64) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
