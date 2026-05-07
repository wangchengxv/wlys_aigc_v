package com.example.aigc.entity;

import com.example.aigc.enums.RevisionKind;

import java.time.Instant;

public class ScriptRevision {
    public String revisionId;
    public int revisionIndex;
    public String label;
    public RevisionKind kind;
    public Instant createdAt;
    public String refinedMarkdownFileId;
    public String refinedJsonFileId;
}
