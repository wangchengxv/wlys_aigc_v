package com.example.aigc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "canvas_graph")
public class CanvasGraph {
    @Id
    public String id;

    @Column(name = "owner_id")
    public String ownerId;

    @Column(name = "project_id")
    public String projectId;

    public String title;

    @Column(name = "graph_json", columnDefinition = "LONGTEXT")
    public String graphJson;

    @Column(name = "viewport_json", length = 512)
    public String viewportJson;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;
}
