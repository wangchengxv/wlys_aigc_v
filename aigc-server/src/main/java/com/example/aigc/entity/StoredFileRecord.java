package com.example.aigc.entity;

import java.time.Instant;

public class StoredFileRecord {
    public String fileId;
    public String projectId;
    public String fileName;
    public String relativePath;
    public String mediaType;
    public long sizeBytes;
    public Instant createdAt;
}
