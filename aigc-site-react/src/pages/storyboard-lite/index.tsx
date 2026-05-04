import { useEffect, useMemo, useState } from 'react'
import {
  confirmStoryboardLiteKeyframe,
  createStoryboardLiteSession,
  getImageModels,
  getVideoModels,
  generateStoryboardLiteKeyframes,
  generateStoryboardLiteVideo,
  queryStoryboardLiteSession,
  resolveApiMediaUrl,
  resolveScriptFileUrl,
  saveStoryboardLiteScript,
} from '@/api'
import { EmptyState } from '@/components/common/EmptyState'
import { useToast } from '@/context/ToastContext'
import { buildEnabledModelConfigsFromOptions, sanitizeEnabledModelName } from '@/lib/scriptProject/modelSelection'
import { useAuthStore } from '@/stores/authStore'
import type { StoryboardLiteSession, StoryboardLiteKeyframe, VideoModelOptionDetail } from '@/types'

import './StoryboardLite.css'
import { StepIndicator } from './components/StepIndicator'
import { Step1Session } from './components/Step1Session'
import { Step2Script, type ModelInputMode } from './components/Step2Script'
import { Step3Keyframe } from './components/Step3Keyframe'
import { Step4Video } from './components/Step4Video'

type LiteModelPrefs = {
  imageModelInputMode: ModelInputMode
  videoModelInputMode: ModelInputMode
  imageModel: string
  videoModel: string
  customImageModel: string
  customVideoModel: string
}

const LITE_MODEL_PREFS_KEY = 'aigc_storyboard_lite_model_prefs_v1'

function loadLiteModelPrefs(): Partial<LiteModelPrefs> {
  if (typeof window === 'undefined') return {}
  try {
    const raw = window.localStorage.getItem(LITE_MODEL_PREFS_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Partial<LiteModelPrefs>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function saveLiteModelPrefs(prefs: LiteModelPrefs) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(LITE_MODEL_PREFS_KEY, JSON.stringify(prefs))
  } catch {
    // ignore
  }
}

function pickPresetModel(
  current: string,
  preferred: string | undefined,
  serverDefault: string | undefined,
  options: string[],
): string {
  const candidates = [current, preferred, serverDefault, options[0]].map((item) => item?.trim()).filter(Boolean) as string[]
  const listed = candidates.find((item) => options.includes(item))
  return listed || candidates[0] || ''
}

export function StoryboardLitePage() {
  const user = useAuthStore((s) => s.user)
  const { showToast } = useToast()
  const [initialModelPrefs] = useState(() => loadLiteModelPrefs())

  // Wizard state
  const [activeStep, setActiveStep] = useState(1)

  // Session state
  const [projectId, setProjectId] = useState('')
  const [title, setTitle] = useState('')
  const [session, setSession] = useState<StoryboardLiteSession | null>(null)

  // Script state
  const [scriptText, setScriptText] = useState('')
  const [threeViewPrompt, setThreeViewPrompt] = useState('')

  // Video state
  const [videoPrompt, setVideoPrompt] = useState('请基于参考图生成5秒电影感镜头。')
  const [videoSourceMode, setVideoSourceMode] = useState<'selected-keyframe' | 'custom-upload'>('selected-keyframe')
  const [customVideoReferenceImageUrl, setCustomVideoReferenceImageUrl] = useState('')

  // Model selection state
  const [imageModelInputMode, setImageModelInputMode] = useState<ModelInputMode>(initialModelPrefs.imageModelInputMode ?? 'preset')
  const [videoModelInputMode, setVideoModelInputMode] = useState<ModelInputMode>(initialModelPrefs.videoModelInputMode ?? 'preset')
  const [imageModel, setImageModel] = useState(initialModelPrefs.imageModel ?? '')
  const [videoModel, setVideoModel] = useState(initialModelPrefs.videoModel ?? '')
  const [customImageModel, setCustomImageModel] = useState(initialModelPrefs.customImageModel ?? '')
  const [customVideoModel, setCustomVideoModel] = useState(initialModelPrefs.customVideoModel ?? '')
  const [imageModelOptions, setImageModelOptions] = useState<string[]>([])
  const [videoModelOptions, setVideoModelOptions] = useState<string[]>([])
  const [imageModelDetails, setImageModelDetails] = useState<VideoModelOptionDetail[]>([])
  const [videoModelDetails, setVideoModelDetails] = useState<VideoModelOptionDetail[]>([])
  const [modelsLoading, setModelsLoading] = useState(false)
  const [busy, setBusy] = useState(false)

  const selectedKeyframe = useMemo(
    () => session?.keyframes.find((item) => item.selected) ?? null,
    [session?.keyframes],
  )
  const selectedKeyframePreviewUrl = useMemo(
    () => (selectedKeyframe ? resolveApiMediaUrl(selectedKeyframe.imageUrl || resolveScriptFileUrl(selectedKeyframe.imageFileId)) : ''),
    [selectedKeyframe],
  )

  const finalImageModel = useMemo(
    () => (imageModelInputMode === 'custom' ? customImageModel.trim() : imageModel.trim()),
    [customImageModel, imageModel, imageModelInputMode],
  )
  const finalVideoModel = useMemo(
    () => (videoModelInputMode === 'custom' ? customVideoModel.trim() : videoModel.trim()),
    [customVideoModel, videoModel, videoModelInputMode],
  )
  const enabledImageModels = useMemo(
    () => buildEnabledModelConfigsFromOptions('image', imageModelOptions, imageModelDetails),
    [imageModelDetails, imageModelOptions],
  )
  const safeImageModel = useMemo(
    () => sanitizeEnabledModelName(enabledImageModels, 'image', finalImageModel),
    [enabledImageModels, finalImageModel],
  )

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setModelsLoading(true)
      const [imageRes, videoRes] = await Promise.allSettled([getImageModels(), getVideoModels()])
      if (cancelled) return

      if (imageRes.status === 'fulfilled') {
        const options = Array.isArray(imageRes.value.options) ? imageRes.value.options : []
        const details = Array.isArray(imageRes.value.details) ? imageRes.value.details : []
        setImageModelOptions(options)
        setImageModelDetails(details)
        setImageModel((current) => pickPresetModel(current, initialModelPrefs.imageModel, imageRes.value.defaultModel, options))
      } else {
        showToast('图片模型列表加载失败，可手动输入模型ID继续测试', 'error')
      }

      if (videoRes.status === 'fulfilled') {
        const options = Array.isArray(videoRes.value.options) ? videoRes.value.options : []
        const details = Array.isArray(videoRes.value.details) ? videoRes.value.details : []
        setVideoModelOptions(options)
        setVideoModelDetails(details)
        setVideoModel((current) => pickPresetModel(current, initialModelPrefs.videoModel, videoRes.value.defaultModel, options))
      } else {
        showToast('视频模型列表加载失败，可手动输入模型ID继续测试', 'error')
      }

      setModelsLoading(false)
    })()
    return () => {
      cancelled = true
    }
  }, [initialModelPrefs.imageModel, initialModelPrefs.videoModel, showToast])

  useEffect(() => {
    saveLiteModelPrefs({
      imageModelInputMode,
      videoModelInputMode,
      imageModel,
      videoModel,
      customImageModel,
      customVideoModel,
    })
  }, [customImageModel, customVideoModel, imageModel, imageModelInputMode, videoModel, videoModelInputMode])

  async function handleRefreshModels() {
    setModelsLoading(true)
    const [imageRes, videoRes] = await Promise.allSettled([getImageModels(), getVideoModels()])

    if (imageRes.status === 'fulfilled') {
      const options = Array.isArray(imageRes.value.options) ? imageRes.value.options : []
      const details = Array.isArray(imageRes.value.details) ? imageRes.value.details : []
      setImageModelOptions(options)
      setImageModelDetails(details)
      setImageModel((current) => pickPresetModel(current, undefined, imageRes.value.defaultModel, options))
    } else {
      showToast('图片模型列表刷新失败', 'error')
    }

    if (videoRes.status === 'fulfilled') {
      const options = Array.isArray(videoRes.value.options) ? videoRes.value.options : []
      const details = Array.isArray(videoRes.value.details) ? videoRes.value.details : []
      setVideoModelOptions(options)
      setVideoModelDetails(details)
      setVideoModel((current) => pickPresetModel(current, undefined, videoRes.value.defaultModel, options))
    } else {
      showToast('视频模型列表刷新失败', 'error')
    }

    setModelsLoading(false)
    if (imageRes.status === 'fulfilled' && videoRes.status === 'fulfilled') {
      showToast('模型列表已刷新', 'success')
    }
  }

  async function withBusy(task: () => Promise<void>) {
    setBusy(true)
    try {
      await task()
    } finally {
      setBusy(false)
    }
  }

  async function refreshSession(sessionId: string) {
    const latest = await queryStoryboardLiteSession(sessionId)
    setSession(latest)
  }

  async function handleCreateSession() {
    await withBusy(async () => {
      const created = await createStoryboardLiteSession({ projectId: projectId || undefined, title: title || undefined })
      setSession(created)
      showToast('已创建独立闭环会话', 'success')
      setActiveStep(2)
    })
  }

  async function handleSaveScript() {
    if (!session?.sessionId) {
      showToast('请先创建会话', 'error')
      return
    }
    await withBusy(async () => {
      const latest = await saveStoryboardLiteScript(session.sessionId, { scriptText })
      setSession(latest)
      showToast('剧本已保存', 'success')
    })
  }

  async function handleGenerateKeyframes() {
    if (!session?.sessionId) {
      showToast('请先创建会话并保存剧本', 'error')
      return
    }
    const prompt = threeViewPrompt.trim()
    const currentScript = scriptText.trim()
    const savedScript = (session.latestScript || '').trim()
    if (!currentScript && !savedScript) {
      showToast('请先填写剧本内容', 'error')
      return
    }
    await withBusy(async () => {
      const activeSession =
        currentScript && currentScript !== savedScript
          ? await saveStoryboardLiteScript(session.sessionId, { scriptText: currentScript })
          : session
      setSession(activeSession)
      await generateStoryboardLiteKeyframes(activeSession.sessionId, {
        imageModel: safeImageModel,
        prompt: prompt || undefined,
      })
      await refreshSession(activeSession.sessionId)
      showToast('三视图已生成', 'success')
      setActiveStep(3)
    })
  }

  async function handleConfirmKeyframe(item: StoryboardLiteKeyframe) {
    if (!session?.sessionId) return
    await withBusy(async () => {
      await confirmStoryboardLiteKeyframe(session.sessionId, item.keyframeId)
      await refreshSession(session.sessionId)
      showToast('关键帧已确认', 'success')
    })
  }

  async function handleGenerateVideo() {
    if (!session?.sessionId) {
      showToast('请先创建会话', 'error')
      return
    }
    if (videoSourceMode === 'selected-keyframe' && !selectedKeyframe) {
      showToast('请先确认关键帧，或切换为自定义上传图片', 'error')
      return
    }
    if (videoSourceMode === 'custom-upload' && !customVideoReferenceImageUrl.trim()) {
      showToast('请先上传或粘贴首帧图片', 'error')
      return
    }
    await withBusy(async () => {
      await generateStoryboardLiteVideo(session.sessionId, {
        keyframeId: videoSourceMode === 'selected-keyframe' ? selectedKeyframe?.keyframeId : undefined,
        videoModel: finalVideoModel || undefined,
        prompt: videoPrompt,
        referenceImageUrl: videoSourceMode === 'custom-upload' ? customVideoReferenceImageUrl.trim() : undefined,
      })
      await refreshSession(session.sessionId)
      showToast('图生视频已提交并回写', 'success')
    })
  }

  if (!user) {
    return <EmptyState title="请先登录后访问" description="该独立闭环功能需要登录后使用。" />
  }

  const hasSession = !!session?.sessionId
  const hasKeyframes = (session?.keyframes?.length || 0) > 0
  const hasConfirmed = !!selectedKeyframe
  let maxAllowedStep = 1
  if (hasSession) maxAllowedStep = 2
  if (hasKeyframes) maxAllowedStep = 3
  if (hasConfirmed || videoSourceMode === 'custom-upload') maxAllowedStep = 4

  return (
    <div className="sl-wizard-container">
      <StepIndicator
        activeStep={activeStep}
        onStepChange={setActiveStep}
        maxAllowedStep={maxAllowedStep}
      />

      {activeStep === 1 && (
        <Step1Session
          projectId={projectId}
          setProjectId={setProjectId}
          title={title}
          setTitle={setTitle}
          onCreateSession={handleCreateSession}
          busy={busy}
          hasSession={hasSession}
          onNext={() => setActiveStep(2)}
        />
      )}

      {activeStep === 2 && (
        <Step2Script
          imageModelInputMode={imageModelInputMode}
          setImageModelInputMode={setImageModelInputMode}
          imageModel={imageModel}
          setImageModel={setImageModel}
          customImageModel={customImageModel}
          setCustomImageModel={setCustomImageModel}
          imageModelOptions={imageModelOptions}
          imageModelDetails={imageModelDetails}
          modelsLoading={modelsLoading}
          handleRefreshModels={handleRefreshModels}
          scriptText={scriptText}
          setScriptText={setScriptText}
          threeViewPrompt={threeViewPrompt}
          setThreeViewPrompt={setThreeViewPrompt}
          onSaveScript={handleSaveScript}
          onGenerateKeyframes={handleGenerateKeyframes}
          busy={busy}
          onNext={() => setActiveStep(3)}
          hasKeyframes={hasKeyframes}
        />
      )}

      {activeStep === 3 && (
        <Step3Keyframe
          keyframes={session?.keyframes || []}
          onConfirmKeyframe={handleConfirmKeyframe}
          busy={busy}
          onNext={() => setActiveStep(4)}
          hasConfirmed={hasConfirmed}
        />
      )}

      {activeStep === 4 && (
        <Step4Video
          videoSourceMode={videoSourceMode}
          setVideoSourceMode={setVideoSourceMode}
          selectedKeyframe={selectedKeyframe}
          selectedKeyframePreviewUrl={selectedKeyframePreviewUrl}
          customVideoReferenceImageUrl={customVideoReferenceImageUrl}
          setCustomVideoReferenceImageUrl={setCustomVideoReferenceImageUrl}
          videoModelInputMode={videoModelInputMode}
          setVideoModelInputMode={setVideoModelInputMode}
          videoModel={videoModel}
          setVideoModel={setVideoModel}
          customVideoModel={customVideoModel}
          setCustomVideoModel={setCustomVideoModel}
          videoModelOptions={videoModelOptions}
          videoModelDetails={videoModelDetails}
          modelsLoading={modelsLoading}
          videoPrompt={videoPrompt}
          setVideoPrompt={setVideoPrompt}
          onGenerateVideo={handleGenerateVideo}
          busy={busy}
          videoTasks={session?.videoTasks || []}
        />
      )}
    </div>
  )
}
