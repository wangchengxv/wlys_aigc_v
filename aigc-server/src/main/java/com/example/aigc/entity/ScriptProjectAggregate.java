package com.example.aigc.entity;

import java.util.ArrayList;
import java.util.List;

public class ScriptProjectAggregate {
    public ScriptProject project = new ScriptProject();
    public VideoEditDraft videoEditDraft;
    public List<ContentReviewRecord> contentReviewRecords = new ArrayList<>();
    public List<ScriptRevision> revisions = new ArrayList<>();
    public List<ScriptDocumentVersion> documents = new ArrayList<>();
    public List<StoredFileRecord> files = new ArrayList<>();
    public List<ExtractedAsset> assets = new ArrayList<>();
    public List<KeyframeRecord> keyframes = new ArrayList<>();
    public List<StoryboardShot> shots = new ArrayList<>();
    public List<VideoSegmentTask> videoTasks = new ArrayList<>();
    public List<DubbingTask> dubbingTasks = new ArrayList<>();
    public List<LipSyncTask> lipSyncTasks = new ArrayList<>();
    public List<VideoEditRenderTask> videoEditRenderTasks = new ArrayList<>();
    public List<FinalCompositionTask> finalCompositionTasks = new ArrayList<>();
    public List<ExportPackageTask> exportPackageTasks = new ArrayList<>();
    public List<PipelineRun> pipelineRuns = new ArrayList<>();
}
