ALTER TABLE ai_task
  ADD COLUMN IF NOT EXISTS user_id BIGINT NOT NULL DEFAULT 1 AFTER id;

CREATE INDEX idx_ai_task_user ON ai_task (user_id);
