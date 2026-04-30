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
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { VideoReferenceImageField } from '@/components/workspace/VideoReferenceImageField'
import { useToast } from '@/context/ToastContext'
import { useAuthStore } from '@/stores/authStore'
import type { StoryboardLiteKeyframe, StoryboardLiteSession, StoryboardLiteVideoTask, VideoModelOptionDetail } from '@/types'

type ModelInputMode = 'preset' | 'custom'

type LiteModelPrefs = {
  imageModelInputMode: ModelInputMode
  videoModelInputMode: ModelInputMode
  imageModel: string
  videoModel: string
  customImageModel: string
  customVideoModel: string
}

const LITE_MODEL_PREFS_KEY = 'aigc_storyboard_lite_model_prefs_v1'

function labelForModel(modelName: string, details: VideoModelOptionDetail[] | undefined): string {
  const detail = details?.find((item) => item.modelName === modelName)
  if (detail?.displayName?.trim()) return `${detail.displayName}（${modelName}）`
  if (detail?.provider?.trim()) return `${detail.provider} · ${modelName}`
  return modelName
}

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
    // localStorage can be unavailable in privacy modes; losing preferences is non-critical.
  }
}

function pickPresetModel(current: string, preferred: string | undefined, serverDefault: string | undefined, options: string[]): string {
  const candidates = [current, preferred, serverDefault, options[0]].map((item) => item?.trim()).filter(Boolean) as string[]
  const listed = candidates.find((item) => options.includes(item))
  return listed || candidates[0] || ''
}

export function StoryboardLitePage() {
  const user = useAuthStore((s) => s.user)
  const { showToast } = useToast()
  const [initialModelPrefs] = useState(() => loadLiteModelPrefs())
  const [projectId, setProjectId] = useState('')
  const [title, setTitle] = useState('')
  const [scriptText, setScriptText] = useState('')
  const [threeViewPrompt, setThreeViewPrompt] = useState('')
  const [session, setSession] = useState<StoryboardLiteSession | null>(null)
  const [videoPrompt, setVideoPrompt] = useState('请基于参考图生成5秒电影感镜头。')
  const [videoSourceMode, setVideoSourceMode] = useState<'selected-keyframe' | 'custom-upload'>('selected-keyframe')
  const [customVideoReferenceImageUrl, setCustomVideoReferenceImageUrl] = useState('')
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
    await withBusy(async () => {
      await generateStoryboardLiteKeyframes(session.sessionId, {
        imageModel: finalImageModel || undefined,
        prompt: prompt || undefined,
      })
      await refreshSession(session.sessionId)
      showToast('三视图已生成', 'success')
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

  function renderVideo(task: StoryboardLiteVideoTask) {
    const src = resolveApiMediaUrl(task.videoUrl || resolveScriptFileUrl(task.resultVideoFileId))
    if (!src) return null
    return <video controls src={src} style={{ width: '100%', borderRadius: 10 }} />
  }

  if (!user) {
    return <EmptyState title="请先登录后访问" description="该独立闭环功能需要登录后使用。" />
  }

  return (
    <section className="tools-asset-visual-page">
      <div className="panel glass tools-asset-visual-actions">
        <div className="tools-asset-visual-section">
          <p className="eyebrow">第一步：创建会话</p>
          <div className="form-grid">
            <AppInput label="关联项目ID（可选）" value={projectId} onChange={(v) => setProjectId(String(v))} placeholder="可不填，按用户归属" />
            <AppInput label="会话标题（可选）" value={title} onChange={(v) => setTitle(String(v))} placeholder="剧本闭环测试" />
          </div>
          <AppButton loading={busy} onClick={() => void handleCreateSession()}>创建独立会话</AppButton>
        </div>
      </div>

      <div className="panel glass tools-asset-visual-actions">
        <div className="tools-asset-visual-section">
          <p className="eyebrow">第二步：剧本</p>
          <div className="row">
            <label className="field">
              <span className="label">三视图图片模型</span>
              <select value={imageModelInputMode} onChange={(e) => setImageModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从可用模型选择</option>
                <option value="custom">手动输入模型ID</option>
              </select>
            </label>
            {imageModelInputMode === 'preset' ? (
              <label className="field">
                <span className="label">当前图片模型</span>
                <select value={imageModel} disabled={modelsLoading} onChange={(e) => setImageModel(e.target.value)}>
                  {modelsLoading ? <option value="">加载中...</option> : null}
                  {imageModelOptions.length === 0 && !modelsLoading ? <option value="">暂无可用模型</option> : null}
                  {imageModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {labelForModel(item, imageModelDetails)}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <AppInput
                label="当前图片模型"
                value={customImageModel}
                onChange={(v) => setCustomImageModel(String(v))}
                placeholder="例如：doubao-seedream-5.0-lite"
              />
            )}
          </div>
          <div className="actions">
            <AppButton size="sm" variant="ghost" loading={modelsLoading} onClick={() => void handleRefreshModels()}>
              刷新模型列表
            </AppButton>
            <span className="muted">生成前可随时切换；选择会保存在本机，下次进入会自动带回。</span>
          </div>
          <p className="muted">若某个模型返回异常，可以换模型重新生成，不影响原工作台文生图入口。</p>
          <AppInput as="textarea" rows={8} label="剧本内容" value={scriptText} onChange={(v) => setScriptText(String(v))} placeholder="请输入剧本..." />
          <AppInput
            as="textarea"
            rows={4}
            label="三视图提示词（文生图，可选）"
            value={threeViewPrompt}
            onChange={(v) => setThreeViewPrompt(String(v))}
            placeholder="不填则自动基于剧本生成提示词"
          />
          <div className="actions">
            <AppButton loading={busy} onClick={() => void handleSaveScript()}>保存剧本</AppButton>
            <AppButton loading={busy} variant="primary" onClick={() => void handleGenerateKeyframes()}>生成三视图</AppButton>
          </div>
        </div>
      </div>

      <div className="panel glass tools-asset-visual-previews">
        <p className="eyebrow">第三步：关键帧确认</p>
        {session?.keyframes.length ? (
          <div className="assets-grid">
            {session.keyframes.map((item) => {
              const src = resolveApiMediaUrl(item.imageUrl || resolveScriptFileUrl(item.imageFileId))
              return (
                <article key={item.keyframeId} className="asset-card">
                  {src ? <img className="asset-image" src={src} alt={item.keyframeId} /> : <p className="muted">无预览</p>}
                  <p className="muted">{item.promptText || '关键帧提示词'}</p>
                  {item.modelName ? <p className="muted">模型：{item.modelName}</p> : null}
                  <AppButton
                    size="sm"
                    variant={item.selected ? 'primary' : 'ghost'}
                    loading={busy}
                    onClick={() => void handleConfirmKeyframe(item)}
                  >
                    {item.selected ? '已确认' : '确认此关键帧'}
                  </AppButton>
                </article>
              )
            })}
          </div>
        ) : (
          <p className="muted">尚未生成关键帧。</p>
        )}
      </div>

      <div className="panel glass tools-asset-visual-actions">
        <div className="tools-asset-visual-section">
          <p className="eyebrow">第四步：图生视频</p>
          <div className="shot-firstframe-bind" style={{ marginBottom: 16 }}>
            <div className="head-actions">
              <div>
                <p className="eyebrow">先选首帧来源</p>
                <p className="muted">默认使用已确认关键帧，也可以直接上传图片做图生视频，不必依赖九宫格流程。</p>
              </div>
            </div>
            <div className="shot-firstframe-tabs">
              <AppButton
                size="sm"
                variant={videoSourceMode === 'selected-keyframe' ? 'primary' : 'ghost'}
                onClick={() => setVideoSourceMode('selected-keyframe')}
              >
                已确认关键帧
              </AppButton>
              <AppButton
                size="sm"
                variant={videoSourceMode === 'custom-upload' ? 'primary' : 'ghost'}
                onClick={() => setVideoSourceMode('custom-upload')}
              >
                上传图片
              </AppButton>
            </div>
            {videoSourceMode === 'selected-keyframe' ? (
              selectedKeyframe ? (
                <div className="video-ref-preview">
                  <div className="video-ref-preview__img-wrap">
                    {selectedKeyframePreviewUrl ? (
                      <img src={selectedKeyframePreviewUrl} alt="已确认关键帧" className="video-ref-preview__img" />
                    ) : (
                      <span className="muted">无预览</span>
                    )}
                  </div>
                  <div className="video-ref-preview__meta">
                    <span className="video-ref-preview__status">当前将使用已确认关键帧作为首帧参考</span>
                    <span className="muted">{selectedKeyframe.promptText || '已确认关键帧'}</span>
                  </div>
                </div>
              ) : (
                <p className="muted">还没有已确认关键帧，可先在上一步确认，或切换到“上传图片”。</p>
              )
            ) : (
              <VideoReferenceImageField
                value={customVideoReferenceImageUrl}
                onChange={setCustomVideoReferenceImageUrl}
                label="上传图片作为视频首帧"
                placeholder="支持拖拽上传、粘贴图片 URL，或粘贴 data:image/... base64"
              />
            )}
          </div>
          <div className="row">
            <label className="field">
              <span className="label">视频模型输入方式</span>
              <select value={videoModelInputMode} onChange={(e) => setVideoModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从可用模型选择</option>
                <option value="custom">手动输入模型ID</option>
              </select>
            </label>
            {videoModelInputMode === 'preset' ? (
              <label className="field">
                <span className="label">当前视频模型</span>
                <select value={videoModel} disabled={modelsLoading} onChange={(e) => setVideoModel(e.target.value)}>
                  {modelsLoading ? <option value="">加载中...</option> : null}
                  {videoModelOptions.length === 0 && !modelsLoading ? <option value="">暂无可用模型</option> : null}
                  {videoModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {labelForModel(item, videoModelDetails)}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <AppInput
                label="当前视频模型"
                value={customVideoModel}
                onChange={(v) => setCustomVideoModel(String(v))}
                placeholder="例如：viduq3-turbo、doubao-seedance-2.0、video-kling-v3-6"
              />
            )}
          </div>
          <AppInput as="textarea" rows={3} label="视频提示词" value={videoPrompt} onChange={(v) => setVideoPrompt(String(v))} />
          <AppButton variant="primary" loading={busy} onClick={() => void handleGenerateVideo()}>
            {videoSourceMode === 'selected-keyframe' ? '从已确认关键帧生成视频' : '从上传图片生成视频'}
          </AppButton>
        </div>
      </div>

      <div className="panel glass tools-asset-visual-previews">
        <p className="eyebrow">生成结果</p>
        {session?.videoTasks.length ? (
          <div className="assets-grid">
            {session.videoTasks.map((task) => (
              <article key={task.videoTaskId} className="asset-card">
                <p className="muted">状态：{task.status}</p>
                {task.modelName ? <p className="muted">模型：{task.modelName}</p> : null}
                {renderVideo(task)}
              </article>
            ))}
          </div>
        ) : (
          <p className="muted">尚无视频任务。</p>
        )}
      </div>
    </section>
  )
}
