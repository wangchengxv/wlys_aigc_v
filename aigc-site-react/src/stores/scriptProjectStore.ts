import { create } from 'zustand'
import {
  applyStoryboardFirstFrame,
  applyRewriteScriptProject,
  approveScriptProjectContentReview,
  appendScriptProjectPreview,
  confirmProjectKeyframe,
  createScriptProject,
  deleteScriptProject,
  extractScriptAssets,
  generateArtDirection,
  generateAssetKeyframes,
  generateAssetVisualPrompt,
  generateScriptProjectDubbing,
  generateScriptProjectFinalComposition,
  generateScriptProjectLipSync,
  generateGroupScene,
  generateThreeView,
  generateScriptProjectExportPackage,
  generateShotVisualPrompt,
  generateStoryboardImage,
  generateStoryboardPlan,
  cropStoryboardPanel,
  rewriteStoryboardPlan,
  translateStoryboardPlan,
  generateTurnaroundImage,
  generateTurnaroundPlan,
  batchGenerateCharacterVisualPrompts,
  generateScriptProjectVideos,
  getProjectKeyframes,
  getScriptAssets,
  getScriptProjectContentReviewStatus,
  getScriptProjectDubbingTasks,
  getScriptProjectExportPackageTasks,
  getScriptProjectFinalCompositionTasks,
  getScriptProject,
  getScriptProjectVideoEditingDraft,
  getScriptProjectVideoEditingRenderTasks,
  getScriptProjectDocument,
  getScriptProjectPipelineStatus,
  getScriptProjectShots,
  getScriptProjectVideoTasks,
  getScriptProjectLipSyncTasks,
  importScriptToProject,
  listScriptProjects,
  listScriptRevisions,
  optimizeScriptCharacters,
  optimizeScriptProps,
  optimizeScriptScenes,
  refineScriptProject,
  refineScriptProjectWithPrompt,
  rejectScriptProjectContentReview,
  rewriteScriptProjectPreview,
  regenerateProjectKeyframe,
  restoreScriptProject,
  restoreScriptRevision,
  retryScriptProjectVideoTask,
  retryScriptProjectDubbingTask,
  retryScriptProjectExportPackageTask,
  retryScriptProjectFinalCompositionTask,
  retryScriptProjectLipSyncTask,
  retryScriptProjectVideoEditingRenderTask,
  splitScriptProjectShots,
  submitScriptProjectContentReview,
  saveScriptProjectVideoEditingDraft,
  resetScriptProjectVideoEditingDraft,
  renderScriptProjectVideoEditingPreview,
  publishScriptProjectVideoEditingResult,
  updateScriptProjectShot,
  updateScriptAsset,
  updateScriptProjectDocument,
  uploadScriptProject,
  getWorkflowModelSettings,
  updateWorkflowModelSettings,
} from '@/api'
import type {
  AppendScriptPreviewRequest,
  AppendScriptPreviewResponse,
  ArtDirectionResponse,
  ApplyStoryboardFirstFrameRequest,
  BatchVisualPromptResponse,
  ContentReviewDecisionRequest,
  ContentReviewStatusResponse,
  ContentReviewSubmitRequest,
  DubbingTask,
  ExportPackageTask,
  ExtractedAsset,
  FinalCompositionTask,
  GenerateGroupSceneRequest,
  GroupSceneResponse,
  KeyframeRecord,
  LipSyncTask,
  PipelineStatus,
  RewriteScriptApplyRequest,
  RewriteScriptPreviewRequest,
  RewriteScriptPreviewResponse,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  ScriptRevision,
  ShotVisualPromptResponse,
  StoryboardFirstFrameResponse,
  StoryboardImageResponse,
  StoryboardPanelCropResponse,
  StoryboardPlanResponse,
  StoryboardRewriteRequest,
  ThreeViewResponse,
  StoryboardShot,
  TurnaroundImageResponse,
  TurnaroundPlanResponse,
  UpdateAssetRequest,
  UpdateShotRequest,
  UpdateScriptRequest,
  VideoSegmentTask,
  VideoEditingDraft,
  VideoEditingDraftSegment,
  VideoEditingDraftSegmentInput,
  VideoEditingPublishRequest,
  VideoEditingRenderRequest,
  VideoEditingSaveDraftRequest,
  VideoEditingSourceOption,
  VisualPromptResponse,
  WorkflowModelSettings,
  WorkflowModelSettingsUpdateRequest,
} from '@/types'

let pollTimer: number | null = null
let pollToken = 0
let pollingInFlight = false
let pollFailureCount = 0
let activeProjectId: string | null = null
const VIDEO_EDITING_DRAFT_STORAGE_KEY = 'aigc_video_editing_drafts_v1'

function shouldApplyProjectState(projectId: string) {
  return activeProjectId === null || activeProjectId === projectId
}

function readPersistedVideoEditingDraftMap(): Record<string, VideoEditingDraft> {
  if (typeof window === 'undefined') return {}
  const raw = window.localStorage.getItem(VIDEO_EDITING_DRAFT_STORAGE_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, VideoEditingDraft>
  } catch {
    return {}
  }
}

function persistVideoEditingDraftMap(map: Record<string, VideoEditingDraft>) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(VIDEO_EDITING_DRAFT_STORAGE_KEY, JSON.stringify(map))
}

function getPersistedVideoEditingDraft(projectId: string): VideoEditingDraft | null {
  return readPersistedVideoEditingDraftMap()[projectId] ?? null
}

function setPersistedVideoEditingDraft(draft: VideoEditingDraft) {
  const map = readPersistedVideoEditingDraftMap()
  map[draft.projectId] = draft
  persistVideoEditingDraftMap(map)
}

function inferSegmentDuration(shot?: StoryboardShot, candidate?: number | null) {
  if (candidate && candidate > 0) return candidate
  if (shot?.targetDurationSec && shot.targetDurationSec > 0) return shot.targetDurationSec
  return 6
}

function buildVideoEditingSourceOptions(
  shot: StoryboardShot,
  videoTasks: VideoSegmentTask[],
  lipSyncTasks: LipSyncTask[],
): VideoEditingSourceOption[] {
  const options: VideoEditingSourceOption[] = []
  const lipSync = lipSyncTasks.find((task) => task.shotId === shot.shotId && task.status === 'SUCCESS' && task.resultVideoFileId)
  const video = videoTasks.find((task) => task.shotId === shot.shotId && task.status === 'SUCCESS' && task.resultVideoFileId)
  if (lipSync?.resultVideoFileId) {
    options.push({
      sourceType: 'LIP_SYNC',
      sourceFileId: lipSync.resultVideoFileId,
      sourceTaskId: lipSync.lipSyncTaskId,
      label: '口型同步结果',
      durationSeconds: inferSegmentDuration(shot),
      available: true,
    })
  }
  if (video?.resultVideoFileId) {
    options.push({
      sourceType: 'VIDEO',
      sourceFileId: video.resultVideoFileId,
      sourceTaskId: video.segmentTaskId,
      label: '镜头视频结果',
      durationSeconds: inferSegmentDuration(shot),
      available: true,
    })
  }
  return options
}

function normalizeVideoEditingSegment(
  segment: Partial<VideoEditingDraftSegment>,
  shot: StoryboardShot,
  availableSources: VideoEditingSourceOption[],
): VideoEditingDraftSegment {
  const preferredSource =
    availableSources.find(
      (item) => item.sourceFileId === segment.sourceFileId && item.sourceType === segment.sourceType,
    ) ?? availableSources[0]
  const fallbackDuration = inferSegmentDuration(shot, preferredSource?.durationSeconds)
  let trimInSeconds = Number.isFinite(segment.trimInSeconds) ? Math.max(0, Number(segment.trimInSeconds)) : 0
  let trimOutSeconds = Number.isFinite(segment.trimOutSeconds) ? Number(segment.trimOutSeconds) : fallbackDuration
  if (trimInSeconds >= fallbackDuration) {
    trimInSeconds = 0
  }
  trimOutSeconds = Math.min(Math.max(trimOutSeconds, trimInSeconds + 0.5), fallbackDuration)

  return {
    segmentId: segment.segmentId || `seg_${shot.shotId}`,
    shotId: shot.shotId,
    sequenceNo: segment.sequenceNo ?? shot.sequenceNo,
    enabled: segment.enabled ?? true,
    sourceType: preferredSource?.sourceType ?? 'VIDEO',
    sourceFileId: preferredSource?.sourceFileId ?? '',
    sourceTaskId: preferredSource?.sourceTaskId ?? null,
    sourceDurationSeconds: fallbackDuration,
    trimInSeconds,
    trimOutSeconds,
    transitionMode: segment.transitionMode ?? 'CUT',
    transitionDurationSeconds: segment.transitionDurationSeconds ?? (segment.transitionMode === 'FADE' ? 0.3 : 0),
    notes: segment.notes ?? null,
    extension: segment.extension ?? null,
    availableSources,
  }
}

function reconcileVideoEditingDraft(
  projectId: string,
  shots: StoryboardShot[],
  videoTasks: VideoSegmentTask[],
  lipSyncTasks: LipSyncTask[],
  draft?: VideoEditingDraft | null,
): VideoEditingDraft {
  const byShotId = new Map((draft?.segments ?? []).map((segment) => [segment.shotId, segment]))
  const segments = shots
    .slice()
    .sort((a, b) => a.sequenceNo - b.sequenceNo)
    .filter((shot) => buildVideoEditingSourceOptions(shot, videoTasks, lipSyncTasks).length > 0)
    .map((shot) => {
      const availableSources = buildVideoEditingSourceOptions(shot, videoTasks, lipSyncTasks)
      return normalizeVideoEditingSegment(byShotId.get(shot.shotId) ?? {}, shot, availableSources)
    })

  return {
    draftId: draft?.draftId || `draft_${projectId}`,
    projectId,
    version: draft?.version ?? 1,
    hasUnpublishedChanges: draft?.hasUnpublishedChanges ?? false,
    lastSavedAt: draft?.lastSavedAt ?? null,
    publishedAt: draft?.publishedAt ?? null,
    publishedRenderTaskId: draft?.publishedRenderTaskId ?? null,
    latestPreviewRenderTaskId: draft?.latestPreviewRenderTaskId ?? null,
    publishedVideoFileId: draft?.publishedVideoFileId ?? null,
    extension: draft?.extension ?? { tracks: 1, placeholders: ['audio', 'subtitles', 'template'] },
    segments,
    renderTasks: [...(draft?.renderTasks ?? [])].sort((a, b) => {
      const left = new Date(b.finishedAt || b.startedAt || 0).getTime()
      const right = new Date(a.finishedAt || a.startedAt || 0).getTime()
      return left - right
    }),
  }
}

function createLocalVideoEditingDraftFromPayload(
  projectId: string,
  payload: { version?: number | null; segments: VideoEditingDraftSegmentInput[]; extension?: Record<string, unknown> | null },
  previousDraft: VideoEditingDraft | null,
  shots: StoryboardShot[],
  videoTasks: VideoSegmentTask[],
  lipSyncTasks: LipSyncTask[],
): VideoEditingDraft {
  const seedDraft: VideoEditingDraft = {
    draftId: previousDraft?.draftId || `draft_${projectId}`,
    projectId,
    version: Math.max(payload.version ?? previousDraft?.version ?? 0, previousDraft?.version ?? 0) + 1,
    hasUnpublishedChanges: true,
    lastSavedAt: new Date().toISOString(),
    publishedAt: previousDraft?.publishedAt ?? null,
    publishedRenderTaskId: previousDraft?.publishedRenderTaskId ?? null,
    latestPreviewRenderTaskId: previousDraft?.latestPreviewRenderTaskId ?? null,
    publishedVideoFileId: previousDraft?.publishedVideoFileId ?? null,
    extension: payload.extension ?? previousDraft?.extension ?? null,
    segments: payload.segments.map((segment) => ({
      segmentId: segment.segmentId || `seg_${segment.shotId}`,
      shotId: segment.shotId,
      sequenceNo: segment.sequenceNo,
      enabled: segment.enabled,
      sourceType: segment.sourceType,
      sourceFileId: segment.sourceFileId,
      sourceTaskId: segment.sourceTaskId ?? null,
      sourceDurationSeconds: null,
      trimInSeconds: segment.trimInSeconds,
      trimOutSeconds: segment.trimOutSeconds,
      transitionMode: segment.transitionMode,
      transitionDurationSeconds: segment.transitionDurationSeconds ?? 0,
      notes: segment.notes ?? null,
      extension: segment.extension ?? null,
      availableSources: [],
    })),
    renderTasks: previousDraft?.renderTasks ?? [],
  }
  return reconcileVideoEditingDraft(projectId, shots, videoTasks, lipSyncTasks, seedDraft)
}

type ScriptProjectState = {
  projects: ScriptProjectSummary[]
  currentProject: ScriptProjectAggregate | null
  scriptPayload: ScriptDocumentPayload | null
  assets: ExtractedAsset[]
  keyframes: KeyframeRecord[]
  shots: StoryboardShot[]
  videoTasks: VideoSegmentTask[]
  dubbingTasks: DubbingTask[]
  lipSyncTasks: LipSyncTask[]
  videoEditingDraft: VideoEditingDraft | null
  finalCompositionTasks: FinalCompositionTask[]
  exportPackageTasks: ExportPackageTask[]
  contentReviewStatus: ContentReviewStatusResponse | null
  pipelineStatus: PipelineStatus | null
  listLoading: boolean
  detailLoading: boolean
  createLoading: boolean
  refineLoading: boolean
  refinePromptLoading: boolean
  saveScriptLoading: boolean
  revisions: ScriptRevision[]
  revisionLoading: boolean
  optimizeLoading: boolean
  appendLoading: boolean
  rewriteLoading: boolean
  importLoading: boolean
  rewritePreviewResult: RewriteScriptPreviewResponse | null
  restoringRevisionId: string | null
  assetLoading: boolean
  keyframeLoading: boolean
  artDirectionLoading: boolean
  visualPromptLoading: boolean
  groupSceneLoading: boolean
  shotVisualLoading: boolean
  shotSaving: boolean
  shotLoading: boolean
  videoLoading: boolean
  dubbingLoading: boolean
  lipSyncLoading: boolean
  videoEditingLoading: boolean
  videoEditingSaving: boolean
  videoEditingResetting: boolean
  videoEditingRendering: boolean
  videoEditingPublishing: boolean
  finalCompositionLoading: boolean
  exportPackageLoading: boolean
  contentReviewLoading: boolean
  contentReviewSubmitting: boolean
  contentReviewProcessing: boolean
  listDeletedMode: boolean
  deletingProjectIds: string[]
  restoringProjectIds: string[]
  workflowModelSettings: WorkflowModelSettings | null
  workflowModelSettingsLoading: boolean
  workflowModelSettingsSaving: boolean
  loadProjects: (options?: { deleted?: boolean }) => Promise<ScriptProjectSummary[]>
  loadProject: (projectId: string) => Promise<ScriptProjectAggregate>
  createFromText: (payload: ScriptProjectCreateRequest) => Promise<ScriptProjectAggregate>
  createFromUpload: (payload: ScriptProjectUploadRequest) => Promise<ScriptProjectAggregate>
  removeProject: (projectId: string) => Promise<void>
  restoreProject: (projectId: string) => Promise<void>
  loadScript: (projectId: string) => Promise<ScriptDocumentPayload>
  refine: (projectId: string) => Promise<ScriptDocumentPayload>
  refineWithPrompt: (projectId: string, briefPrompt: string) => Promise<ScriptDocumentPayload>
  saveScript: (projectId: string, payload: UpdateScriptRequest) => Promise<ScriptDocumentPayload>
  loadRevisions: (projectId: string) => Promise<ScriptRevision[]>
  restoreRevision: (projectId: string, revisionId: string) => Promise<ScriptDocumentPayload>
  optimizeScenes: (projectId: string) => Promise<ScriptDocumentPayload>
  optimizeCharacters: (projectId: string) => Promise<ScriptDocumentPayload>
  optimizeProps: (projectId: string) => Promise<ScriptDocumentPayload>
  appendPreview: (projectId: string, payload?: AppendScriptPreviewRequest) => Promise<AppendScriptPreviewResponse>
  rewritePreview: (projectId: string, payload: RewriteScriptPreviewRequest) => Promise<RewriteScriptPreviewResponse>
  applyRewrite: (projectId: string, payload: RewriteScriptApplyRequest) => Promise<ScriptDocumentPayload>
  importScript: (
    projectId: string,
    file: File,
    options?: { replaceName?: string; autoRefine?: boolean },
  ) => Promise<void>
  loadAssets: (projectId: string) => Promise<ExtractedAsset[]>
  extractAssets: (projectId: string, type: 'characters' | 'backgrounds' | 'props') => Promise<ExtractedAsset[]>
  saveAsset: (projectId: string, assetId: string, payload: UpdateAssetRequest) => Promise<ExtractedAsset>
  loadKeyframes: (projectId: string) => Promise<KeyframeRecord[]>
  generateKeyframes: (projectId: string, assetId: string) => Promise<KeyframeRecord[]>
  confirmKeyframe: (projectId: string, keyframeId: string) => Promise<void>
  regenerateKeyframe: (projectId: string, keyframeId: string) => Promise<void>
  generateArtDirectionAction: (projectId: string) => Promise<ArtDirectionResponse>
  batchCharacterVisualPrompts: (projectId: string) => Promise<BatchVisualPromptResponse>
  generateVisualPromptForAsset: (projectId: string, assetId: string) => Promise<VisualPromptResponse>
  generateTurnaroundPlanForAsset: (projectId: string, assetId: string) => Promise<TurnaroundPlanResponse>
  generateTurnaroundImageForAsset: (projectId: string, assetId: string) => Promise<TurnaroundImageResponse>
  generateStoryboardPlanForAsset: (projectId: string, assetId: string) => Promise<StoryboardPlanResponse>
  translateStoryboardPlanForAsset: (projectId: string, assetId: string) => Promise<StoryboardPlanResponse>
  rewriteStoryboardPlanForAsset: (
    projectId: string,
    assetId: string,
    payload: StoryboardRewriteRequest,
  ) => Promise<StoryboardPlanResponse>
  generateStoryboardImageForAsset: (projectId: string, assetId: string) => Promise<StoryboardImageResponse>
  cropStoryboardPanelForAsset: (projectId: string, assetId: string, panelIndex: number) => Promise<StoryboardPanelCropResponse>
  applyStoryboardFirstFrameForShot: (
    projectId: string,
    shotId: string,
    payload: ApplyStoryboardFirstFrameRequest,
  ) => Promise<StoryboardFirstFrameResponse>
  generateThreeViewForAsset: (projectId: string, assetId: string) => Promise<ThreeViewResponse>
  generateGroupScenePrompt: (projectId: string, payload: GenerateGroupSceneRequest) => Promise<GroupSceneResponse>
  generateVisualPromptForShot: (projectId: string, shotId: string) => Promise<ShotVisualPromptResponse>
  saveShot: (projectId: string, shotId: string, payload: UpdateShotRequest) => Promise<StoryboardShot>
  loadShots: (projectId: string) => Promise<StoryboardShot[]>
  splitShots: (projectId: string) => Promise<StoryboardShot[]>
  loadVideoTasks: (projectId: string) => Promise<VideoSegmentTask[]>
  loadDubbingTasks: (projectId: string) => Promise<DubbingTask[]>
  loadLipSyncTasks: (projectId: string) => Promise<LipSyncTask[]>
  loadVideoEditingDraft: (projectId: string) => Promise<VideoEditingDraft>
  saveVideoEditingDraft: (projectId: string, payload: VideoEditingSaveDraftRequest) => Promise<VideoEditingDraft>
  resetVideoEditingDraft: (projectId: string) => Promise<VideoEditingDraft>
  renderVideoEditingPreview: (projectId: string, payload?: VideoEditingRenderRequest) => Promise<VideoEditingDraft>
  publishVideoEditingComposition: (projectId: string, payload?: VideoEditingPublishRequest) => Promise<VideoEditingDraft>
  retryVideoEditingRenderTask: (projectId: string, renderTaskId: string) => Promise<VideoEditingDraft>
  loadFinalCompositionTasks: (projectId: string) => Promise<FinalCompositionTask[]>
  loadExportPackageTasks: (projectId: string) => Promise<ExportPackageTask[]>
  loadContentReviewStatus: (projectId: string) => Promise<ContentReviewStatusResponse>
  loadPipelineStatus: (projectId: string) => Promise<PipelineStatus>
  startVideoGeneration: (projectId: string) => Promise<PipelineStatus>
  retryVideoTask: (projectId: string, segmentTaskId: string) => Promise<PipelineStatus>
  startDubbingGeneration: (projectId: string) => Promise<PipelineStatus>
  retryDubbingTask: (projectId: string, dubbingTaskId: string) => Promise<PipelineStatus>
  startLipSyncGeneration: (projectId: string) => Promise<PipelineStatus>
  retryLipSyncTask: (projectId: string, lipSyncTaskId: string) => Promise<PipelineStatus>
  startFinalComposition: (projectId: string) => Promise<PipelineStatus>
  retryFinalCompositionTask: (projectId: string, finalCompositionTaskId: string) => Promise<PipelineStatus>
  startExportPackage: (projectId: string) => Promise<PipelineStatus>
  retryExportPackageTask: (projectId: string, exportPackageTaskId: string) => Promise<PipelineStatus>
  submitContentReview: (projectId: string, payload?: ContentReviewSubmitRequest) => Promise<ContentReviewStatusResponse>
  approveContentReview: (projectId: string, payload?: ContentReviewDecisionRequest) => Promise<ContentReviewStatusResponse>
  rejectContentReview: (projectId: string, payload?: ContentReviewDecisionRequest) => Promise<ContentReviewStatusResponse>
  hydrate: (projectId: string) => Promise<void>
  startPolling: (projectId: string) => void
  stopPolling: () => void
  loadWorkflowModelSettings: (projectId: string) => Promise<WorkflowModelSettings>
  saveWorkflowModelSettings: (projectId: string, req: WorkflowModelSettingsUpdateRequest) => Promise<WorkflowModelSettings>
}

export const useScriptProjectStore = create<ScriptProjectState>((set, get) => ({
  projects: [],
  currentProject: null,
  scriptPayload: null,
  assets: [],
  keyframes: [],
  shots: [],
  videoTasks: [],
  dubbingTasks: [],
  lipSyncTasks: [],
  videoEditingDraft: null,
  finalCompositionTasks: [],
  exportPackageTasks: [],
  contentReviewStatus: null,
  pipelineStatus: null,
  listLoading: false,
  detailLoading: false,
  createLoading: false,
  refineLoading: false,
  refinePromptLoading: false,
  saveScriptLoading: false,
  revisions: [],
  revisionLoading: false,
  optimizeLoading: false,
  appendLoading: false,
  rewriteLoading: false,
  importLoading: false,
  rewritePreviewResult: null,
  restoringRevisionId: null,
  assetLoading: false,
  keyframeLoading: false,
  artDirectionLoading: false,
  visualPromptLoading: false,
  groupSceneLoading: false,
  shotVisualLoading: false,
  shotSaving: false,
  shotLoading: false,
  videoLoading: false,
  dubbingLoading: false,
  lipSyncLoading: false,
  videoEditingLoading: false,
  videoEditingSaving: false,
  videoEditingResetting: false,
  videoEditingRendering: false,
  videoEditingPublishing: false,
  finalCompositionLoading: false,
  exportPackageLoading: false,
  contentReviewLoading: false,
  contentReviewSubmitting: false,
  contentReviewProcessing: false,
  listDeletedMode: false,
  deletingProjectIds: [],
  restoringProjectIds: [],
  workflowModelSettings: null,
  workflowModelSettingsLoading: false,
  workflowModelSettingsSaving: false,

  loadProjects: async (options) => {
    const deleted = options?.deleted === true
    set({ listLoading: true })
    try {
      const list = await listScriptProjects({ deleted })
      set({ projects: list, listDeletedMode: deleted })
      return list
    } finally {
      set({ listLoading: false })
    }
  },

  loadProject: async (projectId) => {
    activeProjectId = projectId
    set({ detailLoading: true })
    try {
      const p = await getScriptProject(projectId)
      if (shouldApplyProjectState(projectId)) {
        set({
          currentProject: p,
          videoTasks: p.videoTasks ?? [],
          dubbingTasks: p.dubbingTasks ?? [],
          lipSyncTasks: p.lipSyncTasks ?? [],
          videoEditingDraft: p.videoEditingDraft ?? get().videoEditingDraft,
          finalCompositionTasks: p.finalCompositionTasks ?? [],
          exportPackageTasks: p.exportPackageTasks ?? [],
        })
      }
      return p
    } finally {
      set({ detailLoading: false })
    }
  },

  createFromText: async (payload) => {
    set({ createLoading: true })
    try {
      const result = await createScriptProject(payload)
      set({ currentProject: result })
      await get().loadProjects()
      return result
    } finally {
      set({ createLoading: false })
    }
  },

  createFromUpload: async (payload) => {
    set({ createLoading: true })
    try {
      const result = await uploadScriptProject(payload)
      set({ currentProject: result })
      await get().loadProjects()
      return result
    } finally {
      set({ createLoading: false })
    }
  },

  removeProject: async (projectId) => {
    set((s) => ({ deletingProjectIds: [...s.deletingProjectIds, projectId] }))
    try {
      await deleteScriptProject(projectId)
      set((s) => ({
        projects: s.projects.filter((item) => item.projectId !== projectId),
        currentProject: s.currentProject?.project.projectId === projectId ? null : s.currentProject,
      }))
      await get().loadProjects({ deleted: get().listDeletedMode })
    } finally {
      set((s) => ({
        deletingProjectIds: s.deletingProjectIds.filter((id) => id !== projectId),
      }))
    }
  },

  restoreProject: async (projectId) => {
    set((s) => ({ restoringProjectIds: [...s.restoringProjectIds, projectId] }))
    try {
      await restoreScriptProject(projectId)
      set((s) => ({
        projects: s.projects.filter((item) => item.projectId !== projectId),
      }))
      await get().loadProjects({ deleted: get().listDeletedMode })
    } finally {
      set((s) => ({
        restoringProjectIds: s.restoringProjectIds.filter((id) => id !== projectId),
      }))
    }
  },

  loadScript: async (projectId) => {
    const doc = await getScriptProjectDocument(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ scriptPayload: doc })
    }
    return doc
  },

  refine: async (projectId) => {
    set({ refineLoading: true })
    try {
      const doc = await refineScriptProject(projectId)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ refineLoading: false })
    }
  },

  refineWithPrompt: async (projectId, briefPrompt) => {
    set({ refinePromptLoading: true })
    try {
      const doc = await refineScriptProjectWithPrompt(projectId, briefPrompt)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ refinePromptLoading: false })
    }
  },

  saveScript: async (projectId, payload) => {
    set({ saveScriptLoading: true })
    try {
      const doc = await updateScriptProjectDocument(projectId, payload)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ saveScriptLoading: false })
    }
  },

  loadRevisions: async (projectId) => {
    set({ revisionLoading: true })
    try {
      const list = await listScriptRevisions(projectId)
      if (shouldApplyProjectState(projectId)) {
        set({ revisions: list })
      }
      return list
    } finally {
      set({ revisionLoading: false })
    }
  },

  restoreRevision: async (projectId, revisionId) => {
    set({ restoringRevisionId: revisionId })
    try {
      const doc = await restoreScriptRevision(projectId, revisionId)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ restoringRevisionId: null })
    }
  },

  optimizeScenes: async (projectId) => {
    set({ optimizeLoading: true })
    try {
      const doc = await optimizeScriptScenes(projectId)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ optimizeLoading: false })
    }
  },

  optimizeCharacters: async (projectId) => {
    set({ optimizeLoading: true })
    try {
      const doc = await optimizeScriptCharacters(projectId)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ optimizeLoading: false })
    }
  },

  optimizeProps: async (projectId) => {
    set({ optimizeLoading: true })
    try {
      const doc = await optimizeScriptProps(projectId)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ optimizeLoading: false })
    }
  },

  appendPreview: async (projectId, payload) => {
    set({ appendLoading: true })
    try {
      return await appendScriptProjectPreview(projectId, payload)
    } finally {
      set({ appendLoading: false })
    }
  },

  rewritePreview: async (projectId, payload) => {
    set({ rewriteLoading: true })
    try {
      const result = await rewriteScriptProjectPreview(projectId, payload)
      set({ rewritePreviewResult: result })
      return result
    } finally {
      set({ rewriteLoading: false })
    }
  },

  applyRewrite: async (projectId, payload) => {
    set({ rewriteLoading: true })
    try {
      const doc = await applyRewriteScriptProject(projectId, payload)
      set({ scriptPayload: doc })
      await get().loadProject(projectId)
      await get().loadRevisions(projectId)
      return doc
    } finally {
      set({ rewriteLoading: false })
    }
  },

  importScript: async (projectId, file, options) => {
    set({ importLoading: true })
    try {
      await importScriptToProject(projectId, file, options)
      await get().loadProject(projectId)
      await get().loadScript(projectId)
      await get().loadRevisions(projectId)
    } finally {
      set({ importLoading: false })
    }
  },

  loadAssets: async (projectId) => {
    const list = await getScriptAssets(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ assets: list })
    }
    return list
  },

  extractAssets: async (projectId, type) => {
    set({ assetLoading: true })
    try {
      await extractScriptAssets(projectId, type)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return get().assets
    } finally {
      set({ assetLoading: false })
    }
  },

  saveAsset: async (projectId, assetId, payload) => {
    set({ assetLoading: true })
    try {
      const updated = await updateScriptAsset(projectId, assetId, payload)
      set((s) => ({
        assets: s.assets.map((item) => (item.assetId === assetId ? updated : item)),
      }))
      return updated
    } finally {
      set({ assetLoading: false })
    }
  },

  loadKeyframes: async (projectId) => {
    const list = await getProjectKeyframes(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ keyframes: list })
    }
    return list
  },

  generateKeyframes: async (projectId, assetId) => {
    set({ keyframeLoading: true })
    try {
      await generateAssetKeyframes(projectId, assetId)
      await get().loadKeyframes(projectId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return get().keyframes
    } finally {
      set({ keyframeLoading: false })
    }
  },

  confirmKeyframe: async (projectId, keyframeId) => {
    set({ keyframeLoading: true })
    try {
      await confirmProjectKeyframe(projectId, keyframeId)
      await get().loadKeyframes(projectId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
    } finally {
      set({ keyframeLoading: false })
    }
  },

  regenerateKeyframe: async (projectId, keyframeId) => {
    set({ keyframeLoading: true })
    try {
      await regenerateProjectKeyframe(projectId, keyframeId)
      await get().loadKeyframes(projectId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
    } finally {
      set({ keyframeLoading: false })
    }
  },

  generateArtDirectionAction: async (projectId) => {
    set({ artDirectionLoading: true })
    try {
      const res = await generateArtDirection(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ artDirectionLoading: false })
    }
  },

  batchCharacterVisualPrompts: async (projectId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await batchGenerateCharacterVisualPrompts(projectId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateVisualPromptForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateAssetVisualPrompt(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateTurnaroundPlanForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateTurnaroundPlan(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateTurnaroundImageForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateTurnaroundImage(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateStoryboardPlanForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateStoryboardPlan(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  translateStoryboardPlanForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await translateStoryboardPlan(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  rewriteStoryboardPlanForAsset: async (projectId, assetId, payload) => {
    set({ visualPromptLoading: true })
    try {
      const res = await rewriteStoryboardPlan(projectId, assetId, payload)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateStoryboardImageForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateStoryboardImage(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  cropStoryboardPanelForAsset: async (projectId, assetId, panelIndex) => {
    set({ visualPromptLoading: true })
    try {
      const res = await cropStoryboardPanel(projectId, assetId, panelIndex)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateThreeViewForAsset: async (projectId, assetId) => {
    set({ visualPromptLoading: true })
    try {
      const res = await generateThreeView(projectId, assetId)
      await get().loadAssets(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ visualPromptLoading: false })
    }
  },

  generateGroupScenePrompt: async (projectId, payload) => {
    set({ groupSceneLoading: true })
    try {
      const res = await generateGroupScene(projectId, payload)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ groupSceneLoading: false })
    }
  },

  generateVisualPromptForShot: async (projectId, shotId) => {
    set({ shotVisualLoading: true })
    try {
      const res = await generateShotVisualPrompt(projectId, shotId)
      await get().loadShots(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ shotVisualLoading: false })
    }
  },

  saveShot: async (projectId, shotId, payload) => {
    set({ shotSaving: true })
    try {
      const updated = await updateScriptProjectShot(projectId, shotId, payload)
      set((s) => ({
        shots: s.shots.map((it) => (it.shotId === shotId ? updated : it)),
      }))
      await get().loadProject(projectId)
      return updated
    } finally {
      set({ shotSaving: false })
    }
  },

  applyStoryboardFirstFrameForShot: async (projectId, shotId, payload) => {
    set({ shotSaving: true })
    try {
      const res = await applyStoryboardFirstFrame(projectId, shotId, payload)
      await get().loadShots(projectId)
      await get().loadProject(projectId)
      return res
    } finally {
      set({ shotSaving: false })
    }
  },

  loadShots: async (projectId) => {
    const list = await getScriptProjectShots(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ shots: list })
    }
    return list
  },

  splitShots: async (projectId) => {
    set({ shotLoading: true })
    try {
      const list = await splitScriptProjectShots(projectId)
      set({ shots: list })
      await get().loadProject(projectId)
      return list
    } finally {
      set({ shotLoading: false })
    }
  },

  loadVideoTasks: async (projectId) => {
    const list = await getScriptProjectVideoTasks(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ videoTasks: list })
    }
    return list
  },

  loadDubbingTasks: async (projectId) => {
    const list = await getScriptProjectDubbingTasks(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ dubbingTasks: list })
    }
    return list
  },

  loadLipSyncTasks: async (projectId) => {
    const list = await getScriptProjectLipSyncTasks(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ lipSyncTasks: list })
    }
    return list
  },

  loadVideoEditingDraft: async (projectId) => {
    set({ videoEditingLoading: true })
    try {
      const state = get()
      try {
        const [remoteDraft, renderTasks] = await Promise.all([
          getScriptProjectVideoEditingDraft(projectId),
          getScriptProjectVideoEditingRenderTasks(projectId).catch(() => []),
        ])
        const draft = reconcileVideoEditingDraft(
          projectId,
          state.shots,
          state.videoTasks,
          state.lipSyncTasks,
          {
            ...remoteDraft,
            renderTasks,
          },
        )
        if (shouldApplyProjectState(projectId)) {
          set({ videoEditingDraft: draft })
        }
        setPersistedVideoEditingDraft(draft)
        return draft
      } catch {
        const fallbackDraft = reconcileVideoEditingDraft(
          projectId,
          state.shots,
          state.videoTasks,
          state.lipSyncTasks,
          getPersistedVideoEditingDraft(projectId),
        )
        if (shouldApplyProjectState(projectId)) {
          set({ videoEditingDraft: fallbackDraft })
        }
        setPersistedVideoEditingDraft(fallbackDraft)
        return fallbackDraft
      }
    } finally {
      set({ videoEditingLoading: false })
    }
  },

  saveVideoEditingDraft: async (projectId, payload) => {
    set({ videoEditingSaving: true })
    try {
      try {
        const savedDraft = await saveScriptProjectVideoEditingDraft(projectId, payload)
        const draft = reconcileVideoEditingDraft(
          projectId,
          get().shots,
          get().videoTasks,
          get().lipSyncTasks,
          {
            ...savedDraft,
            renderTasks: get().videoEditingDraft?.renderTasks ?? savedDraft.renderTasks,
          },
        )
        set({ videoEditingDraft: draft })
        setPersistedVideoEditingDraft(draft)
        return draft
      } catch {
        const localDraft = createLocalVideoEditingDraftFromPayload(
          projectId,
          payload,
          get().videoEditingDraft,
          get().shots,
          get().videoTasks,
          get().lipSyncTasks,
        )
        set({ videoEditingDraft: localDraft })
        setPersistedVideoEditingDraft(localDraft)
        return localDraft
      }
    } finally {
      set({ videoEditingSaving: false })
    }
  },

  resetVideoEditingDraft: async (projectId) => {
    set({ videoEditingResetting: true })
    try {
      try {
        const resetDraft = await resetScriptProjectVideoEditingDraft(projectId)
        const draft = reconcileVideoEditingDraft(
          projectId,
          get().shots,
          get().videoTasks,
          get().lipSyncTasks,
          {
            ...resetDraft,
            renderTasks: get().videoEditingDraft?.renderTasks ?? resetDraft.renderTasks,
          },
        )
        set({ videoEditingDraft: draft })
        setPersistedVideoEditingDraft(draft)
        return draft
      } catch {
        const localDraft = reconcileVideoEditingDraft(
          projectId,
          get().shots,
          get().videoTasks,
          get().lipSyncTasks,
          {
            ...get().videoEditingDraft,
            draftId: get().videoEditingDraft?.draftId || `draft_${projectId}`,
            projectId,
            version: (get().videoEditingDraft?.version ?? 0) + 1,
            hasUnpublishedChanges: true,
            lastSavedAt: new Date().toISOString(),
            renderTasks: get().videoEditingDraft?.renderTasks ?? [],
            segments: [],
          } as VideoEditingDraft,
        )
        set({ videoEditingDraft: localDraft })
        setPersistedVideoEditingDraft(localDraft)
        return localDraft
      }
    } finally {
      set({ videoEditingResetting: false })
    }
  },

  renderVideoEditingPreview: async (projectId, payload) => {
    set({ videoEditingRendering: true })
    try {
      const status = await renderScriptProjectVideoEditingPreview(projectId, payload)
      set({ pipelineStatus: status })
      const draft = await get().loadVideoEditingDraft(projectId)
      setPersistedVideoEditingDraft(draft)
      get().startPolling(projectId)
      return draft
    } finally {
      set({ videoEditingRendering: false })
    }
  },

  publishVideoEditingComposition: async (projectId, payload) => {
    set({ videoEditingPublishing: true })
    try {
      const status = await publishScriptProjectVideoEditingResult(projectId, payload)
      set({ pipelineStatus: status })
      const draft = await get().loadVideoEditingDraft(projectId)
      setPersistedVideoEditingDraft(draft)
      await get().loadPipelineStatus(projectId)
      return draft
    } finally {
      set({ videoEditingPublishing: false })
    }
  },

  retryVideoEditingRenderTask: async (projectId, renderTaskId) => {
    set({ videoEditingRendering: true })
    try {
      const status = await retryScriptProjectVideoEditingRenderTask(projectId, renderTaskId)
      set({ pipelineStatus: status })
      const nextDraft = await get().loadVideoEditingDraft(projectId)
      set({ videoEditingDraft: nextDraft })
      setPersistedVideoEditingDraft(nextDraft)
      get().startPolling(projectId)
      return nextDraft
    } finally {
      set({ videoEditingRendering: false })
    }
  },

  loadFinalCompositionTasks: async (projectId) => {
    const list = await getScriptProjectFinalCompositionTasks(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ finalCompositionTasks: list })
    }
    return list
  },

  loadExportPackageTasks: async (projectId) => {
    const list = await getScriptProjectExportPackageTasks(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ exportPackageTasks: list })
    }
    return list
  },

  loadContentReviewStatus: async (projectId) => {
    set({ contentReviewLoading: true })
    try {
      const status = await getScriptProjectContentReviewStatus(projectId)
      if (shouldApplyProjectState(projectId)) {
        set({ contentReviewStatus: status })
      }
      return status
    } finally {
      set({ contentReviewLoading: false })
    }
  },

  loadPipelineStatus: async (projectId) => {
    const st = await getScriptProjectPipelineStatus(projectId)
    if (shouldApplyProjectState(projectId)) {
      set({ pipelineStatus: st })
    }
    return st
  },

  startVideoGeneration: async (projectId) => {
    set({ videoLoading: true })
    try {
      const st = await generateScriptProjectVideos(projectId)
      set({ pipelineStatus: st })
      await get().loadVideoTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return st
    } finally {
      set({ videoLoading: false })
    }
  },

  retryVideoTask: async (projectId, segmentTaskId) => {
    set({ videoLoading: true })
    try {
      const st = await retryScriptProjectVideoTask(projectId, segmentTaskId)
      set({ pipelineStatus: st })
      await get().loadVideoTasks(projectId)
      get().startPolling(projectId)
      return st
    } finally {
      set({ videoLoading: false })
    }
  },

  startDubbingGeneration: async (projectId) => {
    set({ dubbingLoading: true })
    try {
      const status = await generateScriptProjectDubbing(projectId)
      set({ pipelineStatus: status })
      await get().loadDubbingTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ dubbingLoading: false })
    }
  },

  retryDubbingTask: async (projectId, dubbingTaskId) => {
    set({ dubbingLoading: true })
    try {
      const status = await retryScriptProjectDubbingTask(projectId, dubbingTaskId)
      set({ pipelineStatus: status })
      await get().loadDubbingTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ dubbingLoading: false })
    }
  },

  startLipSyncGeneration: async (projectId) => {
    set({ lipSyncLoading: true })
    try {
      const status = await generateScriptProjectLipSync(projectId)
      set({ pipelineStatus: status })
      await get().loadLipSyncTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ lipSyncLoading: false })
    }
  },

  retryLipSyncTask: async (projectId, lipSyncTaskId) => {
    set({ lipSyncLoading: true })
    try {
      const status = await retryScriptProjectLipSyncTask(projectId, lipSyncTaskId)
      set({ pipelineStatus: status })
      await get().loadLipSyncTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ lipSyncLoading: false })
    }
  },

  startFinalComposition: async (projectId) => {
    set({ finalCompositionLoading: true })
    try {
      const status = await generateScriptProjectFinalComposition(projectId)
      set({ pipelineStatus: status })
      await get().loadFinalCompositionTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ finalCompositionLoading: false })
    }
  },

  retryFinalCompositionTask: async (projectId, finalCompositionTaskId) => {
    set({ finalCompositionLoading: true })
    try {
      const status = await retryScriptProjectFinalCompositionTask(projectId, finalCompositionTaskId)
      set({ pipelineStatus: status })
      await get().loadFinalCompositionTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ finalCompositionLoading: false })
    }
  },

  startExportPackage: async (projectId) => {
    set({ exportPackageLoading: true })
    try {
      const status = await generateScriptProjectExportPackage(projectId)
      set({ pipelineStatus: status })
      await get().loadExportPackageTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ exportPackageLoading: false })
    }
  },

  retryExportPackageTask: async (projectId, exportPackageTaskId) => {
    set({ exportPackageLoading: true })
    try {
      const status = await retryScriptProjectExportPackageTask(projectId, exportPackageTaskId)
      set({ pipelineStatus: status })
      await get().loadExportPackageTasks(projectId)
      await get().loadProject(projectId)
      get().startPolling(projectId)
      return status
    } finally {
      set({ exportPackageLoading: false })
    }
  },

  submitContentReview: async (projectId, payload) => {
    set({ contentReviewSubmitting: true })
    try {
      const status = await submitScriptProjectContentReview(projectId, payload)
      set({ contentReviewStatus: status })
      await get().loadProject(projectId)
      await get().loadPipelineStatus(projectId)
      return status
    } finally {
      set({ contentReviewSubmitting: false })
    }
  },

  approveContentReview: async (projectId, payload) => {
    set({ contentReviewProcessing: true })
    try {
      const status = await approveScriptProjectContentReview(projectId, payload)
      set({ contentReviewStatus: status })
      await get().loadProject(projectId)
      await get().loadPipelineStatus(projectId)
      return status
    } finally {
      set({ contentReviewProcessing: false })
    }
  },

  rejectContentReview: async (projectId, payload) => {
    set({ contentReviewProcessing: true })
    try {
      const status = await rejectScriptProjectContentReview(projectId, payload)
      set({ contentReviewStatus: status })
      await get().loadProject(projectId)
      await get().loadPipelineStatus(projectId)
      return status
    } finally {
      set({ contentReviewProcessing: false })
    }
  },

  hydrate: async (projectId) => {
    activeProjectId = projectId
    await Promise.all([
      get().loadProject(projectId),
      get().loadScript(projectId),
      get().loadAssets(projectId),
      get().loadKeyframes(projectId),
      get().loadShots(projectId),
      get().loadVideoTasks(projectId),
      get().loadDubbingTasks(projectId),
      get().loadLipSyncTasks(projectId),
      get().loadFinalCompositionTasks(projectId),
      get().loadExportPackageTasks(projectId),
      get().loadContentReviewStatus(projectId),
      get().loadPipelineStatus(projectId),
    ])
    await get().loadVideoEditingDraft(projectId)
  },

  startPolling: (projectId) => {
    get().stopPolling()
    activeProjectId = projectId
    const currentToken = ++pollToken
    const schedule = (delay: number) => {
      pollTimer = window.setTimeout(() => {
        void tick()
      }, delay)
    }
    const tick = async () => {
      if (currentToken !== pollToken) return
      if (pollingInFlight) {
        schedule(1000)
        return
      }
      pollingInFlight = true
      try {
        await Promise.all([
          get().loadVideoTasks(projectId),
          get().loadDubbingTasks(projectId),
          get().loadLipSyncTasks(projectId),
          get().loadVideoEditingDraft(projectId),
          get().loadFinalCompositionTasks(projectId),
          get().loadExportPackageTasks(projectId),
          get().loadPipelineStatus(projectId),
          get().loadProject(projectId),
        ])
        pollFailureCount = 0
        const status = get().pipelineStatus?.projectStatus
        if (
          status &&
          status !== 'VIDEO_GENERATING' &&
          status !== 'DUBBING_GENERATING' &&
          status !== 'LIP_SYNC_GENERATING' &&
          status !== 'VIDEO_EDITING_RENDERING' &&
          status !== 'FINAL_COMPOSITION_GENERATING' &&
          status !== 'EXPORT_PACKAGE_GENERATING'
        ) {
          get().stopPolling()
          return
        }
        schedule(3000)
      } catch {
        pollFailureCount += 1
        if (pollFailureCount >= 3) {
          get().stopPolling()
          return
        }
        schedule(Math.min(12000, 2000 * 2 ** (pollFailureCount - 1)))
      } finally {
        pollingInFlight = false
      }
    }
    schedule(1000)
  },

  stopPolling: () => {
    pollToken += 1
    pollFailureCount = 0
    pollingInFlight = false
    if (pollTimer != null) {
      window.clearTimeout(pollTimer)
      pollTimer = null
    }
  },

  loadWorkflowModelSettings: async (projectId) => {
    set({ workflowModelSettingsLoading: true })
    try {
      const settings = await getWorkflowModelSettings(projectId)
      set({ workflowModelSettings: settings })
      return settings
    } finally {
      set({ workflowModelSettingsLoading: false })
    }
  },

  saveWorkflowModelSettings: async (projectId, req) => {
    set({ workflowModelSettingsSaving: true })
    try {
      const settings = await updateWorkflowModelSettings(projectId, req)
      set({ workflowModelSettings: settings })
      return settings
    } finally {
      set({ workflowModelSettingsSaving: false })
    }
  },
}))
