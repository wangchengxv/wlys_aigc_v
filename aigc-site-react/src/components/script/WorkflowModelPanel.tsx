/**
 * WorkflowModelPanel — 可折叠的工作流级模型切换面板。
 *
 * 放置在剧本预览页、资产页、视频页右上角，允许为每个功能节点单独覆盖模型。
 * 层级：函数覆盖 → 项目默认 → 路由器默认
 */
import { useEffect, useRef, useState } from 'react'
import { AppButton } from '@/components/common/AppButton'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import { useToast } from '@/context/ToastContext'
import type { ModelConfig } from '@/types'
import { WorkflowModelKey } from '@/types'

// ─── Label maps ──────────────────────────────────────────────────────────────

type PageScope = 'preview' | 'assets' | 'video'

const SCOPE_KEYS: Record<PageScope, { key: string; label: string; capability: 'text' | 'image' | 'video' }[]> = {
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
}

const CAP_LABEL: Record<string, string> = { text: '文本', image: '图像', video: '视频' }

// ─── Props ────────────────────────────────────────────────────────────────────

interface Props {
  projectId: string
  scope: PageScope
  allModels: ModelConfig[]
}

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
  const loaded = useRef(false)

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
    setDraft(settings.overrides ?? {})
  }, [settings])

  const keys = SCOPE_KEYS[scope]

  // 按 capability 过滤可选模型
  function modelsFor(cap: string): ModelConfig[] {
    return allModels.filter(
      (m) =>
        m.enabled &&
        Array.isArray(m.metadata?.capabilities) &&
        (m.metadata.capabilities as string[]).includes(cap),
    )
  }

  async function handleSave() {
    // 去掉空字符串覆盖（等同清除）
    const cleanOverrides: Record<string, string> = {}
    for (const [k, v] of Object.entries(draft)) {
      if (v.trim()) cleanOverrides[k] = v.trim()
    }
    try {
      await saveSettings(projectId, {
        defaultTextModel:  defaultText.trim()  || null,
        defaultImageModel: defaultImage.trim() || null,
        defaultVideoModel: defaultVideo.trim() || null,
        overrides: cleanOverrides,
      })
      showToast('模型设置已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  // ── 渲染 ──────────────────────────────────────────────────────────────────
  return (
    <div className="wf-model-panel">
      <AppButton
        variant="ghost"
        size="sm"
        onClick={() => setOpen((v) => !v)}
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
        <div className="wf-model-panel__body">
          {settingsLoading ? (
            <p className="muted" style={{ padding: '8px 0' }}>加载中…</p>
          ) : (
            <>
              {/* 项目默认模型区域 */}
              <div className="wf-model-panel__section">
                <div className="wf-model-panel__section-title">项目默认模型</div>
                {(['text', 'image', 'video'] as const).map((cap) => {
                  const list = modelsFor(cap)
                  const val = cap === 'text' ? defaultText : cap === 'image' ? defaultImage : defaultVideo
                  const setter = cap === 'text' ? setDefaultText : cap === 'image' ? setDefaultImage : setDefaultVideo
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
                          <option value={val}>{val}（手动）</option>
                        )}
                      </select>
                    </div>
                  )
                })}
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
                          <option value={val}>{val}（手动）</option>
                        )}
                      </select>
                    </div>
                  )
                })}
              </div>

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
