package model

// ScriptProjectAggregate mirrors Java ScriptProjectAggregate
type ScriptProjectAggregate struct {
	Project      *ScriptProject
	Revisions    []ScriptRevision
	Documents    []ScriptDocumentVersion
	Files        []StoredFileRecord
	Assets       []ExtractedAsset
	Keyframes    []KeyframeRecord
	Shots        []StoryboardShot
	VideoTasks   []VideoSegmentTask
	PipelineRuns []PipelineRun
}
