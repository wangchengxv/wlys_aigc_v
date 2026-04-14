CREATE TABLE IF NOT EXISTS canvas_graph (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    project_id VARCHAR(64),
    title VARCHAR(255),
    graph_json LONGTEXT NOT NULL,
    viewport_json VARCHAR(512),
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    INDEX idx_canvas_graph_owner (owner_id),
    INDEX idx_canvas_graph_project (project_id)
);
