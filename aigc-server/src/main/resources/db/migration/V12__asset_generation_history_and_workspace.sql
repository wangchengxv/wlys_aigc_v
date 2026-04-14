-- 工作台生成结果持久化：关联 fileId 列表（JSON 字符串列表）
ALTER TABLE generation_task
    ADD COLUMN persisted_image_file_ids_json LONGTEXT NULL AFTER video_results_json,
    ADD COLUMN persisted_video_file_ids_json LONGTEXT NULL AFTER persisted_image_file_ids_json;

-- 数字资产生成历史（重新生成前快照）
CREATE TABLE IF NOT EXISTS asset_generation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    reference_id VARCHAR(128) NULL,
    file_id VARCHAR(128) NOT NULL,
    prompt_text TEXT NULL,
    model_name VARCHAR(255) NULL,
    generation_params_json LONGTEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_hist_project (project_id),
    INDEX idx_asset_hist_ref (reference_id),
    INDEX idx_asset_hist_type (asset_type),
    INDEX idx_asset_hist_created (created_at)
);

-- 系统剧本工程：用于工作台 /generate 落盘文件（script-projects/_aigc_workspace/）
INSERT INTO script_project (
    project_id,
    name,
    status,
    source_type,
    created_at,
    updated_at
) SELECT
    '_aigc_workspace',
    '系统-工作台生成缓存',
    'DRAFT',
    'SYSTEM_WORKSPACE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM script_project WHERE project_id = '_aigc_workspace');
