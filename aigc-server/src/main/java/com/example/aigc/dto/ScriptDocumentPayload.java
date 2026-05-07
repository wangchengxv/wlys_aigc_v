package com.example.aigc.dto;

import com.example.aigc.entity.ScriptDocumentVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScriptDocumentPayload {
    public String projectId;
    public String originalText;
    public String refinedMarkdown;
    public Map<String, Object> structuredScript = new LinkedHashMap<>();
    public List<ScriptDocumentVersion> documents = new ArrayList<>();
}
