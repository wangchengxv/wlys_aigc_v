package com.example.aigc.entity;

import com.example.aigc.enums.AssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "teaching_assignment")
public class TeachingAssignment {
    @Id
    @Column(name = "assignment_id")
    public String assignmentId;

    @Column(name = "course_id")
    public String courseId;

    public String title;

    @Column(columnDefinition = "LONGTEXT")
    public String brief;

    @Column(name = "style_template_id")
    public String styleTemplateId;

    @Column(name = "aspect_ratio")
    public String aspectRatio;

    @Column(name = "target_duration")
    public Integer targetDuration;

    public String language;

    // 历史数据可为空；有值时统一按 UTC Instant 存储。
    @Column(name = "due_at")
    public Instant dueAt;

    @Column(name = "owner_id")
    public String ownerId;

    @Column(name = "owner_name")
    public String ownerName;

    @Enumerated(EnumType.STRING)
    public AssignmentStatus status = AssignmentStatus.PUBLISHED;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
