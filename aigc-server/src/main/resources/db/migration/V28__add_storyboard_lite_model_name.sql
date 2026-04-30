SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'storyboard_lite_keyframe' AND COLUMN_NAME = 'model_name'
        ),
        'SELECT 1',
        'ALTER TABLE storyboard_lite_keyframe ADD COLUMN model_name VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
