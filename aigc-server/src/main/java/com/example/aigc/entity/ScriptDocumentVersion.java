package com.example.aigc.entity;

import com.example.aigc.enums.DocumentVersionType;

import java.time.Instant;

public class ScriptDocumentVersion {
    public String documentId;
    public String projectId;
    public DocumentVersionType versionType;
    public String format;
    public String fileId;
    public String contentDigest;
    public Instant createdAt;
}
