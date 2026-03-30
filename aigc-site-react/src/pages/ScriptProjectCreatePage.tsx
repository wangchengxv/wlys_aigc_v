import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getModels } from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { useToast } from '@/context/ToastContext'
import { resolveVisualStyleForProject, styleSummaryShort } from '@/data/videoStylePresets'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { GlobalAspectRatio, ModelConfig } from '@/types'

const ASPECT_PRESETS: GlobalAspectRatio[] = ['16:9', '9:16', '4:3', '3:4', '1:1', '21:9']

function hasCapability(model: ModelConfig, capability: 'text' | 'image' | 'video') {
  const caps = Array.isArray(model.metadata?.capabilities) ? (model.metadata.capabilities as string[]) : []
  return caps.map((item) => String(item).toLowerCase()).includes(capability)
}

function normalizeExplicitValue(value: string, mode: 'preset' | 'custom') {
  if (mode === 'custom') return value.trim()
  return value
}

export function ScriptProjectCreatePage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const createFromText = useScriptProjectStore((s) => s.createFromText)
  const createFromUpload = useScriptProjectStore((s) => s.createFromUpload)
  const createLoading = useScriptProjectStore((s) => s.createLoading)

  const [mode, setMode] = useState<'text' | 'upload'>('text')
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [sourceText, setSourceText] = useState('')
  const [visualStyle, setVisualStyle] = useState(() => resolveVisualStyleForProject(useGlobalSettingsStore.getState()))
  const [aspectRatio, setAspectRatio] = useState<GlobalAspectRatio>(() => useGlobalSettingsStore.getState().aspectRatio)
  const scriptType = useGlobalSettingsStore((s) => s.scriptType)
  const modelStrategy = useGlobalSettingsStore((s) => s.modelStrategy)
  const creationMode = useGlobalSettingsStore((s) => s.creationMode)
  const storyboardLayout = useGlobalSettingsStore((s) => s.storyboardLayout)
  const globalTargetDurationSec = useGlobalSettingsStore((s) => s.targetDurationSec)
  const styleSummary = useGlobalSettingsStore((s) =>
    styleSummaryShort({
      visualStyleMode: s.visualStyleMode,
      visualStylePresetId: s.visualStylePresetId,
      customVisualStyle: s.customVisualStyle,
      visualStyleLongTextMode: s.visualStyleLongTextMode,
    }),
  )
  const [targetDuration, setTargetDuration] = useState(() => globalTargetDurationSec)
  const [language, setLanguage] = useState('中文')
  const [explicitTextModel, setExplicitTextModel] = useState('')
  const [explicitImageModel, setExplicitImageModel] = useState('')
  const [explicitVideoModel, setExplicitVideoModel] = useState('')
  const [error, setError] = useState('')
  const [warnings, setWarnings] = useState<string[]>([])
  const [modelLoading, setModelLoading] = useState(false)
  const [modelOptions, setModelOptions] = useState<ModelConfig[]>([])
  const [textModelInputMode, setTextModelInputMode] = useState<'preset' | 'custom'>('preset')
  const [imageModelInputMode, setImageModelInputMode] = useState<'preset' | 'custom'>('preset')
  const [videoModelInputMode, setVideoModelInputMode] = useState<'preset' | 'custom'>('preset')

  const textModelOptions = useMemo(
    () => modelOptions.filter((item) => hasCapability(item, 'text')).map((item) => item.modelName),
    [modelOptions],
  )
  const imageModelOptions = useMemo(
    () => modelOptions.filter((item) => hasCapability(item, 'image')).map((item) => item.modelName),
    [modelOptions],
  )
  const videoModelOptions = useMemo(
    () => modelOptions.filter((item) => hasCapability(item, 'video')).map((item) => item.modelName),
    [modelOptions],
  )

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setModelLoading(true)
      try {
        const allModels = await getModels()
        if (cancelled) return
        const enabled = allModels.filter((item) => item.enabled)
        setModelOptions(enabled)
        const te = enabled.filter((item) => hasCapability(item, 'text')).map((item) => item.modelName)
        const im = enabled.filter((item) => hasCapability(item, 'image')).map((item) => item.modelName)
        const vm = enabled.filter((item) => hasCapability(item, 'video')).map((item) => item.modelName)
        setExplicitTextModel((prev) => prev || te[0] || '')
        setExplicitImageModel((prev) => prev || im[0] || '')
        setExplicitVideoModel((prev) => prev || vm[0] || '')
      } catch (e) {
        if (!cancelled) setWarnings((w) => [...w, e instanceof Error ? `模型列表加载失败：${e.message}` : '模型列表加载失败'])
      } finally {
        if (!cancelled) setModelLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setWarnings([])
    const textM = normalizeExplicitValue(explicitTextModel, textModelInputMode)
    const imageM = normalizeExplicitValue(explicitImageModel, imageModelInputMode)
    const videoM = normalizeExplicitValue(explicitVideoModel, videoModelInputMode)
    const nextWarnings: string[] = []
    if (textModelInputMode === 'custom' && textM && !textModelOptions.includes(textM)) {
      nextWarnings.push('文本模型不在已配置模型中，运行时可能走系统 fallback。')
    }
    if (imageModelInputMode === 'custom' && imageM && !imageModelOptions.includes(imageM)) {
      nextWarnings.push('图片模型不在已配置模型中，运行时可能走系统 fallback。')
    }
    if (videoModelInputMode === 'custom' && videoM && !videoModelOptions.includes(videoM)) {
      nextWarnings.push('视频模型不在已配置模型中，运行时可能走系统 fallback。')
    }
    setWarnings(nextWarnings)

    try {
      let result
      const base = {
        name,
        visualStyle,
        aspectRatio,
        targetDuration,
        language,
        explicitTextModel: textM || undefined,
        explicitImageModel: imageM || undefined,
        explicitVideoModel: videoM || undefined,
      }
      if (mode === 'text') {
        if (!sourceText.trim()) {
          setError('请输入剧本文本')
          return
        }
        result = await createFromText({ ...base, sourceText: sourceText.trim() })
      } else {
        if (!uploadFile) {
          setError('请选择要上传的剧本文件')
          return
        }
        result = await createFromUpload({ ...base, file: uploadFile })
      }
      showToast('项目创建成功', 'success')
      navigate(`/script-projects/${result.project.projectId}/preview`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建项目失败')
    }
  }

  return (
    <section className="script-create-page">
      <div className="mode-switch panel glass">
        <button type="button" className={mode === 'text' ? 'active' : ''} onClick={() => setMode('text')}>
          粘贴文本
        </button>
        <button type="button" className={mode === 'upload' ? 'active' : ''} onClick={() => setMode('upload')}>
          上传文件
        </button>
      </div>

      <form className="form panel glass" onSubmit={(e) => void submit(e)}>
        <div className="global-prefs-hint panel glass">
          <p className="muted">
            当前全局偏好：剧本「{scriptType}」· 风格「{styleSummary}」· 策略「{modelStrategy}」· 创作「{creationMode}」· 分镜「{storyboardLayout}」。
            <Link to="/global-settings">去全局设定</Link>
          </p>
        </div>
        <div className="grid">
          <AppInput value={name} onChange={(v) => setName(String(v))} label="项目名称" placeholder="例如：都市悬疑短片" />
          <AppInput value={visualStyle} onChange={(v) => setVisualStyle(String(v))} label="视觉风格" placeholder="例如：电影感写实 / 国风 / 赛博朋克" />
          <label className="model-field">
            <span className="label">视频比例</span>
            <select value={aspectRatio} onChange={(e) => setAspectRatio(e.target.value as GlobalAspectRatio)}>
              {ASPECT_PRESETS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
            <span className="hint muted">默认取自全局设定，可按项目单独修改。</span>
          </label>
          <AppInput value={targetDuration} onChange={(v) => setTargetDuration(Number(v))} label="目标时长（秒）" type="number" min={1} max={600} />
          <AppInput value={language} onChange={(v) => setLanguage(String(v))} label="输出语言" placeholder="中文" />
          <label className="model-field">
            <span className="label">文本模型（可选）</span>
            <div className="model-row">
              <select value={textModelInputMode} onChange={(e) => setTextModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从已配置模型选择</option>
                <option value="custom">手动输入</option>
              </select>
              {textModelInputMode === 'preset' ? (
                <select value={explicitTextModel} disabled={modelLoading} onChange={(e) => setExplicitTextModel(e.target.value)}>
                  <option value="">未填则走系统路由</option>
                  {textModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              ) : (
                <AppInput value={explicitTextModel} onChange={(v) => setExplicitTextModel(String(v))} placeholder="请输入模型标识（modelName）" />
              )}
            </div>
          </label>
          <label className="model-field">
            <span className="label">图片模型（可选）</span>
            <div className="model-row">
              <select value={imageModelInputMode} onChange={(e) => setImageModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从已配置模型选择</option>
                <option value="custom">手动输入</option>
              </select>
              {imageModelInputMode === 'preset' ? (
                <select value={explicitImageModel} disabled={modelLoading} onChange={(e) => setExplicitImageModel(e.target.value)}>
                  <option value="">未填则走系统路由</option>
                  {imageModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              ) : (
                <AppInput value={explicitImageModel} onChange={(v) => setExplicitImageModel(String(v))} placeholder="请输入模型标识（modelName）" />
              )}
            </div>
          </label>
          <label className="model-field">
            <span className="label">视频模型（可选）</span>
            <div className="model-row">
              <select value={videoModelInputMode} onChange={(e) => setVideoModelInputMode(e.target.value as 'preset' | 'custom')}>
                <option value="preset">从已配置模型选择</option>
                <option value="custom">手动输入</option>
              </select>
              {videoModelInputMode === 'preset' ? (
                <select value={explicitVideoModel} disabled={modelLoading} onChange={(e) => setExplicitVideoModel(e.target.value)}>
                  <option value="">未填则走系统路由</option>
                  {videoModelOptions.map((item) => (
                    <option key={item} value={item}>
                      {item}
                    </option>
                  ))}
                </select>
              ) : (
                <AppInput value={explicitVideoModel} onChange={(v) => setExplicitVideoModel(String(v))} placeholder="请输入模型标识（modelName）" />
              )}
            </div>
          </label>
        </div>

        {mode === 'text' ? (
          <AppInput value={sourceText} onChange={(v) => setSourceText(String(v))} label="剧本文本" as="textarea" rows={14} placeholder="请直接粘贴剧本文本，支持分段、对白和场景说明。" />
        ) : (
          <label className="upload-box">
            <span className="label">上传剧本文件</span>
            <input
              accept=".txt,.md,.docx"
              type="file"
              onChange={(ev) => setUploadFile(ev.target.files?.[0] || null)}
            />
            <span className="muted">{uploadFile?.name || '支持 .txt / .md / .docx'}</span>
          </label>
        )}

        {error ? <p className="error">{error}</p> : null}
        {warnings.map((item) => (
          <p key={item} className="warning">
            {item}
          </p>
        ))}

        <div className="actions">
          <AppButton type="submit" variant="primary" loading={createLoading}>
            创建并进入剧本预览
          </AppButton>
          <Link className="back-link muted" to="/script-projects">
            返回列表
          </Link>
        </div>
      </form>
    </section>
  )
}
