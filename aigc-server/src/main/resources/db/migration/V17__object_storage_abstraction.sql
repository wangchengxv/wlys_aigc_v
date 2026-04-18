SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stored_file_record' AND COLUMN_NAME = 'storage_provider'
        ),
        'SELECT 1',
        'ALTER TABLE stored_file_record ADD COLUMN storage_provider VARCHAR(32) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stored_file_record' AND COLUMN_NAME = 'bucket_name'
        ),
        'SELECT 1',
        'ALTER TABLE stored_file_record ADD COLUMN bucket_name VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stored_file_record' AND COLUMN_NAME = 'object_key'
        ),
        'SELECT 1',
        'ALTER TABLE stored_file_record ADD COLUMN object_key VARCHAR(1024) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stored_file_record' AND COLUMN_NAME = 'public_url'
        ),
        'SELECT 1',
        'ALTER TABLE stored_file_record ADD COLUMN public_url VARCHAR(1024) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE stored_file_record
SET storage_provider = COALESCE(storage_provider, 'LOCAL'),
    bucket_name = COALESCE(bucket_name, 'aigc-local'),
    object_key = COALESCE(object_key, relative_path),
    public_url = COALESCE(public_url, CONCAT('/api/v1/files/', file_id))
WHERE storage_provider IS NULL
   OR bucket_name IS NULL
   OR object_key IS NULL
   OR public_url IS NULL;
