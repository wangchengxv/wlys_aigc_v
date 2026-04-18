CREATE TABLE IF NOT EXISTS video_edit_draft (
    project_id VARCHAR(64) PRIMARY KEY,
    draft_id VARCHAR(64) NULL,
    version_no INT NULL,
    segments_json LONGTEXT NULL,
    extensions_json LONGTEXT NULL,
    published_version INT NULL,
    published_at TIMESTAMP NULL,
    published_render_task_id VARCHAR(64) NULL,
    latest_preview_task_id VARCHAR(64) NULL,
    latest_publish_task_id VARCHAR(64) NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS video_edit_render_task (
    render_task_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    draft_version INT NULL,
    task_type VARCHAR(32) NULL,
    input_segments_json LONGTEXT NULL,
    request_payload_file_id VARCHAR(64) NULL,
    result_video_file_id VARCHAR(64) NULL,
    provider_task_id VARCHAR(128) NULL,
    model_name VARCHAR(255) NULL,
    status VARCHAR(64) NULL,
    retry_count INT NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_message TEXT NULL,
    INDEX idx_video_edit_render_task_project_id (project_id),
    INDEX idx_video_edit_render_task_created_at (created_at)
);

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'export_package_task' AND COLUMN_NAME = 'source_video_edit_render_task_id'
        ),
        'SELECT 1',
        'ALTER TABLE export_package_task ADD COLUMN source_video_edit_render_task_id VARCHAR(64) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'export_package_task' AND COLUMN_NAME = 'source_video_edit_draft_version'
        ),
        'SELECT 1',
        'ALTER TABLE export_package_task ADD COLUMN source_video_edit_draft_version INT NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'export_package_task' AND COLUMN_NAME = 'source_video_origin_type'
        ),
        'SELECT 1',
        'ALTER TABLE export_package_task ADD COLUMN source_video_origin_type VARCHAR(64) NULL'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
