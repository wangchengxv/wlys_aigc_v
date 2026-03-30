import { create } from 'zustand'
import {
  confirmProjectKeyframe,
  createScriptProject,
  deleteScriptProject,
  extractScriptAssets,
  generateAssetKeyframes,
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
  regenerateProjectKeyframe,
  restoreScriptRevision,
  retryScriptProjectVideoTask,
  splitScriptProjectShots,
  updateScriptAsset,
  updateScriptProjectDocument,
  uploadScriptProject,
} from '@/api'
import type {
  ExtractedAsset,
  KeyframeRecord,
  PipelineStatus,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  ScriptRevision,
  StoryboardShot,
  UpdateAssetRequest,
  UpdateScriptRequest,
  VideoSegmentTask,
} from '@/types'

let pollTimer: number | null = null

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
  importLoading: boolean
  restoringRevisionId: string | null
  assetLoading: boolean
  keyframeLoading: boolean
  shotLoading: boolean
  videoLoading: boolean
  loadProjects: () => Promise<ScriptProjectSummary[]>
  loadProject: (projectId: string) => Promise<ScriptProjectAggregate>
  createFromText: (payload: ScriptProjectCreateRequest) => Promise<ScriptProjectAggregate>
  createFromUpload: (payload: ScriptProjectUploadRequest) => Promise<ScriptProjectAggregate>
  removeProject: (projectId: string) => Promise<void>
  loadScript: (projectId: string) => Promise<ScriptDocumentPayload>
  refine: (projectId: string) => Promise<ScriptDocumentPayload>
  refineWithPrompt: (projectId: string, briefPrompt: string) => Promise<ScriptDocumentPayload>
  saveScript: (projectId: string, payload: UpdateScriptRequest) => Promise<ScriptDocumentPayload>
  loadRevisions: (projectId: string) => Promise<ScriptRevision[]>
  restoreRevision: (projectId: string, revisionId: string) => Promise<ScriptDocumentPayload>
  optimizeScenes: (projectId: string) => Promise<ScriptDocumentPayload>
  optimizeCharacters: (projectId: string) => Promise<ScriptDocumentPayload>
  optimizeProps: (projectId: string) => Promise<ScriptDocumentPayload>
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
  loadShots: (projectId: string) => Promise<StoryboardShot[]>
  splitShots: (projectId: string) => Promise<StoryboardShot[]>
  loadVideoTasks: (projectId: string) => Promise<VideoSegmentTask[]>
  loadPipelineStatus: (projectId: string) => Promise<PipelineStatus>
  startVideoGeneration: (projectId: string) => Promise<PipelineStatus>
  retryVideoTask: (projectId: string, segmentTaskId: string) => Promise<PipelineStatus>
  hydrate: (projectId: string) => Promise<void>
  startPolling: (projectId: string) => void
  stopPolling: () => void
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
  importLoading: false,
  restoringRevisionId: null,
  assetLoading: false,
  keyframeLoading: false,
  shotLoading: false,
  videoLoading: false,

  loadProjects: async () => {
    set({ listLoading: true })
    try {
      const list = await listScriptProjects()
      set({ projects: list })
      return list
    } finally {
      set({ listLoading: false })
    }
  },

  loadProject: async (projectId) => {
    set({ detailLoading: true })
    try {
      const p = await getScriptProject(projectId)
      set({ currentProject: p })
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
    await deleteScriptProject(projectId)
    set((s) => ({
      projects: s.projects.filter((item) => item.projectId !== projectId),
      currentProject: s.currentProject?.project.projectId === projectId ? null : s.currentProject,
    }))
  },

  loadScript: async (projectId) => {
    const doc = await getScriptProjectDocument(projectId)
    set({ scriptPayload: doc })
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
      set({ revisions: list })
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
    set({ assets: list })
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
    set({ keyframes: list })
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

  loadShots: async (projectId) => {
    const list = await getScriptProjectShots(projectId)
    set({ shots: list })
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
    set({ videoTasks: list })
    return list
  },

  loadPipelineStatus: async (projectId) => {
    const st = await getScriptProjectPipelineStatus(projectId)
    set({ pipelineStatus: st })
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
    pollTimer = window.setInterval(async () => {
      try {
        await Promise.all([
          get().loadVideoTasks(projectId),
          get().loadPipelineStatus(projectId),
          get().loadProject(projectId),
        ])
        const status = get().pipelineStatus?.projectStatus
        if (status && status !== 'VIDEO_GENERATING') {
          get().stopPolling()
        }
      } catch {
        get().stopPolling()
      }
    }, 3000)
  },

  stopPolling: () => {
    if (pollTimer != null) {
      window.clearInterval(pollTimer)
      pollTimer = null
    }
  },
}))
