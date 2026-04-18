package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "account_import_task")
public class AccountImportTask {
    @Id
    @Column(name = "task_id")
    public String taskId;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "source_file_name")
    public String sourceFileName;

    @Column(name = "operator_user_id")
    public String operatorUserId;

    @Column(name = "operator_user_name")
    public String operatorUserName;

    @Column(name = "total_rows", nullable = false)
    public int totalRows;

    @Column(name = "success_rows", nullable = false)
    public int successRows;

    @Column(name = "failed_rows", nullable = false)
    public int failedRows;

    @Column(name = "error_details_json", columnDefinition = "LONGTEXT")
    public String errorDetailsJson;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "finished_at")
    public Instant finishedAt;
}
