CREATE TABLE IF NOT EXISTS account_import_task (
    task_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    source_file_name VARCHAR(255) NULL,
    operator_user_id VARCHAR(64) NULL,
    operator_user_name VARCHAR(128) NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    error_details_json LONGTEXT NULL,
    created_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    INDEX idx_account_import_task_created_at (created_at),
    INDEX idx_account_import_task_operator (operator_user_id)
);
