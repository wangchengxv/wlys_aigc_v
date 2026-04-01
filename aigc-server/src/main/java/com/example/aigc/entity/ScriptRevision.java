package com.example.aigc.entity;

import com.example.aigc.enums.RevisionKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "script_revision")
public class ScriptRevision {
    @Id
    @Column(name = "revision_id")
    public String revisionId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "revision_index")
    public int revisionIndex;
    public String label;
    @Enumerated(EnumType.STRING)
    public RevisionKind kind;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "refined_markdown_file_id")
    public String refinedMarkdownFileId;
    @Column(name = "refined_json_file_id")
    public String refinedJsonFileId;
}
