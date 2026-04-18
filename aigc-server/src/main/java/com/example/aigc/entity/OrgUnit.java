package com.example.aigc.entity;

import com.example.aigc.enums.OrgUnitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "org_unit")
public class OrgUnit {
    @Id
    @Column(name = "unit_id")
    public String unitId;

    @Column(nullable = false)
    public String name;

    public String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    public OrgUnitType type;

    @Column(name = "parent_unit_id")
    public String parentUnitId;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
