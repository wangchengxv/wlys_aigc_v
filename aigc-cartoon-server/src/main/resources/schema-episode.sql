-- 剧集表
CREATE TABLE IF NOT EXISTS t_episode (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '剧集ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    episode_number INT NOT NULL COMMENT '集号',
    title VARCHAR(200) COMMENT '剧集标题',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态：draft-草稿，published-已发布',
    script_summary TEXT COMMENT '剧本概要',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_project_id (project_id),
    INDEX idx_episode_number (episode_number),
    FOREIGN KEY (project_id) REFERENCES t_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='剧集表';
