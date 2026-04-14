import { useEffect, useMemo, useState } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { getImageModels, getVideoModels } from '@/api'
import { resolveVisualStyleForProject, styleSummaryShort } from '@/data/videoStylePresets'
import { useGenerationStore } from '@/stores/generationStore'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'
import type { GenerateMode } from '@/types'

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

/** 与后端 isViduWorkspaceModel / Moark 对齐：Vidu 用模型名前缀 `vidu`，避免子串误匹配 */
function workspaceVideoNeedsFirstFrameImage(videoModel: string): boolean {
  const m = videoModel.trim().toLowerCase()
  if (!m) return false
  if (m.startsWith('vidu')) return true
  if (m.includes('wan') && m.includes('i2v')) return true
  if (m.includes('moark')) return true
  return false
}

function isValidWorkspaceVideoRef(ref: string): boolean {
  const t = ref.trim()
  if (!t) return false
  if (t.startsWith('http://') || t.startsWith('https://')) return true
  return /^data:image\//i.test(t) && t.includes('base64')
}

type Props = {
  onGenerated: () => void
  /** 由路由（如 /tools/image）注入的初始模式 */
  defaultMode?: GenerateMode
}

export function PromptPanel({ onGenerated, defaultMode }: Props) {
  const store = useGenerationStore()
  const visualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const visualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const customVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const visualStyleLongTextMode = useGlobalSettingsStore((s) => s.visualStyleLongTextMode)
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
  const [videoReferenceImageUrl, setVideoReferenceImageUrl] = useState('')

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
      return resolveVisualStyleForProject(globalStyleSlice)
    }
    return styleSelection
  }, [styleSelection, globalStyleSlice])

  const globalStyleLabel = useMemo(() => styleSummaryShort(globalStyleSlice), [globalStyleSlice])

  useEffect(() => {
    if (defaultMode !== undefined) {
      setMode(defaultMode)
    }
  }, [defaultMode])

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
        setVideoModelOptions(opts)
        setVideoModel(videoRes.value?.defaultModel || opts[0] || DEFAULT_VIDEO_MODEL)
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
  }, [])

  async function runGenerate(overrideMode?: GenerateMode, styleOverride?: string) {
    if (store.loading) return
    const m = overrideMode ?? mode
    const needImg = m === 'image' || m === 'both'
    const needVid = m === 'video'
    const needTxtLen = m === 'text' || m === 'both'
    const globalResolvedStyle = resolveVisualStyleForProject(globalStyleSlice)
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
    if (needVid && workspaceVideoNeedsFirstFrameImage(finalVideoModel) && !isValidWorkspaceVideoRef(videoReferenceImageUrl)) {
      setError('图生视频需填写参考图：可访问的 http(s) 图片地址，或 data:image/...;base64,...（Moark / Vidu）')
      return
    }

    if (overrideMode) setMode(overrideMode)

    try {
      await store.generate({
        prompt: prompt.trim(),
        mode: m,
        style: styleForRequest,
        imageSize,
        textLength: needTxtLen ? textLength : 'medium',
        count: Math.max(1, Math.min(4, count)),
        imageModel: needImg ? finalImageModel || undefined : undefined,
        videoModel: needVid ? finalVideoModel || undefined : undefined,
        videoReferenceImageUrl:
          m === 'video' && videoReferenceImageUrl.trim() ? videoReferenceImageUrl.trim() : undefined,
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
        <label className="field">
          <span className="label">生成模式</span>
          <select value={mode} onChange={(e) => setMode(e.target.value as GenerateMode)}>
            <option value="text">仅文本</option>
            <option value="image">仅图片</option>
            <option value="both">图文一起</option>
            <option value="video">仅视频</option>
          </select>
        </label>
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
                title={imageModel}
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

      {needsVideoModel ? (
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
              <span className="label">视频模型（即梦）</span>
              <select
                value={videoModel}
                title={videoModel}
                aria-label="视频模型"
                disabled={videoModelLoading}
                onChange={(e) => setVideoModel(e.target.value)}
              >
                {videoModelLoading ? <option value="">加载中...</option> : null}
                {videoModelOptions.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <AppInput
              value={customVideoModel}
              onChange={(v) => setCustomVideoModel(String(v))}
              label="视频模型ID"
              placeholder="请输入视频模型ID，例如：doubao-seedance-1-5-pro-251215"
            />
          )}
        </div>
      ) : null}

      {mode === 'video' ? (
        <AppInput
          value={videoReferenceImageUrl}
          onChange={(v) => setVideoReferenceImageUrl(String(v))}
          label={
            workspaceVideoNeedsFirstFrameImage(finalVideoModel)
              ? '参考图（图生视频必填：Moark / Vidu）'
              : '参考图 URL（方舟文生视频通常可不填）'
          }
          placeholder="https://… 可访问图片；或 data:image/png;base64,...（Vidu 支持）"
        />
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
      </div>
    </section>
  )
}
