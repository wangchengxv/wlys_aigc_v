import { useEffect, useMemo, useState } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { VideoReferenceImageField } from '@/components/workspace/VideoReferenceImageField'
import { getImageModels, getVideoModels } from '@/api'
import { resolveVisualStyleForProject, styleSummaryShort } from '@/data/videoStylePresets'
import {
  buildWorkspaceAdvancedMediaPayload,
  emptyImageAdvancedForm,
  emptyViduForm,
  getWorkspaceAdvancedValidationError,
  isWorkspaceViduModelName,
  labelForImageAdvancedCapability,
  pickDefaultVideoForImageToVideo,
  resolveImageAdvancedCapability,
  workspaceVideoNeedsFirstFrameImage,
} from '@/lib/workspace/advancedMedia'
import type { ImageAdvancedFormState, ViduFormState, ViduTri } from '@/lib/workspace/advancedMedia'
import type { WorkspaceRouteVariant } from '@/routes/types'
import { useGenerationStore } from '@/stores/generationStore'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'
import { useStyleTemplateStore } from '@/stores/styleTemplateStore'
import type { GenerateMode, VideoModelOptionDetail } from '@/types'

const styles = ['科技风', '国潮风', '简约风', '可爱风', '商务风'] as const
const STYLE_GLOBAL = 'global'
const sizes = ['512x512', '768x768', '1024x1024']
const DEFAULT_IMAGE_MODEL = 'doubao-seedream-5-0-260128'
const DEFAULT_VIDEO_MODEL = 'doubao-seedance-1-5-pro-251215'
const textLengthOptions = [
  { label: '短', value: 'short' as const },
  { label: '中', value: 'medium' as const },
  { label: '长', value: 'long' as const },
]
/** 与后端 GenerationServiceImpl.MAX_VIDEO_MERGED_PROMPT_CHARS 一致 */
const MAX_VIDEO_MERGED_PROMPT_CHARS = 8000

function labelForVideoModel(modelName: string, details: VideoModelOptionDetail[] | undefined): string {
  const d = details?.find((x) => x.modelName === modelName)
  if (d?.displayName?.trim()) return `${d.displayName}（${modelName}）`
  if (d?.provider) return `${d.provider} · ${modelName}`
  return modelName
}

type Props = {
  onGenerated: () => void
  /** 由路由（如 /tools/image）注入的初始模式 */
  defaultMode?: GenerateMode
  workspaceVariant?: WorkspaceRouteVariant
  availableModes?: GenerateMode[]
  showQuickVideoAction?: boolean
}

const MODE_OPTIONS: Array<{ value: GenerateMode; label: string }> = [
  { value: 'text', label: '仅文本' },
  { value: 'image', label: '仅图片' },
  { value: 'both', label: '图文一起' },
  { value: 'video', label: '仅视频' },
]

export function PromptPanel({
  onGenerated,
  defaultMode,
  workspaceVariant = 'workspace',
  availableModes = MODE_OPTIONS.map((item) => item.value),
  showQuickVideoAction = true,
}: Props) {
  const store = useGenerationStore()
  const visualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const visualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const customVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const visualStyleLongTextMode = useGlobalSettingsStore((s) => s.visualStyleLongTextMode)
  const templates = useStyleTemplateStore((s) => s.templates)
  const [prompt, setPrompt] = useState('')
  const [styleSelection, setStyleSelection] = useState<string>(STYLE_GLOBAL)
  const [mode, setMode] = useState<GenerateMode>(() => defaultMode ?? 'both')
  const [imageSize, setImageSize] = useState('1024x1024')
  const [textLength, setTextLength] = useState<'short' | 'medium' | 'long'>('medium')
  const [count, setCount] = useState(1)
  const [imageModel, setImageModel] = useState('')
  const [customImageModel, setCustomImageModel] = useState('')
  const [videoModel, setVideoModel] = useState(DEFAULT_VIDEO_MODEL)
  const [customVideoModel, setCustomVideoModel] = useState('')
  const [error, setError] = useState('')
  const [imageModelLoading, setImageModelLoading] = useState(false)
  const [videoModelLoading, setVideoModelLoading] = useState(false)
  const [imageModelInputMode, setImageModelInputMode] = useState<'preset' | 'custom'>('preset')
  const [videoModelInputMode, setVideoModelInputMode] = useState<'preset' | 'custom'>('preset')
  const [imageModelOptions, setImageModelOptions] = useState<string[]>([])
  const [videoModelOptions, setVideoModelOptions] = useState<string[]>([])
  const [videoModelDetails, setVideoModelDetails] = useState<VideoModelOptionDetail[]>([])
  const [videoReferenceImageUrl, setVideoReferenceImageUrl] = useState('')
  const [viduForm, setViduForm] = useState<ViduFormState>(() => emptyViduForm())
  const [imageAdvancedForm, setImageAdvancedForm] = useState<ImageAdvancedFormState>(() => emptyImageAdvancedForm())

  const promptCount = prompt.length
  const promptPercent = Math.min(100, Math.round((promptCount / 500) * 100))
  const needsImageModel = mode === 'image' || mode === 'both'
  const needsVideoModel = mode === 'video'
  const needsTextLength = mode === 'text' || mode === 'both'

  const finalImageModel = useMemo(
    () => (imageModelInputMode === 'custom' ? customImageModel.trim() : imageModel),
    [imageModelInputMode, customImageModel, imageModel],
  )
  const finalVideoModel = useMemo(
    () => (videoModelInputMode === 'custom' ? customVideoModel.trim() : videoModel.trim()),
    [videoModelInputMode, customVideoModel, videoModel],
  )
  const imageAdvancedCapability = useMemo(() => resolveImageAdvancedCapability(finalImageModel), [finalImageModel])

  const globalStyleSlice = useMemo(
    () => ({
      visualStyleMode,
      visualStylePresetId,
      customVisualStyle,
      visualStyleLongTextMode,
    }),
    [visualStyleMode, visualStylePresetId, customVisualStyle, visualStyleLongTextMode],
  )

  const effectiveStyle = useMemo(() => {
    if (styleSelection === STYLE_GLOBAL) {
      return resolveVisualStyleForProject(globalStyleSlice, templates)
    }
    return styleSelection
  }, [styleSelection, globalStyleSlice, templates])

  const globalStyleLabel = useMemo(() => styleSummaryShort(globalStyleSlice, templates), [globalStyleSlice, templates])

  const modeOptions = useMemo(
    () => MODE_OPTIONS.filter((item) => availableModes.includes(item.value)),
    [availableModes],
  )

  useEffect(() => {
    if (defaultMode !== undefined) {
      setMode(defaultMode)
    }
  }, [defaultMode])

  useEffect(() => {
    if (modeOptions.length === 0) return
    if (!modeOptions.some((item) => item.value === mode)) {
      setMode(defaultMode ?? modeOptions[0].value)
    }
  }, [defaultMode, mode, modeOptions])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setImageModelLoading(true)
      setVideoModelLoading(true)
      const [imageRes, videoRes] = await Promise.allSettled([getImageModels(), getVideoModels()])

      if (cancelled) return

      if (imageRes.status === 'fulfilled') {
        const opts = Array.isArray(imageRes.value?.options) ? imageRes.value.options : []
        setImageModelOptions(opts)
        setImageModel(imageRes.value?.defaultModel || opts[0] || DEFAULT_IMAGE_MODEL)
      } else {
        setImageModelOptions([DEFAULT_IMAGE_MODEL])
        setImageModel(DEFAULT_IMAGE_MODEL)
        setError('图片模型列表加载失败，已使用默认模型')
      }

      if (videoRes.status === 'fulfilled') {
        const opts = Array.isArray(videoRes.value?.options) ? videoRes.value.options : []
        const det = videoRes.value?.details
        setVideoModelDetails(Array.isArray(det) ? det : [])
        setVideoModelOptions(opts)
        let sel = videoRes.value?.defaultModel || opts[0] || DEFAULT_VIDEO_MODEL
        if (workspaceVariant === 'image-to-video') {
          sel = pickDefaultVideoForImageToVideo(opts, sel)
        }
        setVideoModel(sel)
      } else {
        setVideoModelOptions([DEFAULT_VIDEO_MODEL])
        setVideoModel(DEFAULT_VIDEO_MODEL)
        setError((e) => (e ? `${e}；视频模型列表加载失败，已使用默认模型` : '视频模型列表加载失败，已使用默认模型'))
      }

      setImageModelLoading(false)
      setVideoModelLoading(false)
    })()
    return () => {
      cancelled = true
    }
  }, [workspaceVariant])

  async function runGenerate(overrideMode?: GenerateMode, styleOverride?: string) {
    if (store.loading) return
    const m = overrideMode ?? mode
    const needImg = m === 'image' || m === 'both'
    const needVid = m === 'video'
    const needTxtLen = m === 'text' || m === 'both'
    const globalResolvedStyle = resolveVisualStyleForProject(globalStyleSlice, templates)
    const styleForRequest = needVid
      ? globalResolvedStyle
      : styleOverride !== undefined
        ? styleOverride
        : effectiveStyle

    setError('')
    if (!prompt.trim()) {
      setError('请输入提示词后再生成')
      return
    }
    if (prompt.length > 500) {
      setError('提示词请控制在 500 字以内')
      return
    }
    if (needVid) {
      const mergedLen =
        globalResolvedStyle.trim().length > 0 && prompt.trim().length > 0
          ? globalResolvedStyle.length + 1 + prompt.trim().length
          : Math.max(globalResolvedStyle.trim().length, prompt.trim().length)
      if (mergedLen > MAX_VIDEO_MERGED_PROMPT_CHARS) {
        setError('视频提示词过长（全局风格与用户描述合并后超限），请缩短描述或调整全局设定')
        return
      }
    }
    if (/(暴恐|色情|违禁)/.test(prompt)) {
      setError('输入内容包含敏感词，请调整后重试')
      return
    }
    if (needImg && !finalImageModel) {
      setError('请选择图片模型或填写自定义模型ID')
      return
    }
    if (needVid && !finalVideoModel) {
      setError('请输入视频模型ID')
      return
    }
    const advancedValidationError = getWorkspaceAdvancedValidationError({
      needImg,
      needVid,
      imageAdvancedCapability,
      imageAdvancedForm,
      finalVideoModel,
      videoReferenceImageUrl,
    })
    if (advancedValidationError) {
      setError(advancedValidationError)
      return
    }

    if (overrideMode) setMode(overrideMode)

    try {
      const payload = buildWorkspaceAdvancedMediaPayload({
        mode: m,
        imageAdvancedCapability,
        imageAdvancedForm,
        videoReferenceImageUrl,
        finalVideoModel,
        viduForm,
      })
      await store.generate({
        prompt: prompt.trim(),
        mode: m,
        style: styleForRequest,
        imageSize,
        textLength: needTxtLen ? textLength : 'medium',
        count: Math.max(1, Math.min(4, count)),
        imageModel: needImg ? finalImageModel || undefined : undefined,
        videoModel: needVid ? finalVideoModel || undefined : undefined,
        advancedMedia: payload.advancedMedia,
        videoReferenceImageUrl: payload.videoReferenceImageUrl,
        videoViduOptions: payload.videoViduOptions,
      })
      onGenerated()
    } catch (e) {
      setError(e instanceof Error ? e.message : '生成失败，请稍后重试')
    }
  }

  function clearAll() {
    setPrompt('')
    setStyleSelection(STYLE_GLOBAL)
    setCustomImageModel('')
    setCustomVideoModel('')
    setVideoModel(DEFAULT_VIDEO_MODEL)
    setVideoReferenceImageUrl('')
    setViduForm(emptyViduForm())
    setImageAdvancedForm(emptyImageAdvancedForm())
    setError('')
    store.clearCurrent()
  }

  async function regenerate() {
    const t = store.currentTask
    if (!t) return
    setPrompt(t.prompt)
    const taskModel = t.imageModel
    setMode(t.mode)
    if (t.mode === 'video') {
      setStyleSelection(STYLE_GLOBAL)
    } else if (styles.some((s) => s === t.style)) {
      setStyleSelection(t.style)
    } else {
      setStyleSelection(STYLE_GLOBAL)
    }
    if (taskModel) {
      if (imageModelOptions.includes(taskModel)) {
        setImageModelInputMode('preset')
        setImageModel(taskModel)
        setCustomImageModel('')
      } else {
        setImageModelInputMode('custom')
        setCustomImageModel(taskModel)
      }
    }
    if (t.videoModel) {
      if (videoModelOptions.includes(t.videoModel)) {
        setVideoModelInputMode('preset')
        setVideoModel(t.videoModel)
        setCustomVideoModel('')
      } else {
        setVideoModelInputMode('custom')
        setCustomVideoModel(t.videoModel)
      }
    }
    await runGenerate(undefined, t.mode === 'video' ? undefined : t.style)
  }

  function quickGenerateVideo() {
    void runGenerate('video')
  }

  return (
    <section className="prompt panel glass">
      <AppInput value={prompt} onChange={(v) => setPrompt(String(v))} label="提示词" as="textarea" placeholder="例如：春季服装上新活动，生成年轻化营销文案与海报配图..." />
      <div className="progress">
        <span className="muted">字数：{promptCount}/500</span>
        <div className="bar">
          <span style={{ width: `${promptPercent}%` }} />
        </div>
      </div>

      <div className="field">
        <p className="label">快捷风格</p>
        <p className="hint muted" style={{ marginBottom: 'var(--space-sm)' }}>
          默认使用全局设定中的视觉风格；可切换为下方预设快捷标签。
          {mode === 'video' ? ' 仅视频模式下，生成将固定采用「全局设定」中的视觉风格（与快捷标签无关）。' : ''}
        </p>
        <select
          value={styleSelection}
          onChange={(e) => setStyleSelection(e.target.value)}
          aria-label="快捷风格"
        >
          <option value={STYLE_GLOBAL}>{`全局设定（${globalStyleLabel}）`}</option>
          {styles.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
      </div>

      <div className="row">
        {modeOptions.length > 1 ? (
          <label className="field">
            <span className="label">生成模式</span>
            <select value={mode} onChange={(e) => setMode(e.target.value as GenerateMode)}>
              {modeOptions.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <label className="field">
            <span className="label">当前模式</span>
            <div className="tips-box muted">
              {modeOptions[0]?.label ?? MODE_OPTIONS.find((item) => item.value === mode)?.label ?? '当前入口固定模式'}
            </div>
          </label>
        )}
        {needsImageModel ? (
          <label className="field">
            <span className="label">图片尺寸</span>
            <select value={imageSize} onChange={(e) => setImageSize(e.target.value)}>
              {sizes.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <label className="field">
            <span className="label">视频能力说明</span>
            <div className="tips-box muted">视频生成通常需要更长时间，建议等待 10~60 秒。</div>
          </label>
        )}
      </div>

      {mode !== 'video' ? (
        needsTextLength ? (
          <div className="row">
            <label className="field">
              <span className="label">文本长度</span>
              <select value={textLength} onChange={(e) => setTextLength(e.target.value as 'short' | 'medium' | 'long')}>
                {textLengthOptions.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
            <AppInput value={count} onChange={(v) => setCount(Number(v))} label="生成数量" type="number" min={1} max={4} />
          </div>
        ) : (
          <div className="row row--single">
            <AppInput value={count} onChange={(v) => setCount(Number(v))} label="生成数量" type="number" min={1} max={4} />
          </div>
        )
      ) : null}

      {needsImageModel ? (
        <div className="row">
          <label className="field">
            <span className="label">图片模型输入方式</span>
            <select value={imageModelInputMode} onChange={(e) => setImageModelInputMode(e.target.value as 'preset' | 'custom')}>
              <option value="preset">从预置模型选择</option>
              <option value="custom">手动输入模型ID</option>
            </select>
          </label>
          {imageModelInputMode === 'preset' ? (
            <label className="field">
              <span className="label">图片模型（默认豆包）</span>
              <select
                value={imageModel}
                aria-label="图片模型"
                disabled={imageModelLoading}
                onChange={(e) => setImageModel(e.target.value)}
              >
                {imageModelLoading ? <option value="">加载中...</option> : null}
                {imageModelOptions.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <AppInput
              value={customImageModel}
              onChange={(v) => setCustomImageModel(String(v))}
              label="自定义模型ID"
              placeholder="请输入模型ID，例如：doubao-seedream-5-0-260128"
            />
          )}
        </div>
      ) : null}

      {needsImageModel && imageAdvancedCapability ? (
        <details className="field image-advanced" style={{ marginTop: 'var(--space-sm)' }} open>
          <summary className="label" style={{ cursor: 'pointer' }}>
            图片高级参数：{labelForImageAdvancedCapability(imageAdvancedCapability)}
          </summary>
          <p className="hint muted" style={{ marginTop: 'var(--space-sm)' }}>
            当前根据模型名自动识别高级能力并序列化到 `advancedMedia.image`；仅提交与该能力相关的字段。
          </p>
          {imageAdvancedCapability === 'vidu_reference2image' ? (
            <VideoReferenceImageField
              value={imageAdvancedForm.reference2imageUrl}
              onChange={(v) => setImageAdvancedForm((f) => ({ ...f, reference2imageUrl: v }))}
              label="参考图（Vidu reference2image 必填）"
              placeholder="https://… 可访问图片；或粘贴 data:image/png;base64,..."
            />
          ) : null}
          {imageAdvancedCapability === 'kling_multi_reference' ? (
            <>
              <p className="hint muted" style={{ marginTop: 'var(--space-sm)' }}>
                Kling 多图参考生图支持 2~4 张参考图，前两张建议优先填写主体与风格。
              </p>
              {imageAdvancedForm.klingReferenceImages.map((value, index) => (
                <VideoReferenceImageField
                  key={`kling-ref-${index}`}
                  value={value}
                  onChange={(next) =>
                    setImageAdvancedForm((f) => ({
                      ...f,
                      klingReferenceImages: f.klingReferenceImages.map((item, itemIndex) => (itemIndex === index ? next : item)),
                    }))
                  }
                  label={`参考图 ${index + 1}${index < 2 ? '（建议必填）' : '（可选）'}`}
                  placeholder="https://… 可访问图片；或粘贴 data:image/png;base64,..."
                />
              ))}
            </>
          ) : null}
          {imageAdvancedCapability === 'outpaint' ? (
            <>
              <VideoReferenceImageField
                value={imageAdvancedForm.outpaintSourceImageUrl}
                onChange={(v) => setImageAdvancedForm((f) => ({ ...f, outpaintSourceImageUrl: v }))}
                label="原图（扩图必填）"
                placeholder="https://… 可访问图片；或粘贴 data:image/png;base64,..."
              />
              <div className="row">
                <AppInput
                  value={imageAdvancedForm.outpaintTop}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, outpaintTop: String(v) }))}
                  label="上扩边 top"
                  placeholder="非负整数"
                />
                <AppInput
                  value={imageAdvancedForm.outpaintRight}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, outpaintRight: String(v) }))}
                  label="右扩边 right"
                  placeholder="非负整数"
                />
              </div>
              <div className="row">
                <AppInput
                  value={imageAdvancedForm.outpaintBottom}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, outpaintBottom: String(v) }))}
                  label="下扩边 bottom"
                  placeholder="非负整数"
                />
                <AppInput
                  value={imageAdvancedForm.outpaintLeft}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, outpaintLeft: String(v) }))}
                  label="左扩边 left"
                  placeholder="非负整数"
                />
              </div>
            </>
          ) : null}
          {imageAdvancedCapability === 'omni' ? (
            <>
              <VideoReferenceImageField
                value={imageAdvancedForm.omniSourceImageUrl}
                onChange={(v) => setImageAdvancedForm((f) => ({ ...f, omniSourceImageUrl: v }))}
                label="输入图（Omni 必填）"
                placeholder="https://… 可访问图片；或粘贴 data:image/png;base64,..."
              />
              <div className="row">
                <AppInput
                  value={imageAdvancedForm.omniMode}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, omniMode: String(v) }))}
                  label="Omni 模式"
                  placeholder="如：smart / character / product（可选）"
                />
                <AppInput
                  value={imageAdvancedForm.omniSubjectPrompt}
                  onChange={(v) => setImageAdvancedForm((f) => ({ ...f, omniSubjectPrompt: String(v) }))}
                  label="主体描述"
                  placeholder="请描述主体、材质或保留重点"
                />
              </div>
            </>
          ) : null}
        </details>
      ) : null}

      {needsVideoModel ? (
        <>
          <div className="row">
            <label className="field">
              <span className="label">视频模型输入方式</span>
              <select value={videoModelInputMode} onChange={(e) => setVideoModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从预置模型选择</option>
                <option value="custom">手动输入模型ID</option>
              </select>
            </label>
            {videoModelInputMode === 'preset' ? (
              <label className="field">
                <span className="label">视频模型</span>
                <select
                  value={videoModel}
                  aria-label="视频模型"
                  disabled={videoModelLoading}
                  onChange={(e) => setVideoModel(e.target.value)}
                >
                  {videoModelLoading ? <option value="">加载中...</option> : null}
                  {videoModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {labelForVideoModel(item, videoModelDetails)}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <AppInput
                value={customVideoModel}
                onChange={(v) => setCustomVideoModel(String(v))}
                label="视频模型ID"
                placeholder="如：doubao-seedance-1-5-pro-251215、viduq3-turbo、kling-v2-6、Wan2.1-I2V-14B-720P"
              />
            )}
          </div>
          <p className="hint muted" style={{ marginTop: 'calc(-1 * var(--space-xs))' }}>
            可随时切换：方舟文生视频、OneLink Kling 文/图生视频、OneLink Vidu 图生视频、Moark 等；文档：{' '}
            <a href="https://docs.onelinkai.cloud/440769864e0" target="_blank" rel="noopener noreferrer">
              OneLink 图生视频 API
            </a>
            。
          </p>
        </>
      ) : null}

      {mode === 'video' ? (
        <VideoReferenceImageField
          value={videoReferenceImageUrl}
          onChange={(v) => setVideoReferenceImageUrl(v)}
          label={
            workspaceVideoNeedsFirstFrameImage(finalVideoModel)
              ? '参考图（图生视频必填：Moark / Vidu）'
              : '参考图 URL（Kling 图生视频可选；方舟文生视频通常可不填）'
          }
          placeholder="https://… 可访问图片；或粘贴 data:image/png;base64,...（上传将自动生成 Base64，Vidu 可直接使用）"
          showHttpOnlyHint={
            workspaceVideoNeedsFirstFrameImage(finalVideoModel) &&
            !isWorkspaceViduModelName(finalVideoModel) &&
            (finalVideoModel.toLowerCase().includes('moark') ||
              (finalVideoModel.toLowerCase().includes('wan') && finalVideoModel.toLowerCase().includes('i2v')))
          }
        />
      ) : null}

      {needsVideoModel && isWorkspaceViduModelName(finalVideoModel) ? (
        <details className="field vidu-advanced" style={{ marginTop: 'var(--space-sm)' }}>
          <summary className="label" style={{ cursor: 'pointer' }}>
            Vidu / OneLink 高级参数（可选）
          </summary>
          <div className="row" style={{ marginTop: 'var(--space-sm)' }}>
            <AppInput
              value={viduForm.duration}
              onChange={(v) => setViduForm((f) => ({ ...f, duration: String(v) }))}
              label="时长 duration（秒）"
              placeholder="留空则服务端默认"
            />
            <AppInput
              value={viduForm.seed}
              onChange={(v) => setViduForm((f) => ({ ...f, seed: String(v) }))}
              label="随机种子 seed"
              placeholder="0 或留空为随机"
            />
          </div>
          <div className="row">
            <label className="field">
              <span className="label">分辨率 resolution</span>
              <select
                value={viduForm.resolution}
                onChange={(e) => setViduForm((f) => ({ ...f, resolution: e.target.value }))}
              >
                <option value="">默认</option>
                <option value="360p">360p</option>
                <option value="480p">480p</option>
                <option value="720p">720p</option>
                <option value="1080p">1080p</option>
              </select>
            </label>
            <label className="field">
              <span className="label">运动幅度 movement_amplitude</span>
              <select
                value={viduForm.movement_amplitude}
                onChange={(e) => setViduForm((f) => ({ ...f, movement_amplitude: e.target.value }))}
              >
                <option value="">默认</option>
                <option value="auto">auto</option>
                <option value="small">small</option>
                <option value="medium">medium</option>
                <option value="large">large</option>
              </select>
            </label>
          </div>
          <div className="row">
            <label className="field">
              <span className="label">音视频直出 audio</span>
              <select
                value={viduForm.audio}
                onChange={(e) => setViduForm((f) => ({ ...f, audio: e.target.value as ViduTri }))}
              >
                <option value="">默认</option>
                <option value="true">开启</option>
                <option value="false">关闭</option>
              </select>
            </label>
            <label className="field">
              <span className="label">音频类型 audio_type</span>
              <select
                value={viduForm.audio_type}
                onChange={(e) => setViduForm((f) => ({ ...f, audio_type: e.target.value }))}
              >
                <option value="">默认</option>
                <option value="all">all</option>
                <option value="speech_only">speech_only</option>
                <option value="sound_effect_only">sound_effect_only</option>
              </select>
            </label>
          </div>
          <div className="row">
            <label className="field">
              <span className="label">错峰 off_peak</span>
              <select
                value={viduForm.off_peak}
                onChange={(e) => setViduForm((f) => ({ ...f, off_peak: e.target.value as ViduTri }))}
              >
                <option value="">默认</option>
                <option value="true">是</option>
                <option value="false">否</option>
              </select>
            </label>
            <label className="field">
              <span className="label">水印 watermark</span>
              <select
                value={viduForm.watermark}
                onChange={(e) => setViduForm((f) => ({ ...f, watermark: e.target.value as ViduTri }))}
              >
                <option value="">默认</option>
                <option value="true">添加</option>
                <option value="false">不添加</option>
              </select>
            </label>
          </div>
          <div className="row">
            <AppInput
              value={viduForm.wm_position}
              onChange={(v) => setViduForm((f) => ({ ...f, wm_position: String(v) }))}
              label="水印位置 wm_position（1–4）"
              placeholder="1左上 2右上 3右下 4左下"
            />
            <AppInput
              value={viduForm.wm_url}
              onChange={(v) => setViduForm((f) => ({ ...f, wm_url: String(v) }))}
              label="水印图 wm_url"
              placeholder="可选 URL"
            />
          </div>
          <div className="row">
            <AppInput
              value={viduForm.voice_id}
              onChange={(v) => setViduForm((f) => ({ ...f, voice_id: String(v) }))}
              label="音色 voice_id（q3 不生效）"
              placeholder="可选"
            />
            <label className="field">
              <span className="label">推荐提示词 is_rec（+10 积分）</span>
              <select
                value={viduForm.is_rec}
                onChange={(e) => setViduForm((f) => ({ ...f, is_rec: e.target.value as ViduTri }))}
              >
                <option value="">默认</option>
                <option value="true">开启</option>
                <option value="false">关闭</option>
              </select>
            </label>
          </div>
          <div className="row">
            <label className="field">
              <span className="label">背景音乐 bgm（q3 不生效）</span>
              <select
                value={viduForm.bgm}
                onChange={(e) => setViduForm((f) => ({ ...f, bgm: e.target.value as ViduTri }))}
              >
                <option value="">默认</option>
                <option value="true">开启</option>
                <option value="false">关闭</option>
              </select>
            </label>
            <AppInput
              value={viduForm.callback_url}
              onChange={(v) => setViduForm((f) => ({ ...f, callback_url: String(v) }))}
              label="回调 callback_url"
              placeholder="可选"
            />
          </div>
          <AppInput
            value={viduForm.payload}
            onChange={(v) => setViduForm((f) => ({ ...f, payload: String(v) }))}
            label="透传 payload"
            as="textarea"
            placeholder="可选"
          />
          <AppInput
            value={viduForm.meta_data}
            onChange={(v) => setViduForm((f) => ({ ...f, meta_data: String(v) }))}
            label="元数据 meta_data（JSON 字符串）"
            as="textarea"
            placeholder="可选"
          />
        </details>
      ) : null}

      {error ? <p className="error">{error}</p> : null}

      <div className="bottom-actions">
        <div className="actions">
          <AppButton variant="primary" loading={store.loading} onClick={() => void runGenerate()}>
            {store.loading ? '生成中...' : '开始生成'}
          </AppButton>
          <AppButton disabled={store.loading || !store.currentTask} onClick={() => void regenerate()}>
            再来一版
          </AppButton>
          <AppButton disabled={store.loading} onClick={clearAll}>
            清空
          </AppButton>
        </div>
        {showQuickVideoAction ? (
          <div className={mode === 'video' ? 'feature-actions feature-actions--with-count' : 'feature-actions'}>
            {mode === 'video' ? (
              <>
                <div className="feature-actions-main">
                  <AppButton variant="primary" loading={store.loading} onClick={() => void quickGenerateVideo()}>
                    {store.loading ? '视频生成中...' : 'AI视频生成'}
                  </AppButton>
                  <p className="muted">一键切换到视频模式并直接生成</p>
                </div>
                <div className="feature-actions-count">
                  <AppInput value={count} onChange={(v) => setCount(Number(v))} label="生成数量" type="number" min={1} max={4} />
                </div>
              </>
            ) : (
              <>
                <AppButton variant="primary" disabled={store.loading} onClick={() => void quickGenerateVideo()}>
                  AI视频生成
                </AppButton>
                <p className="muted">一键切换到视频模式并直接生成</p>
              </>
            )}
          </div>
        ) : null}
      </div>
    </section>
  )
}
