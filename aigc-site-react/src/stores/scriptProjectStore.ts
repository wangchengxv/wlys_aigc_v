import { create } from 'zustand'
import {
  applyStoryboardFirstFrame,
  applyRewriteScriptProject,
  appendScriptProjectPreview,
  confirmProjectKeyframe,
  createScriptProject,
  deleteScriptProject,
  extractScriptAssets,
  generateArtDirection,
  generateAssetKeyframes,
  generateAssetVisualPrompt,
  generateGroupScene,
  generateThreeView,
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
  getScriptProject,
  getScriptProjectDocument,
  getScriptProjectPipelineStatus,
  getScriptProjectShots,
  getScriptProjectVideoTasks,
  importScriptToProject,
  listScriptProjects,
  listScriptRevisions,
  optimizeScriptCharacters,
  optimizeScriptProps,
  optimizeScriptScenes,
  refineScriptProject,
  refineScriptProjectWithPrompt,
  rewriteScriptProjectPreview,
  regenerateProjectKeyframe,
  restoreScriptProject,
  restoreScriptRevision,
  retryScriptProjectVideoTask,
  splitScriptProjectShots,
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
  ExtractedAsset,
  GenerateGroupSceneRequest,
  GroupSceneResponse,
  KeyframeRecord,
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
  VisualPromptResponse,
  WorkflowModelSettings,
  WorkflowModelSettingsUpdateRequest,
} from '@/types'

let pollTimer: number | null = null
let pollToken = 0
let pollingInFlight = false
let pollFailureCount = 0
let activeProjectId: string | null = null

function shouldApplyProjectState(projectId: string) {
  return activeProjectId === null || activeProjectId === projectId
}

type ScriptProjectState = {
  projects: ScriptProjectSummary[]
  currentProject: ScriptProjectAggregate | null
  scriptPayload: ScriptDocumentPayload | null
  assets: ExtractedAsset[]
  keyframes: KeyframeRecord[]
  shots: StoryboardShot[]
  videoTasks: VideoSegmentTask[]
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
  loadPipelineStatus: (projectId: string) => Promise<PipelineStatus>
  startVideoGeneration: (projectId: string) => Promise<PipelineStatus>
  retryVideoTask: (projectId: string, segmentTaskId: string) => Promise<PipelineStatus>
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
        set({ currentProject: p })
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

  hydrate: async (projectId) => {
    activeProjectId = projectId
    await Promise.all([
      get().loadProject(projectId),
      get().loadScript(projectId),
      get().loadAssets(projectId),
      get().loadKeyframes(projectId),
      get().loadShots(projectId),
      get().loadVideoTasks(projectId),
      get().loadPipelineStatus(projectId),
    ])
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
        await Promise.all([get().loadVideoTasks(projectId), get().loadPipelineStatus(projectId), get().loadProject(projectId)])
        pollFailureCount = 0
        const status = get().pipelineStatus?.projectStatus
        if (status && status !== 'VIDEO_GENERATING') {
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
