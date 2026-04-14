-- 实体提示词版本历史（JSON 数组）
ALTER TABLE extracted_asset
    ADD COLUMN prompt_versions_json LONGTEXT NULL;

ALTER TABLE storyboard_shot
    ADD COLUMN prompt_versions_json LONGTEXT NULL;

ALTER TABLE keyframe_record
    ADD COLUMN prompt_versions_json LONGTEXT NULL;

-- 项目级提示词模板覆盖（classpath 路径 -> 模板正文）
ALTER TABLE script_project
    ADD COLUMN prompt_template_overrides LONGTEXT NULL;
