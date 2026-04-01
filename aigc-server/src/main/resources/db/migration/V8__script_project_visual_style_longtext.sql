-- 剧本工程 visual_style 需容纳自由描述 / 全局设定「长文本模式」整段提示词（原 VARCHAR(255) 不足）
ALTER TABLE script_project MODIFY COLUMN visual_style LONGTEXT;
