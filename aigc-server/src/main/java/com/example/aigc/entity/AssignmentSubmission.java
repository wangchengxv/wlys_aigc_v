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
@Table(name = "assignment_submission")
public class AssignmentSubmission {
    @Id
    @Column(name = "submission_id")
    public String submissionId;

    @Column(name = "assignment_id")
    public String assignmentId;

    @Column(name = "course_id")
    public String courseId;

    @Column(name = "project_id")
    public String projectId;

    @Column(name = "student_user_id")
    public String studentUserId;

    @Column(name = "student_user_name")
    public String studentUserName;

    @Column(columnDefinition = "LONGTEXT")
    public String note;

    @Enumerated(EnumType.STRING)
    public SubmissionStatus status = SubmissionStatus.SUBMITTED;

    public Integer score;

    @Column(name = "review_comment", columnDefinition = "LONGTEXT")
    public String reviewComment;

    @Column(name = "submitted_at")
    public Instant submittedAt;

    @Column(name = "reviewed_at")
    public Instant reviewedAt;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
