ALTER TABLE generation_task
    ADD COLUMN owner_id VARCHAR(128) NULL;

UPDATE generation_task
SET owner_id = 'legacy'
WHERE owner_id IS NULL OR owner_id = '';

ALTER TABLE generation_task
    MODIFY owner_id VARCHAR(128) NOT NULL;

CREATE INDEX idx_generation_task_owner_created_at
    ON generation_task (owner_id, created_at);
