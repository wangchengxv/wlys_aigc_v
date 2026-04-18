package com.example.aigc.entity;

import com.example.aigc.enums.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "review_record")
public class ReviewRecord {
    @Id
    @Column(name = "review_id")
    public String reviewId;

    @Column(name = "submission_id")
    public String submissionId;

    @Column(name = "assignment_id")
    public String assignmentId;

    @Column(name = "reviewer_user_id")
    public String reviewerUserId;

    @Column(name = "reviewer_user_name")
    public String reviewerUserName;

    @Enumerated(EnumType.STRING)
    public SubmissionStatus status;

    public Integer score;

    @Column(columnDefinition = "LONGTEXT")
    public String comment;

    @Column(name = "created_at")
    public Instant createdAt;
}
