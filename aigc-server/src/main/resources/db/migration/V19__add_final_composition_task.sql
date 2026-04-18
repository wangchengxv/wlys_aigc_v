CREATE TABLE IF NOT EXISTS final_composition_task (
    final_composition_task_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    input_segments_json LONGTEXT,
    request_payload_file_id VARCHAR(64),
    result_video_file_id VARCHAR(64),
    provider_task_id VARCHAR(128),
    model_name VARCHAR(255),
    status VARCHAR(64),
    retry_count INT,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_message TEXT,
    INDEX idx_final_composition_project_id (project_id),
    INDEX idx_final_composition_started_at (started_at)
);
