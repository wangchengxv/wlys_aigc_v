CREATE UNIQUE INDEX uk_project_user_name ON project (user_id, name);
CREATE INDEX idx_script_project ON script (project_id);
CREATE INDEX idx_subject_project ON subject (project_id);
CREATE INDEX idx_asset_project ON asset (project_id);
CREATE INDEX idx_ai_task_project_status ON ai_task (project_id, status);
