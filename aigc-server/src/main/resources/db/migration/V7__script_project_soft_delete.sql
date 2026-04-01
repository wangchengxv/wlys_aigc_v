ALTER TABLE script_project
    ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_script_project_deleted_at
    ON script_project (deleted_at);
