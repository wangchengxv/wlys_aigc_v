package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "teaching_course")
public class TeachingCourse {
    @Id
    @Column(name = "course_id")
    public String courseId;

    public String name;
    public String code;

    @Column(columnDefinition = "LONGTEXT")
    public String description;

    @Column(name = "owner_id")
    public String ownerId;

    @Column(name = "owner_name")
    public String ownerName;

    @Column(name = "org_unit_id")
    public String orgUnitId;

    public boolean archived;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
