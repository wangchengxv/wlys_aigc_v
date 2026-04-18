package com.example.aigc.entity;

import com.example.aigc.enums.ContentReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_review_record")
public class ContentReviewRecord {
    @Id
    @Column(name = "review_id")
    public String reviewId;

    @Column(name = "project_id")
    public String projectId;

    @Enumerated(EnumType.STRING)
    public ContentReviewStatus status = ContentReviewStatus.NOT_SUBMITTED;

    @Column(name = "submitter_user_id")
    public String submitterUserId;

    @Column(name = "submitter_user_name")
    public String submitterUserName;

    @Column(name = "submission_comment", columnDefinition = "LONGTEXT")
    public String submissionComment;

    @Column(name = "reviewer_user_id")
    public String reviewerUserId;

    @Column(name = "reviewer_user_name")
    public String reviewerUserName;

    @Column(name = "review_comment", columnDefinition = "LONGTEXT")
    public String reviewComment;

    @Column(name = "resubmit_count")
    public Integer resubmitCount = 0;

    @Column(name = "submitted_at")
    public Instant submittedAt;

    @Column(name = "reviewed_at")
    public Instant reviewedAt;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
