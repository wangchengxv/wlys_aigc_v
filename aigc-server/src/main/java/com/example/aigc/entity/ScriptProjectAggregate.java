package com.example.aigc.entity;

import java.util.ArrayList;
import java.util.List;

public class ScriptProjectAggregate {
    public ScriptProject project = new ScriptProject();
    public List<ScriptRevision> revisions = new ArrayList<>();
    public List<ScriptDocumentVersion> documents = new ArrayList<>();
    public List<StoredFileRecord> files = new ArrayList<>();
    public List<ExtractedAsset> assets = new ArrayList<>();
    public List<KeyframeRecord> keyframes = new ArrayList<>();
    public List<StoryboardShot> shots = new ArrayList<>();
    public List<VideoSegmentTask> videoTasks = new ArrayList<>();
    public List<PipelineRun> pipelineRuns = new ArrayList<>();
}
