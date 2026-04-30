CREATE TABLE IF NOT EXISTS storyboard_lite_session (
    session_id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NULL,
    title VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_storyboard_lite_session_owner_created (owner_id, created_at),
    INDEX idx_storyboard_lite_session_project_created (project_id, created_at)
);

CREATE TABLE IF NOT EXISTS storyboard_lite_script (
    script_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    script_text LONGTEXT NOT NULL,
    version_no INT NOT NULL,
    created_at TIMESTAMP NULL,
    INDEX idx_storyboard_lite_script_session_version (session_id, version_no)
);

CREATE TABLE IF NOT EXISTS storyboard_lite_keyframe (
    keyframe_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    prompt_text TEXT NULL,
    image_url TEXT NULL,
    image_file_id VARCHAR(64) NULL,
    model_name VARCHAR(120) NULL,
    selected BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_storyboard_lite_keyframe_session_created (session_id, created_at)
);

CREATE TABLE IF NOT EXISTS storyboard_lite_video_task (
    video_task_id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    keyframe_id VARCHAR(64) NULL,
    prompt_text TEXT NULL,
    provider_task_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    video_url TEXT NULL,
    result_video_file_id VARCHAR(64) NULL,
    model_name VARCHAR(120) NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_storyboard_lite_video_task_session_created (session_id, created_at)
);
