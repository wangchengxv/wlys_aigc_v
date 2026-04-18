package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "entity_type")
    public String entityType;

    @Column(name = "entity_id")
    public String entityId;

    public String action;

    @Column(name = "actor_user_id")
    public String actorUserId;

    @Column(name = "actor_user_name")
    public String actorUserName;

    @Column(name = "org_unit_id")
    public String orgUnitId;

    @Column(name = "course_id")
    public String courseId;

    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    public String detailsJson;

    @Column(name = "created_at")
    public Instant createdAt;
}
