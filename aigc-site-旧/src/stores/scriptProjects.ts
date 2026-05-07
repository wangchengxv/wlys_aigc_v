import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
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
  listScriptProjects,
  refineScriptProject,
  regenerateProjectKeyframe,
  retryScriptProjectVideoTask,
  splitScriptProjectShots,
  updateScriptAsset,
  updateScriptProjectDocument,
  uploadScriptProject,
} from '@/services/api'
import type {
  AssetType,
  ExtractedAsset,
  KeyframeRecord,
  PipelineStatus,
  ScriptDocumentPayload,
  ScriptProjectAggregate,
  ScriptProjectCreateRequest,
  ScriptProjectSummary,
  ScriptProjectUploadRequest,
  StoryboardShot,
  UpdateAssetRequest,
  UpdateScriptRequest,
  VideoSegmentTask,
} from '@/types'

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
  const saveScriptLoading = ref(false)
  const assetLoading = ref(false)
  const keyframeLoading = ref(false)
  const shotLoading = ref(false)
  const videoLoading = ref(false)

  const pollTimer = ref<number | null>(null)

  const assetsByType = computed<Record<AssetType, ExtractedAsset[]>>(() => ({
    CHARACTER: assets.value.filter((item) => item.assetType === 'CHARACTER'),
    BACKGROUND: assets.value.filter((item) => item.assetType === 'BACKGROUND'),
    PROP: assets.value.filter((item) => item.assetType === 'PROP'),
  }))

  const keyframesByAsset = computed<Record<string, KeyframeRecord[]>>(() => {
    return keyframes.value.reduce<Record<string, KeyframeRecord[]>>((acc, item) => {
      acc[item.assetId] ??= []
      acc[item.assetId].push(item)
      return acc
    }, {})
  })

  async function loadProjects() {
    listLoading.value = true
    try {
      projects.value = await listScriptProjects()
      return projects.value
    } finally {
      listLoading.value = false
    }
  }

  async function loadProject(projectId: string) {
    detailLoading.value = true
    try {
      currentProject.value = await getScriptProject(projectId)
      return currentProject.value
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
    await deleteScriptProject(projectId)
    projects.value = projects.value.filter((item) => item.projectId !== projectId)
    if (currentProject.value?.project.projectId === projectId) {
      currentProject.value = null
    }
  }

  async function loadScript(projectId: string) {
    scriptPayload.value = await getScriptProjectDocument(projectId)
    return scriptPayload.value
  }

  async function refine(projectId: string) {
    refineLoading.value = true
    try {
      scriptPayload.value = await refineScriptProject(projectId)
      await loadProject(projectId)
      return scriptPayload.value
    } finally {
      refineLoading.value = false
    }
  }

  async function saveScript(projectId: string, payload: UpdateScriptRequest) {
    saveScriptLoading.value = true
    try {
      scriptPayload.value = await updateScriptProjectDocument(projectId, payload)
      await loadProject(projectId)
      return scriptPayload.value
    } finally {
      saveScriptLoading.value = false
    }
  }

  async function loadAssets(projectId: string) {
    assets.value = await getScriptAssets(projectId)
    return assets.value
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
    keyframes.value = await getProjectKeyframes(projectId)
    return keyframes.value
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

  async function loadShots(projectId: string) {
    shots.value = await getScriptProjectShots(projectId)
    return shots.value
  }

  async function splitShots(projectId: string) {
    shotLoading.value = true
    try {
      shots.value = await splitScriptProjectShots(projectId)
      await loadProject(projectId)
      return shots.value
    } finally {
      shotLoading.value = false
    }
  }

  async function loadVideoTasks(projectId: string) {
    videoTasks.value = await getScriptProjectVideoTasks(projectId)
    return videoTasks.value
  }

  async function loadPipelineStatus(projectId: string) {
    pipelineStatus.value = await getScriptProjectPipelineStatus(projectId)
    return pipelineStatus.value
  }

  async function startVideoGeneration(projectId: string) {
    videoLoading.value = true
    try {
      pipelineStatus.value = await generateScriptProjectVideos(projectId)
      await loadVideoTasks(projectId)
      await loadProject(projectId)
      startPolling(projectId)
      return pipelineStatus.value
    } finally {
      videoLoading.value = false
    }
  }

  async function retryVideoTask(projectId: string, segmentTaskId: string) {
    videoLoading.value = true
    try {
      pipelineStatus.value = await retryScriptProjectVideoTask(projectId, segmentTaskId)
      await loadVideoTasks(projectId)
      startPolling(projectId)
      return pipelineStatus.value
    } finally {
      videoLoading.value = false
    }
  }

  async function hydrate(projectId: string) {
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
    pollTimer.value = window.setInterval(async () => {
      try {
        await Promise.all([loadVideoTasks(projectId), loadPipelineStatus(projectId), loadProject(projectId)])
        const status = pipelineStatus.value?.projectStatus
        if (status && status !== 'VIDEO_GENERATING') {
          stopPolling()
        }
      } catch {
        stopPolling()
      }
    }, 3000)
  }

  function stopPolling() {
    if (pollTimer.value != null) {
      window.clearInterval(pollTimer.value)
      pollTimer.value = null
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
    saveScriptLoading,
    assetLoading,
    keyframeLoading,
    shotLoading,
    videoLoading,
    loadProjects,
    loadProject,
    createFromText,
    createFromUpload,
    removeProject,
    loadScript,
    refine,
    saveScript,
    loadAssets,
    extractAssets,
    saveAsset,
    loadKeyframes,
    generateKeyframes,
    confirmKeyframe,
    regenerateKeyframe,
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
