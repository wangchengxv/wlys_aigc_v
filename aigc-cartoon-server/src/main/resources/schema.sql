-- AIGC漫剧工具数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS aigc_cartoon DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aigc_cartoon;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(500) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '用户状态：0-禁用，1-正常',
    role VARCHAR(20) DEFAULT 'user' COMMENT '用户角色：admin-管理员，user-普通用户',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 项目表
CREATE TABLE IF NOT EXISTS t_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    cover_image VARCHAR(500) COMMENT '封面图片URL',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '项目状态：draft-草稿，published-已发布',
    style VARCHAR(50) COMMENT '项目风格',
    style_template_id VARCHAR(100) COMMENT '风格模板ID',
    visual_style_prompt TEXT COMMENT '视觉风格提示词',
    visual_style_mode VARCHAR(20) DEFAULT 'preset' COMMENT '视觉风格模式：preset/custom',
    visual_style_long_text_mode TINYINT(1) DEFAULT 0 COMMENT '视觉风格长文本模式：0-否，1-是',
    custom_style_text TEXT COMMENT '自定义风格文本',
    aspect_ratio VARCHAR(10) DEFAULT '16:9' COMMENT '画面比例',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- 兼容已存在库的增量字段（MySQL 8.0+）
ALTER TABLE t_project ADD COLUMN IF NOT EXISTS style_template_id VARCHAR(100) COMMENT '风格模板ID' AFTER style;
ALTER TABLE t_project ADD COLUMN IF NOT EXISTS visual_style_prompt TEXT COMMENT '视觉风格提示词' AFTER style_template_id;
ALTER TABLE t_project ADD COLUMN IF NOT EXISTS visual_style_mode VARCHAR(20) DEFAULT 'preset' COMMENT '视觉风格模式：preset/custom' AFTER visual_style_prompt;
ALTER TABLE t_project ADD COLUMN IF NOT EXISTS visual_style_long_text_mode TINYINT(1) DEFAULT 0 COMMENT '视觉风格长文本模式：0-否，1-是' AFTER visual_style_mode;
ALTER TABLE t_project ADD COLUMN IF NOT EXISTS custom_style_text TEXT COMMENT '自定义风格文本' AFTER visual_style_long_text_mode;

-- 项目配置表
CREATE TABLE IF NOT EXISTS t_project_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    dialog_model VARCHAR(100) DEFAULT 'GPT-5.4' COMMENT '对话模型',
    image_model VARCHAR(100) COMMENT '图片模型',
    video_model VARCHAR(100) COMMENT '视频模型',
    audio_model VARCHAR(100) COMMENT '音频模型',
    script TEXT COMMENT '剧本内容',
    episode_count INT DEFAULT 1 COMMENT '集数',
    batch_model VARCHAR(100) DEFAULT 'Doubao-Seed-2.0-Pro' COMMENT '批量生成模型',
    batch_ratio VARCHAR(10) DEFAULT '16:9' COMMENT '批量生成比例',
    batch_quality VARCHAR(20) DEFAULT '1K' COMMENT '批量生成质量',
    batch_method VARCHAR(50) DEFAULT '多视图' COMMENT '批量生成方式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_project_id (project_id),
    FOREIGN KEY (project_id) REFERENCES t_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目配置表';

-- 剧本表
CREATE TABLE IF NOT EXISTS t_script (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '剧本ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    title VARCHAR(200) NOT NULL COMMENT '剧本标题',
    content TEXT NOT NULL COMMENT '剧本内容',
    version INT DEFAULT 1 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_project_id (project_id),
    FOREIGN KEY (project_id) REFERENCES t_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='剧本表';

-- 分镜表
CREATE TABLE IF NOT EXISTS t_storyboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分镜ID',
    script_id BIGINT NOT NULL COMMENT '所属剧本ID',
    scene_number INT NOT NULL COMMENT '场景序号',
    scene_description TEXT COMMENT '场景描述',
    duration INT DEFAULT 3 COMMENT '预估时长（秒）',
    bg_prompt TEXT COMMENT '背景提示词',
    character_prompt TEXT COMMENT '角色提示词',
    audio_url VARCHAR(500) COMMENT '配音音频URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_script_id (script_id),
    FOREIGN KEY (script_id) REFERENCES t_script(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜表';

-- 角色表
CREATE TABLE IF NOT EXISTS t_character (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description TEXT COMMENT '角色描述',
    appearance_prompt TEXT COMMENT '外貌提示词',
    image_url VARCHAR(500) COMMENT '角色图片URL',
    voice_config JSON COMMENT '语音配置',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_project_id (project_id),
    FOREIGN KEY (project_id) REFERENCES t_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 视频表
CREATE TABLE IF NOT EXISTS t_video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频ID',
    project_id BIGINT NOT NULL COMMENT '所属项目ID',
    storyboard_id BIGINT COMMENT '关联分镜ID',
    video_url VARCHAR(500) COMMENT '视频URL',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    duration INT COMMENT '视频时长（秒）',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '生成状态：pending-待处理，processing-处理中，completed-已完成，failed-失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    FOREIGN KEY (project_id) REFERENCES t_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';

-- 插入默认管理员用户（密码：admin123）
INSERT IGNORE INTO t_user (username, password, nickname, email, role) VALUES 
('admin', '$2a$10$abc123hashedpasswordfortest', '管理员', 'admin@example.com', 'admin');
