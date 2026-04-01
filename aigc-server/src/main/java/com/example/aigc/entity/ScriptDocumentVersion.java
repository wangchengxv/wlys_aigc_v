package com.example.aigc.entity;

import com.example.aigc.enums.DocumentVersionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "script_document_version")
public class ScriptDocumentVersion {
    @Id
    @Column(name = "document_id")
    public String documentId;
    @Column(name = "project_id")
    public String projectId;
    @Enumerated(EnumType.STRING)
    @Column(name = "version_type")
    public DocumentVersionType versionType;
    public String format;
    @Column(name = "file_id")
    public String fileId;
    @Column(name = "content_digest")
    public String contentDigest;
    @Column(name = "created_at")
    public Instant createdAt;
}
