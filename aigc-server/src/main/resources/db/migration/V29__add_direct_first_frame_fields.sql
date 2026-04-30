SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'storyboard_shot' AND COLUMN_NAME = 'first_frame_image_file_id'
        ),
        'SELECT 1',
        'ALTER TABLE storyboard_shot ADD COLUMN first_frame_image_file_id VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
