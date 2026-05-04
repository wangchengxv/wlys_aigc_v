/**
 * WorkflowModelPanel — 可折叠的工作流级模型切换面板。
 *
 * 放置在剧本预览页、资产页、视频页右上角，允许为每个功能节点单独覆盖模型。
 * 层级：函数覆盖 → 项目默认 → 路由器默认
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import { useToast } from '@/context/ToastContext'
import { getEnabledModelNames, isEnabledModelName, type ModelCapability } from '@/lib/scriptProject/modelSelection'
import type { ModelConfig } from '@/types'
import { WorkflowModelKey } from '@/types'

// ─── Label maps ──────────────────────────────────────────────────────────────

type PageScope = 'preview' | 'assets' | 'video' | 'dubbing'

const SCOPE_KEYS: Record<PageScope, { key: string; label: string; capability: 'text' | 'image' | 'video' | 'tts' }[]> = {
  preview: [
    { key: WorkflowModelKey.SCRIPT_REFINE,         label: '智能完善',   capability: 'text' },
    { key: WorkflowModelKey.SCRIPT_APPEND,         label: 'AI 续写',    capability: 'text' },
    { key: WorkflowModelKey.SCRIPT_REWRITE,        label: 'AI 改写',    capability: 'text' },
    { key: WorkflowModelKey.OPTIMIZE_SCENES,       label: '优化场次',   capability: 'text' },
    { key: WorkflowModelKey.OPTIMIZE_CHARACTERS,   label: '优化角色',   capability: 'text' },
    { key: WorkflowModelKey.OPTIMIZE_PROPS,        label: '优化道具',   capability: 'text' },
  ],
  assets: [
    { key: WorkflowModelKey.ART_DIRECTION,         label: '美术指导',   capability: 'text' },
    { key: WorkflowModelKey.CHARACTER_VISUAL_PROMPT, label: '视觉提示词', capability: 'text' },
    { key: WorkflowModelKey.KEYFRAME_IMAGE,        label: '关键帧生成', capability: 'image' },
    { key: WorkflowModelKey.TURNAROUND_PLAN,       label: '九宫格规划', capability: 'text' },
    { key: WorkflowModelKey.TURNAROUND_IMAGE,      label: '九宫格出图', capability: 'image' },
    { key: WorkflowModelKey.STORYBOARD_PLAN,       label: '分镜规划',   capability: 'text' },
    { key: WorkflowModelKey.STORYBOARD_IMAGE,      label: '分镜出图',   capability: 'image' },
    { key: WorkflowModelKey.THREE_VIEW_IMAGE,      label: '三视图',     capability: 'image' },
    { key: WorkflowModelKey.GROUP_SCENE_IMAGE,     label: '群像概念图', capability: 'image' },
  ],
  video: [
    { key: WorkflowModelKey.SHOT_VISUAL_PROMPT,    label: '分镜提示词', capability: 'text' },
    { key: WorkflowModelKey.VIDEO_GENERATION,      label: '视频生成',   capability: 'video' },
  ],
  dubbing: [
    { key: WorkflowModelKey.TTS_DUBBING,           label: '配音生成',   capability: 'tts' },
  ],
}

const CAP_LABEL: Record<string, string> = { text: '文本', image: '图像', video: '视频', tts: '配音' }
const ALL_SCOPE_ENTRIES = Object.values(SCOPE_KEYS).flat()
const CAPABILITY_BY_KEY = new Map(ALL_SCOPE_ENTRIES.map((item) => [item.key, item.capability]))
const LABEL_BY_KEY = new Map(ALL_SCOPE_ENTRIES.map((item) => [item.key, item.label]))

// ─── Props ────────────────────────────────────────────────────────────────────

interface Props {
  projectId: string
  scope: PageScope
  allModels: ModelConfig[]
}

const FLOAT_PANEL_WIDTH = 360
const FLOAT_PANEL_MARGIN = 12

// ─── Component ────────────────────────────────────────────────────────────────

export function WorkflowModelPanel({ projectId, scope, allModels }: Props) {
  const { showToast } = useToast()
  const settings        = useScriptProjectStore((s) => s.workflowModelSettings)
  const settingsLoading = useScriptProjectStore((s) => s.workflowModelSettingsLoading)
  const settingsSaving  = useScriptProjectStore((s) => s.workflowModelSettingsSaving)
  const loadSettings    = useScriptProjectStore((s) => s.loadWorkflowModelSettings)
  const saveSettings    = useScriptProjectStore((s) => s.saveWorkflowModelSettings)

  const [open, setOpen] = useState(false)
  // local draft: key → modelName ('' = 清除覆盖)
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [defaultText,  setDefaultText]  = useState('')
  const [defaultImage, setDefaultImage] = useState('')
  const [defaultVideo, setDefaultVideo] = useState('')
  const [defaultTts, setDefaultTts] = useState('')
  const [dubbingVoice, setDubbingVoice] = useState('')
  const [dubbingLanguage, setDubbingLanguage] = useState('')
  const [dubbingSpeed, setDubbingSpeed] = useState('1')
  const [panelPos, setPanelPos] = useState<{ x: number; y: number } | null>(null)
  const [dragging, setDragging] = useState(false)
  const loaded = useRef(false)
  const rootRef = useRef<HTMLDivElement | null>(null)
  const panelRef = useRef<HTMLDivElement | null>(null)
  const dragStateRef = useRef({ active: false, offsetX: 0, offsetY: 0 })

  function clampPanelPosition(nextX: number, nextY: number, width: number, height: number) {
    const maxX = Math.max(FLOAT_PANEL_MARGIN, window.innerWidth - width - FLOAT_PANEL_MARGIN)
    const maxY = Math.max(FLOAT_PANEL_MARGIN, window.innerHeight - height - FLOAT_PANEL_MARGIN)
    return {
      x: Math.min(Math.max(FLOAT_PANEL_MARGIN, nextX), maxX),
      y: Math.min(Math.max(FLOAT_PANEL_MARGIN, nextY), maxY),
    }
  }

  function getInitPanelPosition() {
    const toggleEl = rootRef.current?.querySelector<HTMLButtonElement>('.wf-model-panel__toggle')
    const rect = toggleEl?.getBoundingClientRect()
    if (!rect) return { x: FLOAT_PANEL_MARGIN, y: FLOAT_PANEL_MARGIN }
    const nextX = rect.right - FLOAT_PANEL_WIDTH
    const nextY = rect.bottom + 8
    return clampPanelPosition(nextX, nextY, FLOAT_PANEL_WIDTH, Math.max(420, window.innerHeight * 0.7))
  }

  // 首次展开时加载
  useEffect(() => {
    if (!open || loaded.current) return
    loaded.current = true
    void loadSettings(projectId).catch((e) =>
      showToast(e instanceof Error ? e.message : '加载模型设置失败', 'error')
    )
  }, [open, projectId, loadSettings, showToast])

  // settings 更新时同步到本地草稿
  useEffect(() => {
    if (!settings) return
    setDefaultText(settings.defaultTextModel ?? '')
    setDefaultImage(settings.defaultImageModel ?? '')
    setDefaultVideo(settings.defaultVideoModel ?? '')
    setDefaultTts(settings.defaultTtsModel ?? '')
    setDubbingVoice(settings.dubbingVoice ?? '')
    setDubbingLanguage(settings.dubbingLanguage ?? '')
    setDubbingSpeed(settings.dubbingSpeed != null ? String(settings.dubbingSpeed) : '1')
    setDraft(settings.overrides ?? {})
  }, [settings])

  useEffect(() => {
    if (!open || !panelPos) return
    function handleResize() {
      const width = panelRef.current?.offsetWidth ?? FLOAT_PANEL_WIDTH
      const height = panelRef.current?.offsetHeight ?? Math.max(420, window.innerHeight * 0.7)
      setPanelPos((pos) => {
        if (!pos) return pos
        return clampPanelPosition(pos.x, pos.y, width, height)
      })
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [open, panelPos])

  const keys = SCOPE_KEYS[scope]
  const enabledModelNames = useMemo(
    () => ({
      text: new Set(getEnabledModelNames(allModels, 'text')),
      image: new Set(getEnabledModelNames(allModels, 'image')),
      video: new Set(getEnabledModelNames(allModels, 'video')),
      tts: new Set(getEnabledModelNames(allModels, 'tts')),
    }),
    [allModels],
  )
  const invalidSelections = useMemo(() => {
    const items: string[] = []
    const check = (label: string, capability: ModelCapability, value: string) => {
      const normalized = value.trim()
      if (normalized && !enabledModelNames[capability].has(normalized)) {
        items.push(`${label}：${normalized}`)
      }
    }

    check('默认文本模型', 'text', defaultText)
    check('默认图像模型', 'image', defaultImage)
    check('默认视频模型', 'video', defaultVideo)
    check('默认配音模型', 'tts', defaultTts)

    Object.entries(draft).forEach(([key, value]) => {
      const capability = CAPABILITY_BY_KEY.get(key)
      if (!capability) return
      check(LABEL_BY_KEY.get(key) || key, capability, value)
    })

    return items
  }, [defaultImage, defaultText, defaultTts, defaultVideo, draft, enabledModelNames])

  // 按 capability 过滤可选模型
  function modelsFor(cap: string): ModelConfig[] {
    return allModels.filter(
      (m) =>
        m.enabled &&
        Array.isArray(m.metadata?.capabilities) &&
        (m.metadata.capabilities as string[]).includes(cap),
    )
  }

  function parseDubbingSpeed(value: string) {
    const parsed = Number(value)
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return 1
    }
    return parsed
  }

  async function handleSave() {
    const skipped: string[] = []
    const sanitizeDefaultModel = (label: string, capability: ModelCapability, value: string) => {
      const normalized = value.trim()
      if (!normalized) return null
      if (isEnabledModelName(allModels, capability, normalized)) {
        return normalized
      }
      skipped.push(`${label}：${normalized}`)
      return null
    }
    // 覆盖项保留空字符串，用来显式清除后端上一次保存的无效覆盖。
    const nextOverrides: Record<string, string> = {}
    for (const [k, v] of Object.entries(draft)) {
      const normalized = v.trim()
      if (!normalized) {
        nextOverrides[k] = ''
        continue
      }
      const capability = CAPABILITY_BY_KEY.get(k)
      if (!capability || isEnabledModelName(allModels, capability, normalized)) {
        nextOverrides[k] = normalized
        continue
      }
      skipped.push(`${LABEL_BY_KEY.get(k) || k}：${normalized}`)
      nextOverrides[k] = ''
    }
    try {
      await saveSettings(projectId, {
        defaultTextModel: sanitizeDefaultModel('默认文本模型', 'text', defaultText),
        defaultImageModel: sanitizeDefaultModel('默认图像模型', 'image', defaultImage),
        defaultVideoModel: sanitizeDefaultModel('默认视频模型', 'video', defaultVideo),
        defaultTtsModel: sanitizeDefaultModel('默认配音模型', 'tts', defaultTts),
        dubbingVoice: dubbingVoice.trim() || null,
        dubbingLanguage: dubbingLanguage.trim() || null,
        dubbingSpeed: parseDubbingSpeed(dubbingSpeed),
        overrides: nextOverrides,
      })
      showToast(
        skipped.length > 0 ? `模型设置已保存，已自动清空 ${skipped.length} 个失效模型名` : '模型设置已保存',
        skipped.length > 0 ? 'info' : 'success',
      )
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  function handleTogglePanel() {
    setOpen((prev) => {
      const next = !prev
      if (next && !panelPos) {
        setPanelPos(getInitPanelPosition())
      }
      return next
    })
  }

  function handleDragStart(event: React.PointerEvent<HTMLDivElement>) {
    if (event.button !== 0) return
    const rect = panelRef.current?.getBoundingClientRect()
    if (!rect) return
    dragStateRef.current = {
      active: true,
      offsetX: event.clientX - rect.left,
      offsetY: event.clientY - rect.top,
    }
    setDragging(true)
    event.currentTarget.setPointerCapture(event.pointerId)
    event.preventDefault()
  }

  function handleDragMove(event: React.PointerEvent<HTMLDivElement>) {
    if (!dragStateRef.current.active) return
    const width = panelRef.current?.offsetWidth ?? FLOAT_PANEL_WIDTH
    const height = panelRef.current?.offsetHeight ?? Math.max(420, window.innerHeight * 0.7)
    const next = clampPanelPosition(
      event.clientX - dragStateRef.current.offsetX,
      event.clientY - dragStateRef.current.offsetY,
      width,
      height,
    )
    setPanelPos(next)
  }

  function handleDragEnd(event: React.PointerEvent<HTMLDivElement>) {
    if (!dragStateRef.current.active) return
    dragStateRef.current.active = false
    setDragging(false)
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
  }

  // ── 渲染 ──────────────────────────────────────────────────────────────────
  return (
    <div ref={rootRef} className="wf-model-panel">
      <AppButton
        variant="ghost"
        size="sm"
        onClick={handleTogglePanel}
        className="wf-model-panel__toggle"
        title="切换功能级模型"
      >
        <span className="wf-model-panel__toggle-icon">{open ? '▾' : '▸'}</span>
        模型设置
        {settings && Object.keys(settings.overrides ?? {}).length > 0 && (
          <span className="wf-model-panel__badge">
            {Object.keys(settings.overrides).length}
          </span>
        )}
      </AppButton>

      {open && (
        <div
          ref={panelRef}
          className="wf-model-panel__body"
          style={panelPos ? { left: `${panelPos.x}px`, top: `${panelPos.y}px` } : undefined}
        >
          <div
            className={`wf-model-panel__drag-handle${dragging ? ' is-dragging' : ''}`}
            onPointerDown={handleDragStart}
            onPointerMove={handleDragMove}
            onPointerUp={handleDragEnd}
            onPointerCancel={handleDragEnd}
          >
            <span>模型配置面板</span>
            <span className="wf-model-panel__drag-tip">按住拖动</span>
          </div>
          {settingsLoading ? (
            <p className="muted" style={{ padding: '8px 0' }}>加载中…</p>
          ) : (
            <>
              {/* 项目默认模型区域 */}
              <div className="wf-model-panel__section">
                <div className="wf-model-panel__section-title">项目默认模型</div>
                {(['text', 'image', 'video', 'tts'] as const).map((cap) => {
                  const list = modelsFor(cap)
                  const val = cap === 'text'
                    ? defaultText
                    : cap === 'image'
                      ? defaultImage
                      : cap === 'video'
                        ? defaultVideo
                        : defaultTts
                  const setter = cap === 'text'
                    ? setDefaultText
                    : cap === 'image'
                      ? setDefaultImage
                      : cap === 'video'
                        ? setDefaultVideo
                        : setDefaultTts
                  return (
                    <div key={cap} className="wf-model-panel__row">
                      <span className="wf-model-panel__row-label">{CAP_LABEL[cap]}模型</span>
                      <select
                        className="wf-model-panel__select"
                        value={val}
                        onChange={(e) => setter(e.target.value)}
                      >
                        <option value="">— 路由器默认 —</option>
                        {list.map((m) => (
                          <option key={m.id} value={m.modelName}>
                            {m.name || m.modelName}
                          </option>
                        ))}
                        {/* 允许手动输入非列表模型 */}
                        {val && !list.find((m) => m.modelName === val) && (
                          <option value={val}>{val}（已失效，保存时清空）</option>
                        )}
                      </select>
                    </div>
                  )
                })}
                {scope === 'dubbing' ? (
                  <>
                    <div className="wf-model-panel__row">
                      <span className="wf-model-panel__row-label">默认音色</span>
                      <input
                        className="wf-model-panel__select"
                        value={dubbingVoice}
                        onChange={(e) => setDubbingVoice(e.target.value)}
                        placeholder="例如：通用女声"
                      />
                    </div>
                    <div className="wf-model-panel__row">
                      <span className="wf-model-panel__row-label">配音语言</span>
                      <input
                        className="wf-model-panel__select"
                        value={dubbingLanguage}
                        onChange={(e) => setDubbingLanguage(e.target.value)}
                        placeholder="例如：中文"
                      />
                    </div>
                    <div className="wf-model-panel__row">
                      <span className="wf-model-panel__row-label">语速</span>
                      <input
                        className="wf-model-panel__select"
                        type="number"
                        min="0.5"
                        max="2"
                        step="0.1"
                        value={dubbingSpeed}
                        onChange={(e) => setDubbingSpeed(e.target.value)}
                        placeholder="1.0"
                      />
                    </div>
                  </>
                ) : null}
              </div>

              {/* 本页功能覆盖区域 */}
              <div className="wf-model-panel__section">
                <div className="wf-model-panel__section-title">本页功能覆盖</div>
                {keys.map(({ key, label, capability }) => {
                  const list = modelsFor(capability)
                  const val = draft[key] ?? ''
                  return (
                    <div key={key} className="wf-model-panel__row">
                      <span className="wf-model-panel__row-label">
                        {label}
                        <span className="wf-model-panel__cap-tag">{CAP_LABEL[capability]}</span>
                      </span>
                      <select
                        className="wf-model-panel__select"
                        value={val}
                        onChange={(e) =>
                          setDraft((d) => ({ ...d, [key]: e.target.value }))
                        }
                      >
                        <option value="">— 跟随项目默认 —</option>
                        {list.map((m) => (
                          <option key={m.id} value={m.modelName}>
                            {m.name || m.modelName}
                          </option>
                        ))}
                        {val && !list.find((m) => m.modelName === val) && (
                          <option value={val}>{val}（已失效，保存时清空）</option>
                        )}
                      </select>
                    </div>
                  )
                })}
              </div>
              {invalidSelections.length > 0 ? (
                <p className="muted" style={{ margin: '8px 0 0' }}>
                  检测到失效模型名，保存时会自动清空，不再传给后端。
                </p>
              ) : null}

              <div className="wf-model-panel__actions">
                <AppButton
                  variant="primary"
                  size="sm"
                  loading={settingsSaving}
                  onClick={handleSave}
                >
                  保存设置
                </AppButton>
                <AppButton
                  variant="ghost"
                  size="sm"
                  onClick={() => setOpen(false)}
                >
                  收起
                </AppButton>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  )
}
