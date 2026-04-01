import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  applyRewriteScriptProject,
  appendScriptProjectPreview,
  applyStoryboardFirstFrame,
  batchGenerateCharacterVisualPrompts,
  confirmProjectKeyframe,
  createScriptProject,
  deleteScriptProject,
  extractScriptAssets,
  generateArtDirection,
  generateAssetKeyframes,
  generateAssetVisualPrompt,
  generateGroupScene,
  generateScriptProjectVideos,
  generateShotVisualPrompt,
  generateStoryboardImage,
  generateStoryboardPlan,
  generateThreeView,
  generateTurnaroundImage,
  generateTurnaroundPlan,
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
  restoreScriptProject,
  restoreScriptRevision,
  retryScriptProjectVideoTask,
  rewriteScriptProjectPreview,
  rewriteStoryboardPlan,
  splitScriptProjectShots,
  translateStoryboardPlan,
  updateScriptAsset,
  updateScriptProjectDocument,
  updateScriptProjectShot,
  uploadScriptProject,
} from '@/services/api'
import type {
  AppendScriptPreviewRequest,
  AppendScriptPreviewResponse,
  ApplyStoryboardFirstFrameRequest,
  AssetType,
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
  StoryboardRewriteRequest,
  StoryboardShot,
  UpdateAssetRequest,
  UpdateScriptRequest,
  UpdateShotRequest,
  VideoSegmentTask,
} from '@/types'

let pollTimer: number | null = null
let pollToken = 0
let pollingInFlight = false
let pollFailureCount = 0
let activeProjectId: string | null = null

function shouldApply(projectId: string) {
  return activeProjectId === null || activeProjectId === projectId
}

export const useScriptProjectStore = defineStore('script-projects', () => {
  const projects = ref<ScriptProjectSummary[]>([])
  const currentProject = ref<ScriptProjectAggregate | null>(null)
  const scriptPayload = ref<ScriptDocumentPayload | null>(null)
  const assets = ref<ExtractedAsset[]>([])
  const keyframes = ref<KeyframeRecord[]>([])
  const shots = ref<StoryboardShot[]>([])
  const videoTasks = ref<VideoSegmentTask[]>([])
  const pipelineStatus = ref<PipelineStatus | null>(null)

  const listLoading = ref(false)
  const detailLoading = ref(false)
  const createLoading = ref(false)
  const refineLoading = ref(false)
  const refinePromptLoading = ref(false)
  const saveScriptLoading = ref(false)
  const revisionLoading = ref(false)
  const optimizeLoading = ref(false)
  const appendLoading = ref(false)
  const rewriteLoading = ref(false)
  const importLoading = ref(false)
  const restoringRevisionId = ref<string | null>(null)
  const rewritePreviewResult = ref<RewriteScriptPreviewResponse | null>(null)
  const revisions = ref<ScriptRevision[]>([])
  const listDeletedMode = ref(false)
  const deletingProjectIds = ref<string[]>([])
  const restoringProjectIds = ref<string[]>([])
  const assetLoading = ref(false)
  const keyframeLoading = ref(false)
  const artDirectionLoading = ref(false)
  const visualPromptLoading = ref(false)
  const groupSceneLoading = ref(false)
  const shotVisualLoading = ref(false)
  const shotSaving = ref(false)
  const shotLoading = ref(false)
  const videoLoading = ref(false)

  const assetsByType = computed<Record<AssetType, ExtractedAsset[]>>(() => ({
    CHARACTER: assets.value.filter((item) => item.assetType === 'CHARACTER'),
    BACKGROUND: assets.value.filter((item) => item.assetType === 'BACKGROUND'),
    PROP: assets.value.filter((item) => item.assetType === 'PROP'),
  }))

  const keyframesByAsset = computed<Record<string, KeyframeRecord[]>>(() =>
    keyframes.value.reduce<Record<string, KeyframeRecord[]>>((acc, item) => {
      acc[item.assetId] ??= []
      acc[item.assetId].push(item)
      return acc
    }, {}),
  )

  async function loadProjects(options?: { deleted?: boolean }) {
    listLoading.value = true
    try {
      const deleted = options?.deleted === true
      const list = await listScriptProjects({ deleted })
      projects.value = list
      listDeletedMode.value = deleted
      return list
    } finally {
      listLoading.value = false
    }
  }

  async function loadProject(projectId: string) {
    activeProjectId = projectId
    detailLoading.value = true
    try {
      const result = await getScriptProject(projectId)
      if (shouldApply(projectId)) currentProject.value = result
      return result
    } finally {
      detailLoading.value = false
    }
  }

  async function createFromText(payload: ScriptProjectCreateRequest) {
    createLoading.value = true
    try {
      const result = await createScriptProject(payload)
      currentProject.value = result
      await loadProjects()
      return result
    } finally {
      createLoading.value = false
    }
  }

  async function createFromUpload(payload: ScriptProjectUploadRequest) {
    createLoading.value = true
    try {
      const result = await uploadScriptProject(payload)
      currentProject.value = result
      await loadProjects()
      return result
    } finally {
      createLoading.value = false
    }
  }

  async function removeProject(projectId: string) {
    deletingProjectIds.value = [...deletingProjectIds.value, projectId]
    try {
      await deleteScriptProject(projectId)
      projects.value = projects.value.filter((item) => item.projectId !== projectId)
      if (currentProject.value?.project.projectId === projectId) currentProject.value = null
      await loadProjects({ deleted: listDeletedMode.value })
    } finally {
      deletingProjectIds.value = deletingProjectIds.value.filter((id) => id !== projectId)
    }
  }

  async function restoreProject(projectId: string) {
    restoringProjectIds.value = [...restoringProjectIds.value, projectId]
    try {
      await restoreScriptProject(projectId)
      projects.value = projects.value.filter((item) => item.projectId !== projectId)
      await loadProjects({ deleted: listDeletedMode.value })
    } finally {
      restoringProjectIds.value = restoringProjectIds.value.filter((id) => id !== projectId)
    }
  }

  async function loadScript(projectId: string) {
    const doc = await getScriptProjectDocument(projectId)
    if (shouldApply(projectId)) scriptPayload.value = doc
    return doc
  }

  async function refine(projectId: string) {
    refineLoading.value = true
    try {
      const doc = await refineScriptProject(projectId)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      refineLoading.value = false
    }
  }

  async function refineWithPrompt(projectId: string, briefPrompt: string) {
    refinePromptLoading.value = true
    try {
      const doc = await refineScriptProjectWithPrompt(projectId, briefPrompt)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      refinePromptLoading.value = false
    }
  }

  async function saveScript(projectId: string, payload: UpdateScriptRequest) {
    saveScriptLoading.value = true
    try {
      const doc = await updateScriptProjectDocument(projectId, payload)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      saveScriptLoading.value = false
    }
  }

  async function loadRevisions(projectId: string) {
    revisionLoading.value = true
    try {
      const list = await listScriptRevisions(projectId)
      if (shouldApply(projectId)) revisions.value = list
      return list
    } finally {
      revisionLoading.value = false
    }
  }

  async function restoreRevision(projectId: string, revisionId: string) {
    restoringRevisionId.value = revisionId
    try {
      const doc = await restoreScriptRevision(projectId, revisionId)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      restoringRevisionId.value = null
    }
  }

  async function optimizeScenes(projectId: string) {
    optimizeLoading.value = true
    try {
      const doc = await optimizeScriptScenes(projectId)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      optimizeLoading.value = false
    }
  }

  async function optimizeCharacters(projectId: string) {
    optimizeLoading.value = true
    try {
      const doc = await optimizeScriptCharacters(projectId)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      optimizeLoading.value = false
    }
  }

  async function optimizeProps(projectId: string) {
    optimizeLoading.value = true
    try {
      const doc = await optimizeScriptProps(projectId)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      optimizeLoading.value = false
    }
  }

  async function appendPreview(projectId: string, payload?: AppendScriptPreviewRequest): Promise<AppendScriptPreviewResponse> {
    appendLoading.value = true
    try {
      return await appendScriptProjectPreview(projectId, payload)
    } finally {
      appendLoading.value = false
    }
  }

  async function rewritePreview(projectId: string, payload: RewriteScriptPreviewRequest): Promise<RewriteScriptPreviewResponse> {
    rewriteLoading.value = true
    try {
      const result = await rewriteScriptProjectPreview(projectId, payload)
      rewritePreviewResult.value = result
      return result
    } finally {
      rewriteLoading.value = false
    }
  }

  async function applyRewrite(projectId: string, payload: RewriteScriptApplyRequest) {
    rewriteLoading.value = true
    try {
      const doc = await applyRewriteScriptProject(projectId, payload)
      scriptPayload.value = doc
      await loadProject(projectId)
      await loadRevisions(projectId)
      return doc
    } finally {
      rewriteLoading.value = false
    }
  }

  async function importScript(projectId: string, file: File, options?: { replaceName?: string; autoRefine?: boolean }) {
    importLoading.value = true
    try {
      await importScriptToProject(projectId, file, options)
      await loadProject(projectId)
      await loadScript(projectId)
      await loadRevisions(projectId)
    } finally {
      importLoading.value = false
    }
  }

  async function loadAssets(projectId: string) {
    const list = await getScriptAssets(projectId)
    if (shouldApply(projectId)) assets.value = list
    return list
  }

  async function extractAssets(projectId: string, type: 'characters' | 'backgrounds' | 'props') {
    assetLoading.value = true
    try {
      await extractScriptAssets(projectId, type)
      await loadAssets(projectId)
      await loadProject(projectId)
      return assets.value
    } finally {
      assetLoading.value = false
    }
  }

  async function saveAsset(projectId: string, assetId: string, payload: UpdateAssetRequest) {
    assetLoading.value = true
    try {
      const updated = await updateScriptAsset(projectId, assetId, payload)
      assets.value = assets.value.map((item) => (item.assetId === assetId ? updated : item))
      return updated
    } finally {
      assetLoading.value = false
    }
  }

  async function loadKeyframes(projectId: string) {
    const list = await getProjectKeyframes(projectId)
    if (shouldApply(projectId)) keyframes.value = list
    return list
  }

  async function generateKeyframes(projectId: string, assetId: string) {
    keyframeLoading.value = true
    try {
      await generateAssetKeyframes(projectId, assetId)
      await loadKeyframes(projectId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return keyframes.value
    } finally {
      keyframeLoading.value = false
    }
  }

  async function confirmKeyframe(projectId: string, keyframeId: string) {
    keyframeLoading.value = true
    try {
      await confirmProjectKeyframe(projectId, keyframeId)
      await loadKeyframes(projectId)
      await loadAssets(projectId)
      await loadProject(projectId)
    } finally {
      keyframeLoading.value = false
    }
  }

  async function regenerateKeyframe(projectId: string, keyframeId: string) {
    keyframeLoading.value = true
    try {
      await regenerateProjectKeyframe(projectId, keyframeId)
      await loadKeyframes(projectId)
      await loadAssets(projectId)
      await loadProject(projectId)
    } finally {
      keyframeLoading.value = false
    }
  }

  async function generateArtDirectionAction(projectId: string) {
    artDirectionLoading.value = true
    try {
      const result = await generateArtDirection(projectId)
      await loadProject(projectId)
      return result
    } finally {
      artDirectionLoading.value = false
    }
  }

  async function batchCharacterVisualPrompts(projectId: string) {
    visualPromptLoading.value = true
    try {
      const result = await batchGenerateCharacterVisualPrompts(projectId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateVisualPromptForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateAssetVisualPrompt(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateTurnaroundPlanForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateTurnaroundPlan(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateTurnaroundImageForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateTurnaroundImage(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateStoryboardPlanForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateStoryboardPlan(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function translateStoryboardPlanForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await translateStoryboardPlan(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function rewriteStoryboardPlanForAsset(projectId: string, assetId: string, payload: StoryboardRewriteRequest) {
    visualPromptLoading.value = true
    try {
      const result = await rewriteStoryboardPlan(projectId, assetId, payload)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateStoryboardImageForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateStoryboardImage(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateThreeViewForAsset(projectId: string, assetId: string) {
    visualPromptLoading.value = true
    try {
      const result = await generateThreeView(projectId, assetId)
      await loadAssets(projectId)
      await loadProject(projectId)
      return result
    } finally {
      visualPromptLoading.value = false
    }
  }

  async function generateGroupScenePrompt(projectId: string, payload: GenerateGroupSceneRequest): Promise<GroupSceneResponse> {
    groupSceneLoading.value = true
    try {
      const result = await generateGroupScene(projectId, payload)
      await loadProject(projectId)
      return result
    } finally {
      groupSceneLoading.value = false
    }
  }

  async function generateVisualPromptForShot(projectId: string, shotId: string): Promise<ShotVisualPromptResponse> {
    shotVisualLoading.value = true
    try {
      const result = await generateShotVisualPrompt(projectId, shotId)
      await loadShots(projectId)
      await loadProject(projectId)
      return result
    } finally {
      shotVisualLoading.value = false
    }
  }

  async function saveShot(projectId: string, shotId: string, payload: UpdateShotRequest) {
    shotSaving.value = true
    try {
      const updated = await updateScriptProjectShot(projectId, shotId, payload)
      shots.value = shots.value.map((item) => (item.shotId === shotId ? updated : item))
      await loadProject(projectId)
      return updated
    } finally {
      shotSaving.value = false
    }
  }

  async function applyStoryboardFirstFrameForShot(projectId: string, shotId: string, payload: ApplyStoryboardFirstFrameRequest) {
    shotSaving.value = true
    try {
      const result = await applyStoryboardFirstFrame(projectId, shotId, payload)
      await loadShots(projectId)
      await loadProject(projectId)
      return result
    } finally {
      shotSaving.value = false
    }
  }

  async function loadShots(projectId: string) {
    const list = await getScriptProjectShots(projectId)
    if (shouldApply(projectId)) shots.value = list
    return list
  }

  async function splitShots(projectId: string) {
    shotLoading.value = true
    try {
      const list = await splitScriptProjectShots(projectId)
      shots.value = list
      await loadProject(projectId)
      return list
    } finally {
      shotLoading.value = false
    }
  }

  async function loadVideoTasks(projectId: string) {
    const list = await getScriptProjectVideoTasks(projectId)
    if (shouldApply(projectId)) videoTasks.value = list
    return list
  }

  async function loadPipelineStatus(projectId: string) {
    const status = await getScriptProjectPipelineStatus(projectId)
    if (shouldApply(projectId)) pipelineStatus.value = status
    return status
  }

  async function startVideoGeneration(projectId: string) {
    videoLoading.value = true
    try {
      const status = await generateScriptProjectVideos(projectId)
      pipelineStatus.value = status
      await loadVideoTasks(projectId)
      await loadProject(projectId)
      startPolling(projectId)
      return status
    } finally {
      videoLoading.value = false
    }
  }

  async function retryVideoTask(projectId: string, segmentTaskId: string) {
    videoLoading.value = true
    try {
      const status = await retryScriptProjectVideoTask(projectId, segmentTaskId)
      pipelineStatus.value = status
      await loadVideoTasks(projectId)
      startPolling(projectId)
      return status
    } finally {
      videoLoading.value = false
    }
  }

  async function hydrate(projectId: string) {
    activeProjectId = projectId
    await Promise.all([
      loadProject(projectId),
      loadScript(projectId),
      loadAssets(projectId),
      loadKeyframes(projectId),
      loadShots(projectId),
      loadVideoTasks(projectId),
      loadPipelineStatus(projectId),
    ])
  }

  function startPolling(projectId: string) {
    stopPolling()
    activeProjectId = projectId
    const currentToken = ++pollToken
    const schedule = (delay: number) => {
      pollTimer = window.setTimeout(() => void tick(), delay)
    }
    const tick = async () => {
      if (currentToken !== pollToken) return
      if (pollingInFlight) {
        schedule(1000)
        return
      }
      pollingInFlight = true
      try {
        await Promise.all([loadVideoTasks(projectId), loadPipelineStatus(projectId), loadProject(projectId)])
        pollFailureCount = 0
        if (pipelineStatus.value?.projectStatus && pipelineStatus.value.projectStatus !== 'VIDEO_GENERATING') {
          stopPolling()
          return
        }
        schedule(3000)
      } catch {
        pollFailureCount += 1
        if (pollFailureCount >= 3) {
          stopPolling()
          return
        }
        schedule(Math.min(12000, 2000 * 2 ** (pollFailureCount - 1)))
      } finally {
        pollingInFlight = false
      }
    }
    schedule(1000)
  }

  function stopPolling() {
    pollToken += 1
    pollFailureCount = 0
    pollingInFlight = false
    if (pollTimer != null) {
      window.clearTimeout(pollTimer)
      pollTimer = null
    }
  }

  return {
    projects,
    currentProject,
    scriptPayload,
    assets,
    assetsByType,
    keyframes,
    keyframesByAsset,
    shots,
    videoTasks,
    pipelineStatus,
    listLoading,
    detailLoading,
    createLoading,
    refineLoading,
    refinePromptLoading,
    saveScriptLoading,
    revisions,
    revisionLoading,
    optimizeLoading,
    appendLoading,
    rewriteLoading,
    importLoading,
    restoringRevisionId,
    rewritePreviewResult,
    listDeletedMode,
    deletingProjectIds,
    restoringProjectIds,
    assetLoading,
    keyframeLoading,
    artDirectionLoading,
    visualPromptLoading,
    groupSceneLoading,
    shotVisualLoading,
    shotSaving,
    shotLoading,
    videoLoading,
    loadProjects,
    loadProject,
    createFromText,
    createFromUpload,
    removeProject,
    restoreProject,
    loadScript,
    refine,
    refineWithPrompt,
    saveScript,
    loadRevisions,
    restoreRevision,
    optimizeScenes,
    optimizeCharacters,
    optimizeProps,
    appendPreview,
    rewritePreview,
    applyRewrite,
    importScript,
    loadAssets,
    extractAssets,
    saveAsset,
    loadKeyframes,
    generateKeyframes,
    confirmKeyframe,
    regenerateKeyframe,
    generateArtDirectionAction,
    batchCharacterVisualPrompts,
    generateVisualPromptForAsset,
    generateTurnaroundPlanForAsset,
    generateTurnaroundImageForAsset,
    generateStoryboardPlanForAsset,
    translateStoryboardPlanForAsset,
    rewriteStoryboardPlanForAsset,
    generateStoryboardImageForAsset,
    generateThreeViewForAsset,
    generateGroupScenePrompt,
    generateVisualPromptForShot,
    saveShot,
    applyStoryboardFirstFrameForShot,
    loadShots,
    splitShots,
    loadVideoTasks,
    loadPipelineStatus,
    startVideoGeneration,
    retryVideoTask,
    hydrate,
    startPolling,
    stopPolling,
  }
})
