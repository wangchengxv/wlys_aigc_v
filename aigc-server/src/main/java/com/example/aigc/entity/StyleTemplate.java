package com.example.aigc.entity;

import com.example.aigc.enums.StyleTemplateScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "style_template")
public class StyleTemplate {
    @Id
    @Column(name = "template_id")
    public String templateId;

    @Enumerated(EnumType.STRING)
    public StyleTemplateScope scope = StyleTemplateScope.PERSONAL;

    public String name;
    public String category;

    @Column(columnDefinition = "TEXT")
    public String traits;

    @Column(name = "full_prompt", columnDefinition = "LONGTEXT")
    public String fullPrompt;

    @Column(name = "style_key")
    public String styleKey;

    @Column(name = "owner_id")
    public String ownerId;

    @Column(name = "owner_name")
    public String ownerName;

    @Column(name = "org_unit_id")
    public String orgUnitId;

    @Column(name = "course_id")
    public String courseId;

    public boolean enabled = true;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
