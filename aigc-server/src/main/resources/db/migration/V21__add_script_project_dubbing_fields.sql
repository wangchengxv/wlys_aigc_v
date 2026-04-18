SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'explicit_tts_model'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN explicit_tts_model VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'dubbing_voice'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN dubbing_voice VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'dubbing_language'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN dubbing_language VARCHAR(64) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'script_project' AND COLUMN_NAME = 'dubbing_speed'
        ),
        'SELECT 1',
        'ALTER TABLE script_project ADD COLUMN dubbing_speed DOUBLE NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
