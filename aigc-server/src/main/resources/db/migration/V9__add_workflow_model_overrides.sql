ALTER TABLE script_project
    ADD COLUMN workflow_model_overrides LONGTEXT NULL COMMENT 'JSON map of workflow-function key -> explicit model name override';
