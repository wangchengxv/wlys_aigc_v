import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { getModels } from '@/api'
import { ActionDrawer } from '@/components/common/ActionDrawer'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { HelpHint } from '@/components/common/HelpHint'
import { useToast } from '@/context/ToastContext'
import { getPresetById, groupPresetsByCategory, presetDescriptor, resolveVisualStyleForProject, styleSummaryShort } from '@/data/videoStylePresets'
import { sanitizeEnabledModelName } from '@/lib/scriptProject/modelSelection'
import { useGlobalSettingsStore } from '@/stores/globalSettingsStore'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import { useStyleTemplateStore } from '@/stores/styleTemplateStore'
import { ScriptAppendPreviewDialog } from '@/components/script/ScriptAppendPreviewDialog'
import { ScriptRewriteDialog } from '@/components/script/ScriptRewriteDialog'
import type { GlobalAspectRatio, ModelConfig } from '@/types'
import type { RewriteDiffMode } from '@/components/script/ScriptRewriteDiffPanel'

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
  const [searchParams] = useSearchParams()
  const { showToast } = useToast()
  const createFromText = useScriptProjectStore((s) => s.createFromText)
  const createFromUpload = useScriptProjectStore((s) => s.createFromUpload)
  const createLoading = useScriptProjectStore((s) => s.createLoading)
  const refineLoading = useScriptProjectStore((s) => s.refineLoading)
  const appendLoading = useScriptProjectStore((s) => s.appendLoading)
  const rewriteLoading = useScriptProjectStore((s) => s.rewriteLoading)
  const refine = useScriptProjectStore((s) => s.refine)
  const appendPreview = useScriptProjectStore((s) => s.appendPreview)
  const rewritePreview = useScriptProjectStore((s) => s.rewritePreview)
  const applyRewrite = useScriptProjectStore((s) => s.applyRewrite)
  const importScript = useScriptProjectStore((s) => s.importScript)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadScript = useScriptProjectStore((s) => s.loadScript)
  const saveScript = useScriptProjectStore((s) => s.saveScript)
  const scriptPayload = useScriptProjectStore((s) => s.scriptPayload)
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const loadTemplates = useStyleTemplateStore((s) => s.loadTemplates)
  const queryCourseId = searchParams.get('courseId')?.trim() || ''
  const queryStyleTemplateId = searchParams.get('styleTemplateId')?.trim() || ''
  const queryAssignmentTitle = searchParams.get('assignmentTitle')?.trim() || ''

  const [mode, setMode] = useState<'text' | 'upload'>('text')
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [name, setName] = useState(() => queryAssignmentTitle || '')
  const [sourceText, setSourceText] = useState('')
  const globalVisualStyleMode = useGlobalSettingsStore((s) => s.visualStyleMode)
  const globalVisualStylePresetId = useGlobalSettingsStore((s) => s.visualStylePresetId)
  const globalCustomVisualStyle = useGlobalSettingsStore((s) => s.customVisualStyle)
  const globalVisualStyleLongTextMode = useGlobalSettingsStore((s) => s.visualStyleLongTextMode)
  const templates = useStyleTemplateStore((s) => s.templates)
  const [styleInputMode, setStyleInputMode] = useState<'preset' | 'custom'>(() =>
    queryStyleTemplateId || (globalVisualStyleMode === 'preset' && !!globalVisualStylePresetId) ? 'preset' : 'custom',
  )
  const [selectedPresetId, setSelectedPresetId] = useState(
    () => queryStyleTemplateId || globalVisualStylePresetId || 'film-cinematic',
  )
  const [customVisualStyle, setCustomVisualStyle] = useState(() =>
    globalVisualStyleMode === 'custom'
      ? globalCustomVisualStyle
      : resolveVisualStyleForProject(useGlobalSettingsStore.getState(), templates),
  )
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
    }, templates),
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
  const [createDrawerOpen, setCreateDrawerOpen] = useState(true)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [showAppendDialog, setShowAppendDialog] = useState(false)
  const [showRewriteDialog, setShowRewriteDialog] = useState(false)
  const [appendPreviewText, setAppendPreviewText] = useState('')
  const [appendPreviewMeta, setAppendPreviewMeta] = useState<{ existingLength: number; maxAppendChars: number; baseUsed: string } | null>(null)
  const [rewriteInstruction, setRewriteInstruction] = useState('')
  const [rewriteTargetStyle, setRewriteTargetStyle] = useState('')
  const [rewriteMaxOutputChars, setRewriteMaxOutputChars] = useState('')
  const [rewritePreviewText, setRewritePreviewText] = useState('')
  const [rewriteDiffMode, setRewriteDiffMode] = useState<RewriteDiffMode>('split')
  const [rewritePreviewMeta, setRewritePreviewMeta] = useState<{ baseUsed: string; sourceLength: number; maxOutputChars?: number | null } | null>(null)
  const [refinedPreviewText, setRefinedPreviewText] = useState('')
  const [createdProjectId, setCreatedProjectId] = useState<string | null>(null)
  const presetGroups = useMemo(() => groupPresetsByCategory(templates), [templates])
  const selectedPreset = useMemo(() => getPresetById(selectedPresetId, templates), [selectedPresetId, templates])
  const courseLinked = !!queryCourseId

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
    void loadTemplates(queryCourseId || undefined)
  }, [loadTemplates, queryCourseId])

  useEffect(() => {
    if (!queryStyleTemplateId) return
    setStyleInputMode('preset')
    setSelectedPresetId(queryStyleTemplateId)
  }, [queryStyleTemplateId])

  useEffect(() => {
    if (!queryAssignmentTitle || name.trim()) return
    setName(queryAssignmentTitle)
  }, [queryAssignmentTitle, name])

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
        setExplicitTextModel((prev) => (prev && te.includes(prev) ? prev : te[0] || ''))
        setExplicitImageModel((prev) => (prev && im.includes(prev) ? prev : im[0] || ''))
        setExplicitVideoModel((prev) => (prev && vm.includes(prev) ? prev : vm[0] || ''))
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
    const safeTextModel = sanitizeEnabledModelName(modelOptions, 'text', textM)
    const safeImageModel = sanitizeEnabledModelName(modelOptions, 'image', imageM)
    const safeVideoModel = sanitizeEnabledModelName(modelOptions, 'video', videoM)
    const nextWarnings: string[] = []
    if (textM && !safeTextModel) {
      nextWarnings.push('文本模型无效或已失效，已改为不传后端。')
    }
    if (imageM && !safeImageModel) {
      nextWarnings.push('图片模型无效或已失效，已改为不传后端。')
    }
    if (videoM && !safeVideoModel) {
      nextWarnings.push('视频模型无效或已失效，已改为不传后端。')
    }
    setWarnings(nextWarnings)

    try {
      let result
      const visualStyle =
        styleInputMode === 'preset'
          ? selectedPreset
            ? globalVisualStyleLongTextMode
              ? selectedPreset.fullPrompt.trim() || presetDescriptor(selectedPreset)
              : presetDescriptor(selectedPreset)
            : resolveVisualStyleForProject(useGlobalSettingsStore.getState(), templates)
          : customVisualStyle.trim() || resolveVisualStyleForProject(useGlobalSettingsStore.getState(), templates)
      const base = {
        name,
        visualStyle,
        styleTemplateId: styleInputMode === 'preset' ? selectedPresetId : undefined,
        aspectRatio,
        targetDuration,
        language,
        courseId: queryCourseId || undefined,
        explicitTextModel: safeTextModel,
        explicitImageModel: safeImageModel,
        explicitVideoModel: safeVideoModel,
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
      setCreatedProjectId(result.project.projectId)
      navigate(`/script-projects/${result.project.projectId}/global-settings`)
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建项目失败')
    }
  }

  async function handleCreateAndGoToGlobalSettings() {
    if (!createdProjectId) {
      showToast('请先创建项目', 'error')
      return
    }
    navigate(`/script-projects/${createdProjectId}/global-settings`)
  }

  function onPickImport() {
    fileInputRef.current?.click()
  }

  async function onImportFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (!createdProjectId) {
      showToast('请先创建项目', 'error')
      return
    }
    try {
      await importScript(createdProjectId, file, { autoRefine: false })
      await loadProject(createdProjectId)
      await loadScript(createdProjectId)
      if (scriptPayload?.originalText) {
        setSourceText(scriptPayload.originalText)
      }
      showToast('剧本已导入', 'success')
    } catch (err) {
      showToast(err instanceof Error ? err.message : '导入失败', 'error')
    }
  }

  async function handleRefine() {
    if (!createdProjectId) {
      showToast('请先创建项目', 'error')
      return
    }
    try {
      const doc = await refine(createdProjectId)
      setRefinedPreviewText(doc.refinedMarkdown || doc.originalText || '')
      if (doc.refinedMarkdown) {
        setSourceText(doc.refinedMarkdown)
      }
      showToast('剧本完善完成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
    }
  }

  async function handleAppendPreview() {
    if (!createdProjectId) {
      showToast('请先创建项目', 'error')
      return
    }
    try {
      const res = await appendPreview(createdProjectId, {})
      setAppendPreviewText(res.appendText || '')
      setAppendPreviewMeta({ existingLength: res.existingLength, maxAppendChars: res.maxAppendChars, baseUsed: res.baseUsed })
      setShowAppendDialog(true)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '续写预览失败', 'error')
    }
  }

  async function handleAppendConfirm() {
    if (!createdProjectId) return
    try {
      const base = refinedPreviewText || sourceText
      const combined = `${base}${base.endsWith('\n') ? '' : '\n'}${appendPreviewText}`.trim()
      await saveScript(createdProjectId, {
        refinedMarkdown: combined,
        structuredScript: scriptPayload?.structuredScript || {},
      })
      setSourceText(combined)
      setRefinedPreviewText(combined)
      showToast('续写已追加并保存', 'success')
      setShowAppendDialog(false)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleRewritePreview() {
    if (!createdProjectId) {
      showToast('请先创建项目', 'error')
      return
    }
    const instruction = rewriteInstruction.trim()
    if (!instruction) {
      showToast('请先输入改写要求', 'error')
      return
    }
    const maxOutputChars = rewriteMaxOutputChars.trim() ? Number(rewriteMaxOutputChars.trim()) : undefined
    if (maxOutputChars != null && (!Number.isFinite(maxOutputChars) || maxOutputChars <= 0)) {
      showToast('字数上限必须为正整数', 'error')
      return
    }
    try {
      const result = await rewritePreview(createdProjectId, {
        rewriteInstruction: instruction,
        targetStyle: rewriteTargetStyle.trim() || undefined,
        maxOutputChars,
        language: currentProject?.project.language || undefined,
      })
      setRewritePreviewText(result.rewrittenText || '')
      setRewritePreviewMeta({
        baseUsed: result.baseUsed,
        sourceLength: result.sourceLength,
        maxOutputChars: result.maxOutputChars,
      })
      showToast('改写预览生成成功', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '改写预览失败', 'error')
    }
  }

  async function handleApplyRewrite() {
    if (!createdProjectId) return
    if (!rewritePreviewText.trim()) {
      showToast('请先生成改写预览', 'error')
      return
    }
    try {
      await applyRewrite(createdProjectId, { rewrittenText: rewritePreviewText })
      setRefinedPreviewText(rewritePreviewText)
      setSourceText(rewritePreviewText)
      setShowRewriteDialog(false)
      setRewriteDiffMode('split')
      showToast('改写结果已应用并保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '应用改写失败', 'error')
    }
  }

  return (
    <section className="script-create-page">
      <div className="script-create-page__intro panel glass">
        <div>
          <p className="eyebrow">Project Setup</p>
          <h2>新建剧本项目</h2>
          <p className="muted">创建入口改为抽屉承载，首屏保留流程说明与关键操作，减少大表单占位。</p>
        </div>
        <div className="script-create-page__intro-actions">
          <AppButton variant="primary" onClick={() => setCreateDrawerOpen(true)}>
            打开创建表单
          </AppButton>
          <Link className="back-link muted" to="/script-projects">
            返回列表
          </Link>
        </div>
      </div>

      <HelpHint title="查看创建说明" className="script-create-page__help">
        <p>创建/编辑表单统一使用抽屉或弹窗承载，不再占据首屏主体。</p>
        <p>进入页面后可直接在抽屉中填写项目信息；若暂不创建，可先关闭抽屉查看当前全局偏好。</p>
      </HelpHint>

      <div className="mode-switch panel glass">
        <button type="button" className={mode === 'text' ? 'active' : ''} onClick={() => setMode('text')}>
          粘贴文本
        </button>
        <button type="button" className={mode === 'upload' ? 'active' : ''} onClick={() => setMode('upload')}>
          上传文件
        </button>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".txt,.md,.docx"
        style={{ display: 'none' }}
        aria-hidden
        onChange={(e) => void onImportFile(e)}
      />

      <ActionDrawer
        open={createDrawerOpen}
        title="新建剧本项目"
        description="创建动作统一为抽屉承载，可按文本粘贴或文件上传方式发起项目。"
        onClose={() => setCreateDrawerOpen(false)}
      >
        <form className="form script-create-page__form" onSubmit={(e) => void submit(e)}>
          <div className="global-prefs-hint panel glass">
            <p className="muted">
              当前全局偏好：剧本「{scriptType}」· 风格「{styleSummary}」· 策略「{modelStrategy}」· 创作「{creationMode}」· 分镜「{storyboardLayout}」。
              <Link to="/global-settings">去全局设定</Link>
            </p>
            {courseLinked ? (
              <p className="muted">
                当前项目将绑定课程 `{queryCourseId}`。
                {queryStyleTemplateId ? ' 这个入口来自课程作业，已预选作业要求模板；如需提交该作业，建议保持模板绑定不变。' : ''}
              </p>
            ) : null}
          </div>
          <div className="grid">
            <AppInput value={name} onChange={(v) => setName(String(v))} label="项目名称" placeholder="例如：都市悬疑短片" />
            <label className="model-field">
              <span className="label">视觉风格</span>
              <div className="model-row">
                <select value={styleInputMode} onChange={(e) => setStyleInputMode(e.target.value as 'preset' | 'custom')}>
                  <option value="preset">从全局风格库选择</option>
                  <option value="custom">手动输入</option>
                </select>
                {styleInputMode === 'preset' ? (
                  <select value={selectedPresetId} onChange={(e) => setSelectedPresetId(e.target.value)}>
                    {presetGroups.map((group) => (
                      <optgroup key={group.category} label={group.category}>
                        {group.presets.map((preset) => (
                          <option key={preset.templateId} value={preset.templateId}>
                            {preset.name}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                ) : (
                  <AppInput
                    value={customVisualStyle}
                    onChange={(v) => setCustomVisualStyle(String(v))}
                    placeholder="例如：电影感写实 / 国风 / 赛博朋克"
                  />
                )}
              </div>
              <span className="hint muted">
                {styleInputMode === 'preset'
                  ? `将提交模板解析后的风格文本，并同时绑定模板ID；当前：${selectedPreset ? presetDescriptor(selectedPreset) : '未选择'}`
                  : '可填写自由中文/英文描述；留空则沿用全局设定中的风格（含长文本模式整段提示词）。'}
              </span>
            </label>
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

          {createdProjectId && (
            <div className="script-create-page__ai-actions panel glass">
              <div className="script-create-page__ai-actions-row">
                <AppButton variant="ghost" onClick={() => onPickImport()}>
                  导入剧本
                </AppButton>
                <AppButton variant="primary" loading={refineLoading} onClick={() => void handleRefine()}>
                  完善剧本
                </AppButton>
                <AppButton variant="primary" loading={appendLoading} onClick={() => void handleAppendPreview()}>
                  AI 续写剧本
                </AppButton>
                <AppButton variant="primary" loading={rewriteLoading} onClick={() => setShowRewriteDialog(true)}>
                  AI 剧本改写
                </AppButton>
              </div>
            </div>
          )}

          {error ? <p className="error">{error}</p> : null}
          {warnings.map((item) => (
            <p key={item} className="warning">
              {item}
            </p>
          ))}

          <div className="actions">
            {!createdProjectId ? (
              <AppButton type="submit" variant="primary" loading={createLoading}>
                创建项目
              </AppButton>
            ) : (
              <>
                <AppButton variant="primary" onClick={() => void handleRefine()}>
                  完善剧本
                </AppButton>
                <AppButton onClick={() => void handleCreateAndGoToGlobalSettings()}>
                  进入全局设定
                </AppButton>
              </>
            )}
            <AppButton onClick={() => setCreateDrawerOpen(false)}>关闭抽屉</AppButton>
          </div>
        </form>
      </ActionDrawer>

      <ScriptAppendPreviewDialog
        visible={showAppendDialog}
        appendText={appendPreviewText}
        loading={appendLoading}
        subtitle={
          appendPreviewMeta
            ? `基于：${appendPreviewMeta.baseUsed}｜已有 ${appendPreviewMeta.existingLength} 字符｜本次上限 ${appendPreviewMeta.maxAppendChars} 字符`
            : undefined
        }
        onCancel={() => setShowAppendDialog(false)}
        onConfirmAppend={() => void handleAppendConfirm()}
      />

      <ScriptRewriteDialog
        visible={showRewriteDialog}
        instruction={rewriteInstruction}
        targetStyle={rewriteTargetStyle}
        maxOutputChars={rewriteMaxOutputChars}
        originalText={sourceText}
        previewText={rewritePreviewText}
        diffMode={rewriteDiffMode}
        loading={rewriteLoading}
        applying={rewriteLoading}
        previewSubtitle={
          rewritePreviewMeta
            ? `基于：${rewritePreviewMeta.baseUsed}｜原文长度 ${rewritePreviewMeta.sourceLength} 字符${
                rewritePreviewMeta.maxOutputChars ? `｜本次上限 ${rewritePreviewMeta.maxOutputChars} 字符` : ''
              }`
            : undefined
        }
        onChangeInstruction={setRewriteInstruction}
        onChangeTargetStyle={setRewriteTargetStyle}
        onChangeMaxOutputChars={setRewriteMaxOutputChars}
        onChangeDiffMode={setRewriteDiffMode}
        onCancel={() => setShowRewriteDialog(false)}
        onPreview={() => void handleRewritePreview()}
        onApply={() => void handleApplyRewrite()}
      />
    </section>
  )
}
