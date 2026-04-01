CREATE TABLE IF NOT EXISTS connection_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(128),
    base_url VARCHAR(1024),
    encrypted_api_key TEXT,
    metadata_json LONGTEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS model_config (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(128),
    model_name VARCHAR(255),
    connection_id VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json LONGTEXT,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_model_config_provider (provider),
    INDEX idx_model_config_connection_id (connection_id)
);

CREATE TABLE IF NOT EXISTS generation_task (
    task_id VARCHAR(64) PRIMARY KEY,
    prompt LONGTEXT,
    mode VARCHAR(32),
    style VARCHAR(128),
    image_model VARCHAR(255),
    video_model VARCHAR(255),
    image_model_source VARCHAR(255),
    video_model_source VARCHAR(255),
    image_model_matched_by VARCHAR(255),
    video_model_matched_by VARCHAR(255),
    image_model_reject_reason VARCHAR(255),
    video_model_reject_reason VARCHAR(255),
    status VARCHAR(32),
    latency_ms BIGINT,
    error_code VARCHAR(128),
    created_at DATETIME NULL,
    text_results_json LONGTEXT,
    image_results_json LONGTEXT,
    video_results_json LONGTEXT,
    INDEX idx_generation_task_mode_created_at (mode, created_at),
    INDEX idx_generation_task_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS router_api_key (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key_value VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    UNIQUE KEY uk_router_api_key_value (key_value),
    INDEX idx_router_api_key_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS routing_config (
    id BIGINT PRIMARY KEY,
    strategy VARCHAR(128),
    priority_connection_ids_json LONGTEXT,
    failover_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    failover_timeout_seconds INT NOT NULL DEFAULT 10,
    time_schedule_json LONGTEXT
);

CREATE TABLE IF NOT EXISTS router_request_log (
    id VARCHAR(64) PRIMARY KEY,
    timestamp TIMESTAMP NULL,
    router_api_key_id VARCHAR(64),
    connection_id VARCHAR(64),
    connection_name VARCHAR(255),
    provider VARCHAR(128),
    model VARCHAR(255),
    request_format VARCHAR(64),
    status VARCHAR(64),
    duration_ms INT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    error_message TEXT,
    INDEX idx_router_request_log_timestamp (timestamp),
    INDEX idx_router_request_log_provider (provider),
    INDEX idx_router_request_log_connection_id (connection_id)
);

CREATE TABLE IF NOT EXISTS script_project (
    project_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(64),
    source_type VARCHAR(64),
    original_script_file_id VARCHAR(64),
    refined_script_file_id VARCHAR(64),
    refined_script_json_file_id VARCHAR(64),
    uploaded_source_file_id VARCHAR(64),
    script_summary TEXT,
    visual_style VARCHAR(255),
    aspect_ratio VARCHAR(64),
    target_duration INT,
    language VARCHAR(64),
    explicit_text_model VARCHAR(255),
    explicit_image_model VARCHAR(255),
    explicit_video_model VARCHAR(255),
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_script_project_updated_at (updated_at),
    INDEX idx_script_project_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS script_revision (
    revision_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    revision_index INT NOT NULL,
    label VARCHAR(255),
    kind VARCHAR(64),
    created_at TIMESTAMP NULL,
    refined_markdown_file_id VARCHAR(64),
    refined_json_file_id VARCHAR(64),
    INDEX idx_script_revision_project_id (project_id),
    INDEX idx_script_revision_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS script_document_version (
    document_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    version_type VARCHAR(64),
    format VARCHAR(64),
    file_id VARCHAR(64),
    content_digest VARCHAR(255),
    created_at TIMESTAMP NULL,
    INDEX idx_script_document_project_id (project_id),
    INDEX idx_script_document_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS stored_file_record (
    file_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    file_name VARCHAR(255),
    relative_path VARCHAR(1024),
    media_type VARCHAR(255),
    size_bytes BIGINT,
    created_at TIMESTAMP NULL,
    INDEX idx_stored_file_project_id (project_id),
    INDEX idx_stored_file_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS extracted_asset (
    asset_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    asset_type VARCHAR(64),
    name VARCHAR(255),
    description TEXT,
    source_shot_id VARCHAR(64),
    tags_json LONGTEXT,
    prompt_draft TEXT,
    status VARCHAR(64),
    metadata_json LONGTEXT,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_extracted_asset_project_id (project_id),
    INDEX idx_extracted_asset_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS keyframe_record (
    keyframe_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    asset_id VARCHAR(64),
    shot_id VARCHAR(64),
    prompt_text TEXT,
    negative_prompt TEXT,
    image_file_id VARCHAR(64),
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(64),
    provider_task_id VARCHAR(128),
    model_name VARCHAR(255),
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_keyframe_project_id (project_id),
    INDEX idx_keyframe_asset_id (asset_id),
    INDEX idx_keyframe_shot_id (shot_id)
);

CREATE TABLE IF NOT EXISTS storyboard_shot (
    shot_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    parent_shot_id VARCHAR(64),
    sequence_no INT,
    title VARCHAR(255),
    script_text LONGTEXT,
    action_summary TEXT,
    camera_movement VARCHAR(255),
    target_duration_sec INT,
    character_refs_json LONGTEXT,
    background_refs_json LONGTEXT,
    prop_refs_json LONGTEXT,
    keyframe_refs_json LONGTEXT,
    status VARCHAR(64),
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_storyboard_shot_project_id (project_id),
    INDEX idx_storyboard_shot_sequence_no (sequence_no)
);

CREATE TABLE IF NOT EXISTS video_segment_task (
    segment_task_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    shot_id VARCHAR(64),
    request_payload_file_id VARCHAR(64),
    result_video_file_id VARCHAR(64),
    provider_task_id VARCHAR(128),
    status VARCHAR(64),
    retry_count INT,
    model_name VARCHAR(255),
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_message TEXT,
    INDEX idx_video_segment_project_id (project_id),
    INDEX idx_video_segment_shot_id (shot_id),
    INDEX idx_video_segment_started_at (started_at)
);

CREATE TABLE IF NOT EXISTS pipeline_run (
    pipeline_run_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    pipeline_type VARCHAR(64),
    status VARCHAR(64),
    current_stage VARCHAR(255),
    total_count INT,
    success_count INT,
    failed_count INT,
    error_message TEXT,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_pipeline_run_project_id (project_id),
    INDEX idx_pipeline_run_created_at (created_at)
);
