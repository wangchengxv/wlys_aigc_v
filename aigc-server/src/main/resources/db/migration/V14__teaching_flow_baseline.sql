CREATE TABLE IF NOT EXISTS teaching_course (
    course_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(128) NULL,
    description LONGTEXT NULL,
    owner_id VARCHAR(64) NOT NULL,
    owner_name VARCHAR(128) NULL,
    org_unit_id VARCHAR(64) NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_teaching_course_owner (owner_id),
    INDEX idx_teaching_course_org_unit (org_unit_id),
    INDEX idx_teaching_course_updated (updated_at)
);

CREATE TABLE IF NOT EXISTS teaching_assignment (
    assignment_id VARCHAR(64) PRIMARY KEY,
    course_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    brief LONGTEXT NULL,
    style_template_id VARCHAR(64) NULL,
    aspect_ratio VARCHAR(32) NULL,
    target_duration INT NULL,
    language VARCHAR(32) NULL,
    due_at TIMESTAMP NULL,
    owner_id VARCHAR(64) NOT NULL,
    owner_name VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_teaching_assignment_course (course_id),
    INDEX idx_teaching_assignment_owner (owner_id),
    INDEX idx_teaching_assignment_status (status),
    INDEX idx_teaching_assignment_due_at (due_at)
);

CREATE TABLE IF NOT EXISTS assignment_submission (
    submission_id VARCHAR(64) PRIMARY KEY,
    assignment_id VARCHAR(64) NOT NULL,
    course_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64) NOT NULL,
    student_user_id VARCHAR(64) NOT NULL,
    student_user_name VARCHAR(128) NULL,
    note LONGTEXT NULL,
    status VARCHAR(32) NOT NULL,
    score INT NULL,
    review_comment LONGTEXT NULL,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    INDEX idx_assignment_submission_assignment (assignment_id),
    INDEX idx_assignment_submission_course (course_id),
    INDEX idx_assignment_submission_student (student_user_id),
    INDEX idx_assignment_submission_project (project_id),
    INDEX idx_assignment_submission_status (status)
);

CREATE TABLE IF NOT EXISTS review_record (
    review_id VARCHAR(64) PRIMARY KEY,
    submission_id VARCHAR(64) NOT NULL,
    assignment_id VARCHAR(64) NOT NULL,
    reviewer_user_id VARCHAR(64) NOT NULL,
    reviewer_user_name VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    score INT NULL,
    comment LONGTEXT NULL,
    created_at TIMESTAMP NULL,
    INDEX idx_review_record_submission (submission_id),
    INDEX idx_review_record_assignment (assignment_id),
    INDEX idx_review_record_reviewer (reviewer_user_id),
    INDEX idx_review_record_created (created_at)
);
