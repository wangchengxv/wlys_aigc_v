package com.example.aigc.entity;

import com.example.aigc.enums.ExportPackageTaskStatus;
import com.example.aigc.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "export_package_task")
public class ExportPackageTask {
    @Id
    @Column(name = "export_package_task_id")
    public String exportPackageTaskId;
    @Column(name = "project_id")
    public String projectId;
    @Column(name = "source_final_composition_task_id")
    public String sourceFinalCompositionTaskId;
    @Column(name = "source_video_edit_render_task_id")
    public String sourceVideoEditRenderTaskId;
    @Column(name = "source_video_edit_draft_version")
    public Integer sourceVideoEditDraftVersion;
    @Column(name = "source_video_origin_type")
    public String sourceVideoOriginType;
    @Column(name = "source_final_video_file_id")
    public String sourceFinalVideoFileId;
    @Column(name = "manifest_file_id")
    public String manifestFileId;
    @Column(name = "result_archive_file_id")
    public String resultArchiveFileId;
    @Enumerated(EnumType.STRING)
    public ExportPackageTaskStatus status = ExportPackageTaskStatus.PENDING;
    @Column(name = "retry_count")
    public Integer retryCount = 0;
    @Column(name = "started_at")
    public Instant startedAt;
    @Column(name = "finished_at")
    public Instant finishedAt;
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;
    @Enumerated(EnumType.STRING)
    @Column(name = "archive_storage_provider")
    public StorageProvider archiveStorageProvider;
    @Column(name = "archive_bucket_name")
    public String archiveBucketName;
    @Column(name = "archive_object_key")
    public String archiveObjectKey;
    @Column(name = "archive_public_url")
    public String archivePublicUrl;
    @Enumerated(EnumType.STRING)
    @Column(name = "manifest_storage_provider")
    public StorageProvider manifestStorageProvider;
    @Column(name = "manifest_bucket_name")
    public String manifestBucketName;
    @Column(name = "manifest_object_key")
    public String manifestObjectKey;
    @Column(name = "manifest_public_url")
    public String manifestPublicUrl;
}
