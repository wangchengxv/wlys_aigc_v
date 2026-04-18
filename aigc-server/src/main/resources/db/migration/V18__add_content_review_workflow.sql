ALTER TABLE script_project
    ADD COLUMN content_review_status VARCHAR(32) NULL AFTER prompt_template_overrides,
    ADD COLUMN current_review_id VARCHAR(64) NULL AFTER content_review_status,
    ADD COLUMN latest_review_comment LONGTEXT NULL AFTER current_review_id,
    ADD COLUMN review_resubmit_count INT NOT NULL DEFAULT 0 AFTER latest_review_comment,
    ADD COLUMN review_submitted_at TIMESTAMP NULL AFTER review_resubmit_count,
    ADD COLUMN reviewed_at TIMESTAMP NULL AFTER review_submitted_at,
    ADD COLUMN reviewer_user_id VARCHAR(64) NULL AFTER reviewed_at,
    ADD COLUMN reviewer_user_name VARCHAR(128) NULL AFTER reviewer_user_id;

UPDATE script_project
SET content_review_status = 'NOT_SUBMITTED'
WHERE content_review_status IS NULL;

CREATE INDEX idx_script_project_content_review_status ON script_project (content_review_status);

CREATE TABLE IF NOT EXISTS content_review_record (
    review_id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitter_user_id VARCHAR(64) NOT NULL,
    submitter_user_name VARCHAR(128) NULL,
    submission_comment LONGTEXT NULL,
    reviewer_user_id VARCHAR(64) NULL,
    reviewer_user_name VARCHAR(128) NULL,
    review_comment LONGTEXT NULL,
    resubmit_count INT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_content_review_project (project_id),
    INDEX idx_content_review_status (status),
    INDEX idx_content_review_reviewer (reviewer_user_id),
    INDEX idx_content_review_created (created_at)
);
