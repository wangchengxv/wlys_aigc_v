package com.example.aigc.entity;

import com.example.aigc.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stored_file_record")
public class StoredFileRecord {
    @Id
    @Column(name = "file_id")
    public String fileId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "file_name")
    public String fileName;
    @Column(name = "relative_path")
    public String relativePath;
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider")
    public StorageProvider storageProvider;
    @Column(name = "bucket_name")
    public String bucketName;
    @Column(name = "object_key")
    public String objectKey;
    @Column(name = "public_url")
    public String publicUrl;
    @Column(name = "media_type")
    public String mediaType;
    @Column(name = "size_bytes")
    public long sizeBytes;
    @Column(name = "created_at")
    public Instant createdAt;
}
