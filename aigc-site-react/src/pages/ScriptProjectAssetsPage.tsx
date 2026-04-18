import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  getModels,
  resolveScriptFileUrl,
  rollbackAssetVisualPrompt,
  rollbackKeyframePrompt,
  updateKeyframePromptText,
} from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { AssetHistoryPanel } from '@/components/script/AssetHistoryPanel'
import { KeyframeCard } from '@/components/script/KeyframeCard'
import { PromptVersionsEditor } from '@/components/script/PromptVersionsEditor'
import { ArtDirectionPreview } from '@/components/script/ArtDirectionPreview'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { WorkflowModelPanel } from '@/components/script/WorkflowModelPanel'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { AssetHistoryType, AssetType, ExtractedAsset, KeyframeRecord, ModelConfig, StoredFileRecord, StoryboardShot } from '@/types'

const tabToEndpoint: Record<AssetType, 'characters' | 'backgrounds' | 'props'> = {
  CHARACTER: 'characters',
  BACKGROUND: 'backgrounds',
  PROP: 'props',
}

function updateTagsFromString(asset: ExtractedAsset, value: string): ExtractedAsset {
  const tags = value
    .split(/[，,、]/)
    .map((item) => item.trim())
    .filter(Boolean)
  return { ...asset, tags }
}

type StoryboardPanel = {
  index: number
  shotSize: string
  cameraAngle: string
  description: string
  descriptionZh?: string
}

function parseStoryboardPanels(asset: ExtractedAsset): StoryboardPanel[] {
  if (!asset.storyboardPlanJson) return []
  try {
    const root = JSON.parse(asset.storyboardPlanJson) as { panels?: Array<Record<string, unknown>> }
    const map = new Map<number, StoryboardPanel>()
    for (const item of root.panels || []) {
      const index = Number(item.index)
      if (!Number.isInteger(index)) continue
      map.set(index, {
        index,
        shotSize: String(item.shotSize || ''),
        cameraAngle: String(item.cameraAngle || ''),
        description: String(item.description || ''),
      })
    }
    if (asset.storyboardTranslationsJson) {
      const transRoot = JSON.parse(asset.storyboardTranslationsJson) as { translations?: Array<Record<string, unknown>> }
      for (const item of transRoot.translations || []) {
        const index = Number(item.index)
        const panel = map.get(index)
        if (panel) panel.descriptionZh = String(item.descriptionZh || '')
      }
    }
    return Array.from(map.values()).sort((a, b) => a.index - b.index)
  } catch {
    return []
  }
}

function AssetBlock({
  projectId,
  asset: initial,
  keyframes,
  assetLoading,
  keyframeLoading,
  visualPromptLoading,
  onSave,
  onSaveVisualPrompt,
  onRollbackVisualPrompt,
  onGenerate,
  onConfirm,
  onRegenerate,
  onKeyframeSavePrompt,
  onKeyframeRollbackPrompt,
  onVisualPrompt,
  onTurnaroundPlan,
  onTurnaroundImage,
  onThreeView,
  shots,
  onStoryboardPlan,
  onStoryboardTranslate,
  onStoryboardRewrite,
  onStoryboardImage,
  onApplyStoryboardFirstFrame,
  onReloadAfterHistory,
}: {
  projectId: string
  asset: ExtractedAsset
  keyframes: KeyframeRecord[]
  assetLoading: boolean
  keyframeLoading: boolean
  visualPromptLoading: boolean
  onSave: (a: ExtractedAsset) => void
  onSaveVisualPrompt: (assetId: string, text: string) => void | Promise<void>
  onRollbackVisualPrompt: (assetId: string, versionId: string) => void | Promise<void>
  onGenerate: (assetId: string) => void
  onConfirm: (keyframeId: string) => void
  onRegenerate: (keyframeId: string) => void
  onKeyframeSavePrompt: (keyframeId: string, text: string) => void | Promise<void>
  onKeyframeRollbackPrompt: (keyframeId: string, versionId: string) => void | Promise<void>
  onVisualPrompt: (assetId: string) => void
  onTurnaroundPlan: (assetId: string) => void
  onTurnaroundImage: (assetId: string) => void
  onThreeView: (assetId: string) => void
  shots: StoryboardShot[]
  onStoryboardPlan: (assetId: string) => void
  onStoryboardTranslate: (assetId: string) => void
  onStoryboardRewrite: (assetId: string, instruction: string) => void
  onStoryboardImage: (assetId: string) => void
  onApplyStoryboardFirstFrame: (assetId: string, shotId: string, mode: 'FULL_GRID' | 'CROPPED_PANEL', panelIndex?: number) => void
  onReloadAfterHistory?: () => void | Promise<void>
}) {
  const [draft, setDraft] = useState(initial)
  const [selectedPanelIndex, setSelectedPanelIndex] = useState(0)
  const [selectedShotId, setSelectedShotId] = useState('')
  const [rewriteInstruction, setRewriteInstruction] = useState('')
  const [histOpen, setHistOpen] = useState<{ type: AssetHistoryType; referenceId: string } | null>(null)
  useEffect(() => {
    setDraft(initial)
  }, [initial])

  const list = keyframes.filter((k) => k.assetId === draft.assetId)
  const isCharacter = draft.assetType === 'CHARACTER'
  const storyboardPanels = useMemo(() => parseStoryboardPanels(draft), [draft])

  useEffect(() => {
    if (shots.length > 0 && !selectedShotId) {
      setSelectedShotId(shots[0].shotId)
    }
  }, [shots, selectedShotId])

  async function copyText(text: string) {
    try {
      await navigator.clipboard.writeText(text)
    } catch {
      /* ignore */
    }
  }

  return (
    <article className="asset-card panel glass">
      <div className="asset-head">
        <div>
          <h3>{draft.name}</h3>
          <p className="muted">状态：{draft.status}</p>
        </div>
        <div className="actions">
          <AppButton size="sm" loading={assetLoading} onClick={() => onSave(draft)}>
            保存
          </AppButton>
          <AppButton
            size="sm"
            variant="ghost"
            loading={visualPromptLoading}
            onClick={() => onVisualPrompt(draft.assetId)}
          >
            生成视觉提示词
          </AppButton>
          <AppButton size="sm" loading={visualPromptLoading} onClick={() => onThreeView(draft.assetId)}>
            生成三视图
          </AppButton>
          {isCharacter ? (
            <>
              <AppButton size="sm" loading={visualPromptLoading} onClick={() => onTurnaroundPlan(draft.assetId)}>
                九宫格规划 B-6
              </AppButton>
              <AppButton size="sm" variant="primary" loading={visualPromptLoading} onClick={() => onTurnaroundImage(draft.assetId)}>
                九宫格造型图 B-7
              </AppButton>
              <AppButton size="sm" loading={visualPromptLoading} onClick={() => onStoryboardPlan(draft.assetId)}>
                分镜九宫格规划
              </AppButton>
              <AppButton size="sm" loading={visualPromptLoading} onClick={() => onStoryboardImage(draft.assetId)}>
                分镜九宫格出图
              </AppButton>
            </>
          ) : null}
          <AppButton size="sm" variant="primary" loading={keyframeLoading} onClick={() => onGenerate(draft.assetId)}>
            生成关键帧
          </AppButton>
        </div>
      </div>

      <div className="form-grid">
        <AppInput value={draft.name} onChange={(v) => setDraft((d) => ({ ...d, name: String(v) }))} label="名称" />
        <AppInput
          value={draft.tags.join(', ')}
          onChange={(v) => setDraft((d) => updateTagsFromString(d, String(v)))}
          label="标签"
          placeholder="例如：主角, 正面像, 暖光"
        />
      </div>
      <AppInput value={draft.description} onChange={(v) => setDraft((d) => ({ ...d, description: String(v) }))} label="描述" as="textarea" rows={4} />
      <AppInput value={draft.promptDraft} onChange={(v) => setDraft((d) => ({ ...d, promptDraft: String(v) }))} label="关键帧提示词草稿" as="textarea" rows={4} />

      <div className="visual-prompt-block">
        <div className="visual-prompt-head">
          <span className="eyebrow">视觉提示词（B-3/4/5）</span>
          {draft.visualPrompt ? (
            <button type="button" className="link-btn" onClick={() => void copyText(draft.visualPrompt || '')}>
              复制当前
            </button>
          ) : null}
        </div>
        <PromptVersionsEditor
          label="编辑视觉提示词"
          value={draft.visualPrompt ?? ''}
          onChange={(v) => setDraft((d) => ({ ...d, visualPrompt: v }))}
          versions={draft.promptVersions ?? undefined}
          busy={assetLoading}
          onSave={async () => {
            await onSaveVisualPrompt(draft.assetId, (draft.visualPrompt ?? '').trim())
          }}
          onRollback={async (versionId) => {
            await onRollbackVisualPrompt(draft.assetId, versionId)
          }}
        />
      </div>

      {isCharacter && draft.turnaroundImageFileId ? (
        <div className="turnaround-preview">
          <div className="asset-preview-head">
            <p className="eyebrow">九宫格造型 B-7</p>
            <AppButton size="sm" variant="ghost" onClick={() => setHistOpen({ type: 'TURNAROUND', referenceId: draft.assetId })}>
              历史版本
            </AppButton>
          </div>
          <img
            className="turnaround-img"
            src={resolveScriptFileUrl(draft.turnaroundImageFileId)}
            alt={`${draft.name} 九宫格`}
          />
        </div>
      ) : null}

      {draft.storyboardImageFileId ? (
        <div className="turnaround-preview">
          <div className="asset-preview-head">
            <p className="eyebrow">分镜九宫格</p>
            <AppButton size="sm" variant="ghost" onClick={() => setHistOpen({ type: 'STORYBOARD', referenceId: draft.assetId })}>
              历史版本
            </AppButton>
          </div>
          <img
            className="turnaround-img"
            src={resolveScriptFileUrl(draft.storyboardImageFileId)}
            alt={`${draft.name} 分镜九宫格`}
          />
        </div>
      ) : null}

      {storyboardPanels.length ? (
        <div className="storyboard-panel-wrap">
          <div className="storyboard-panel-head">
            <span className="eyebrow">九宫格分镜结构</span>
            <div className="actions">
              <AppButton size="sm" loading={visualPromptLoading} onClick={() => onStoryboardTranslate(draft.assetId)}>
                重译中文
              </AppButton>
            </div>
          </div>
          <div className="storyboard-grid">
            {storyboardPanels.map((panel) => (
              <button
                type="button"
                key={panel.index}
                className={`storyboard-cell ${selectedPanelIndex === panel.index ? 'active' : ''}`}
                onClick={() => setSelectedPanelIndex(panel.index)}
              >
                <span className="storyboard-cell-title">
                  #{panel.index + 1} · {panel.shotSize} / {panel.cameraAngle}
                </span>
                <span className="storyboard-cell-desc">{panel.descriptionZh || panel.description}</span>
              </button>
            ))}
          </div>
          <div className="storyboard-rewrite-row">
            <AppInput
              value={rewriteInstruction}
              onChange={(v) => setRewriteInstruction(String(v))}
              label="分镜改写指令"
              placeholder="例如：改成雨夜压迫感、减少动作幅度"
            />
            <AppButton
              size="sm"
              loading={visualPromptLoading}
              onClick={() => onStoryboardRewrite(draft.assetId, rewriteInstruction)}
            >
              按指令改写分镜
            </AppButton>
          </div>
          <div className="storyboard-apply-row">
            <label className="input-wrap">
              <span className="label">应用到镜头首帧</span>
              <select className="ctrl" value={selectedShotId} onChange={(e) => setSelectedShotId(e.target.value)}>
                <option value="">请选择镜头</option>
                {shots.map((shot) => (
                  <option key={shot.shotId} value={shot.shotId}>
                    Shot {shot.sequenceNo} · {shot.title}
                  </option>
                ))}
              </select>
            </label>
            <div className="actions">
              <AppButton
                size="sm"
                loading={visualPromptLoading}
                onClick={() => selectedShotId && onApplyStoryboardFirstFrame(draft.assetId, selectedShotId, 'FULL_GRID')}
              >
                整图作为首帧
              </AppButton>
              <AppButton
                size="sm"
                variant="primary"
                loading={visualPromptLoading}
                onClick={() =>
                  selectedShotId && onApplyStoryboardFirstFrame(draft.assetId, selectedShotId, 'CROPPED_PANEL', selectedPanelIndex)
                }
              >
                使用已选格子裁剪首帧
              </AppButton>
            </div>
          </div>
        </div>
      ) : null}

      {draft.threeViewImageFileId ? (
        <div className="turnaround-preview">
          <div className="asset-preview-head">
            <p className="eyebrow">三视图（正/侧/背）</p>
            <AppButton size="sm" variant="ghost" onClick={() => setHistOpen({ type: 'THREE_VIEW', referenceId: draft.assetId })}>
              历史版本
            </AppButton>
          </div>
          <img
            className="turnaround-img"
            src={resolveScriptFileUrl(draft.threeViewImageFileId)}
            alt={`${draft.name} 三视图`}
          />
        </div>
      ) : null}

      {list.length > 0 ? (
        <div className="keyframe-grid">
          {list.map((item) => (
            <KeyframeCard
              key={item.keyframeId}
              projectId={projectId}
              item={item}
              busy={keyframeLoading}
              onConfirm={onConfirm}
              onRegenerate={onRegenerate}
              onSavePrompt={onKeyframeSavePrompt}
              onRollbackPrompt={onKeyframeRollbackPrompt}
              onHistoryRestored={onReloadAfterHistory}
            />
          ))}
        </div>
      ) : (
        <p className="muted">还没有关键帧，保存后可直接生成。</p>
      )}

      {histOpen ? (
        <AssetHistoryPanel
          projectId={projectId}
          assetType={histOpen.type}
          referenceId={histOpen.referenceId}
          open={!!histOpen}
          onClose={() => setHistOpen(null)}
          onRestored={onReloadAfterHistory}
        />
      ) : null}
    </article>
  )
}

export function ScriptProjectAssetsPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const assets = useScriptProjectStore((s) => s.assets)
  const keyframes = useScriptProjectStore((s) => s.keyframes)
  const assetLoading = useScriptProjectStore((s) => s.assetLoading)
  const keyframeLoading = useScriptProjectStore((s) => s.keyframeLoading)
  const artDirectionLoading = useScriptProjectStore((s) => s.artDirectionLoading)
  const visualPromptLoading = useScriptProjectStore((s) => s.visualPromptLoading)
  const groupSceneLoading = useScriptProjectStore((s) => s.groupSceneLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadAssets = useScriptProjectStore((s) => s.loadAssets)
  const loadKeyframes = useScriptProjectStore((s) => s.loadKeyframes)
  const extractAssets = useScriptProjectStore((s) => s.extractAssets)
  const saveAsset = useScriptProjectStore((s) => s.saveAsset)
  const generateKeyframes = useScriptProjectStore((s) => s.generateKeyframes)
  const confirmKeyframe = useScriptProjectStore((s) => s.confirmKeyframe)
  const regenerateKeyframe = useScriptProjectStore((s) => s.regenerateKeyframe)
  const generateArtDirectionAction = useScriptProjectStore((s) => s.generateArtDirectionAction)
  const batchCharacterVisualPrompts = useScriptProjectStore((s) => s.batchCharacterVisualPrompts)
  const generateVisualPromptForAsset = useScriptProjectStore((s) => s.generateVisualPromptForAsset)
  const generateTurnaroundPlanForAsset = useScriptProjectStore((s) => s.generateTurnaroundPlanForAsset)
  const generateTurnaroundImageForAsset = useScriptProjectStore((s) => s.generateTurnaroundImageForAsset)
  const generateStoryboardPlanForAsset = useScriptProjectStore((s) => s.generateStoryboardPlanForAsset)
  const translateStoryboardPlanForAsset = useScriptProjectStore((s) => s.translateStoryboardPlanForAsset)
  const rewriteStoryboardPlanForAsset = useScriptProjectStore((s) => s.rewriteStoryboardPlanForAsset)
  const generateStoryboardImageForAsset = useScriptProjectStore((s) => s.generateStoryboardImageForAsset)
  const applyStoryboardFirstFrameForShot = useScriptProjectStore((s) => s.applyStoryboardFirstFrameForShot)
  const generateThreeViewForAsset = useScriptProjectStore((s) => s.generateThreeViewForAsset)
  const generateGroupScenePrompt = useScriptProjectStore((s) => s.generateGroupScenePrompt)
  const shots = useScriptProjectStore((s) => s.shots)
  const loadShots = useScriptProjectStore((s) => s.loadShots)

  const [activeTab, setActiveTab] = useState<AssetType>('CHARACTER')
  const [artExpanded, setArtExpanded] = useState(true)
  const [allModels, setAllModels] = useState<ModelConfig[]>([])
  const [groupSelected, setGroupSelected] = useState<Record<string, boolean>>({})
  const [groupLocation, setGroupLocation] = useState('')
  const [groupTime, setGroupTime] = useState('')
  const [groupAtmosphere, setGroupAtmosphere] = useState('')
  const [groupGenerateImage, setGroupGenerateImage] = useState(false)
  const [lastGroupPrompt, setLastGroupPrompt] = useState('')

  const currentAssets = useMemo(() => {
    return assets.filter((item) => item.assetType === activeTab)
  }, [assets, activeTab])

  const characterAssets = useMemo(() => assets.filter((a) => a.assetType === 'CHARACTER'), [assets])

  const groupSceneHistory = useMemo(() => {
    const files = currentProject?.files ?? []
    return files
      .filter((f: StoredFileRecord) => (f.relativePath || '').startsWith('group-scene/') && (f.fileName || '').toLowerCase().endsWith('.txt'))
      .slice()
      .sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  }, [currentProject?.files])

  useEffect(() => {
    void getModels().then(setAllModels).catch(() => {})
  }, [])

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      try {
        await Promise.all([loadProject(projectId), loadAssets(projectId), loadKeyframes(projectId), loadShots(projectId)])
      } catch (e) {
        showToast(e instanceof Error ? e.message : '页面初始化失败，请重试', 'error')
      }
    })()
  }, [projectId, loadProject, loadAssets, loadKeyframes, loadShots, showToast])

  async function extractCurrent() {
    try {
      await extractAssets(projectId, tabToEndpoint[activeTab])
      showToast('资产抽取完成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '抽取失败', 'error')
    }
  }

  async function handleSave(asset: ExtractedAsset) {
    try {
      await saveAsset(projectId, asset.assetId, {
        name: asset.name,
        description: asset.description,
        promptDraft: asset.promptDraft,
        tags: asset.tags,
        metadata: asset.metadata,
        visualPrompt: asset.visualPrompt ?? undefined,
      })
      showToast('资产已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleSaveVisualPrompt(assetId: string, text: string) {
    try {
      await saveAsset(projectId, assetId, { visualPrompt: text })
      showToast('视觉提示词已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleRollbackVisualPrompt(assetId: string, versionId: string) {
    try {
      await rollbackAssetVisualPrompt(projectId, assetId, { versionId })
      await loadAssets(projectId)
      showToast('已回滚到所选版本', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '回滚失败', 'error')
    }
  }

  async function handleKeyframeSavePrompt(keyframeId: string, text: string) {
    try {
      await updateKeyframePromptText(projectId, keyframeId, { promptText: text })
      await loadKeyframes(projectId)
      showToast('关键帧提示词已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
    }
  }

  async function handleKeyframeRollbackPrompt(keyframeId: string, versionId: string) {
    try {
      await rollbackKeyframePrompt(projectId, keyframeId, { versionId })
      await loadKeyframes(projectId)
      showToast('已回滚关键帧提示词', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '回滚失败', 'error')
    }
  }

  async function handleGenerate(assetId: string) {
    try {
      await generateKeyframes(projectId, assetId)
      showToast('关键帧生成完成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '关键帧生成失败', 'error')
    }
  }

  async function handleConfirm(keyframeId: string) {
    try {
      await confirmKeyframe(projectId, keyframeId)
      showToast('已确认关键帧', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '确认失败', 'error')
    }
  }

  async function handleRegenerate(keyframeId: string) {
    try {
      await regenerateKeyframe(projectId, keyframeId)
      showToast('关键帧已重生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '重生成失败', 'error')
    }
  }

  async function handleArtDirection() {
    try {
      await generateArtDirectionAction(projectId)
      showToast('美术指导已生成（B-1）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '生成失败', 'error')
    }
  }

  async function handleBatchCharacters() {
    try {
      await batchCharacterVisualPrompts(projectId)
      showToast('批量角色视觉提示词已生成（B-2）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '批量生成失败', 'error')
    }
  }

  async function handleVisualPrompt(assetId: string) {
    try {
      await generateVisualPromptForAsset(projectId, assetId)
      showToast('视觉提示词已更新', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '生成失败', 'error')
    }
  }

  async function handleTurnaroundPlan(assetId: string) {
    try {
      await generateTurnaroundPlanForAsset(projectId, assetId)
      showToast('九宫格视角规划已生成（B-6）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '规划失败', 'error')
    }
  }

  async function handleTurnaroundImage(assetId: string) {
    try {
      await generateTurnaroundImageForAsset(projectId, assetId)
      showToast('九宫格造型图已生成（B-7）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '生成失败', 'error')
    }
  }

  async function handleThreeView(assetId: string) {
    try {
      await generateThreeViewForAsset(projectId, assetId)
      showToast('三视图已生成（正/侧/背）', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '三视图生成失败', 'error')
    }
  }

  async function handleStoryboardPlan(assetId: string) {
    try {
      await generateStoryboardPlanForAsset(projectId, assetId)
      showToast('九宫格分镜规划已生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '生成九宫格分镜规划失败', 'error')
    }
  }

  async function handleStoryboardTranslate(assetId: string) {
    try {
      await translateStoryboardPlanForAsset(projectId, assetId)
      showToast('九宫格分镜翻译已更新', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '翻译失败', 'error')
    }
  }

  async function handleStoryboardRewrite(assetId: string, instruction: string) {
    if (!instruction.trim()) {
      showToast('请先填写改写指令', 'error')
      return
    }
    try {
      await rewriteStoryboardPlanForAsset(projectId, assetId, { instruction: instruction.trim() })
      showToast('九宫格分镜已改写', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '改写失败', 'error')
    }
  }

  async function handleStoryboardImage(assetId: string) {
    try {
      await generateStoryboardImageForAsset(projectId, assetId)
      showToast('九宫格分镜图已生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '出图失败', 'error')
    }
  }

  async function handleApplyStoryboardFirstFrame(
    assetId: string,
    shotId: string,
    mode: 'FULL_GRID' | 'CROPPED_PANEL',
    panelIndex?: number,
  ) {
    try {
      await applyStoryboardFirstFrameForShot(projectId, shotId, {
        assetId,
        mode,
        panelIndex,
      })
      showToast(mode === 'FULL_GRID' ? '已应用整张九宫格为首帧' : '已应用格子裁剪图为首帧', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '应用首帧失败', 'error')
    }
  }

  async function handleGroupScene() {
    const ids = characterAssets.filter((a) => groupSelected[a.assetId]).map((a) => a.assetId)
    if (ids.length < 1) {
      showToast('请至少勾选一个角色', 'error')
      return
    }
    try {
      const res = await generateGroupScenePrompt(projectId, {
        characterAssetIds: ids,
        location: groupLocation.trim() || undefined,
        time: groupTime.trim() || undefined,
        atmosphere: groupAtmosphere.trim() || undefined,
        generateImage: groupGenerateImage,
      })
      setLastGroupPrompt(res.promptText)
      showToast(groupGenerateImage && res.imageFileId ? '群像提示词与概念图已生成（B-8）' : '群像提示词已生成（B-8）', 'success')
      await loadProject(projectId)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '群像生成失败', 'error')
    }
  }

  function toggleGroup(id: string) {
    setGroupSelected((s) => ({ ...s, [id]: !s[id] }))
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  const project = currentProject.project

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="资产与关键帧"
      description="把资产抽取、视觉提示词、关键帧、九宫格和群像都收进同一工作区，首屏只保留当前分类、模型入口和关键动作。"
      meta={
        <>
          <span className="soft-badge">{project.name}</span>
          <span className="soft-badge">{activeTab === 'CHARACTER' ? '人物形象' : activeTab === 'BACKGROUND' ? '视频背景' : '视频道具'}</span>
        </>
      }
      stats={[
        { key: 'assets', label: '当前分类资产', value: currentAssets.length },
        { key: 'keyframes', label: '关键帧总数', value: keyframes.length },
        { key: 'characters', label: '角色资产', value: characterAssets.length },
        { key: 'group-history', label: '群像历史', value: groupSceneHistory.length },
      ]}
      helpTitle="查看资产页说明"
      help={
        <>
          <p>资产页已经整合抽取资产、美术指导、提示词、关键帧、三视图、九宫格和群像，不再分散到多个次级入口。</p>
          <p>帮助说明默认收起，批量生成和历史回看留在就近区域处理。</p>
        </>
      }
    >
    <section className="script-assets-page">
      <div className="toolbar panel glass">
        <div>
          <h2>资产与关键帧</h2>
          <p className="muted">抽取资产 → 生成美术指导（B-1）→ 视觉提示词（B-2～B-5）→ 关键帧 / 九宫格（B-6/B-7）→ 群像（B-8）。</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <WorkflowModelPanel projectId={projectId} scope="assets" allModels={allModels} />
        </div>
        <div className="actions">
          <AppButton variant="primary" loading={assetLoading} onClick={() => void extractCurrent()}>
            抽取当前分类
          </AppButton>
          <AppButton loading={artDirectionLoading} onClick={() => void handleArtDirection()}>
            生成美术指导 B-1
          </AppButton>
          <Link className="nav-btn" to={`/script-projects/${projectId}/video`}>
            进入视频页
          </Link>
          <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
            成片与导出
          </Link>
        </div>
      </div>

      <div className="panel glass art-direction-panel">
        <button type="button" className="art-toggle" onClick={() => setArtExpanded((v) => !v)}>
          {artExpanded ? '▼' : '▶'} 全局美术指导（B-1）
        </button>
        {artExpanded ? (
          <div className="art-direction-body">
            <ArtDirectionPreview
              artDirectionJson={currentProject?.project.artDirectionJson ?? null}
              onCopy={(label) => showToast(`${label} 已复制`, 'success')}
            />
          </div>
        ) : null}
      </div>

      <div className="panel glass group-scene-panel">
        <h3>多角色群像 B-8</h3>
        <p className="muted">勾选多个已生成视觉提示词的角色，填写场景信息后生成群像画面提示词（可选同时出概念图）。</p>
        <div className="group-character-picks">
          {characterAssets.map((a) => (
            <label key={a.assetId} className="group-check">
              <input type="checkbox" checked={!!groupSelected[a.assetId]} onChange={() => toggleGroup(a.assetId)} />
              {a.name}
            </label>
          ))}
          {characterAssets.length === 0 ? <span className="muted">暂无角色资产</span> : null}
        </div>
        <div className="form-grid">
          <AppInput value={groupLocation} onChange={(v) => setGroupLocation(String(v))} label="地点" placeholder="例如：废弃仓库二楼" />
          <AppInput value={groupTime} onChange={(v) => setGroupTime(String(v))} label="时间" placeholder="例如：深夜" />
          <AppInput value={groupAtmosphere} onChange={(v) => setGroupAtmosphere(String(v))} label="氛围" placeholder="例如：紧张、雨夜" />
        </div>
        <label className="group-check">
          <input type="checkbox" checked={groupGenerateImage} onChange={(e) => setGroupGenerateImage(e.target.checked)} />
          同时调用图像模型生成概念图（消耗额度）
        </label>
        <AppButton variant="primary" loading={groupSceneLoading} onClick={() => void handleGroupScene()}>
          生成群像提示词
        </AppButton>
        {lastGroupPrompt ? (
          <div className="visual-prompt-block">
            <p className="eyebrow">上次群像提示词</p>
            <p className="visual-prompt-text">{lastGroupPrompt}</p>
          </div>
        ) : null}

        {groupSceneHistory.length ? (
          <div className="group-scene-history">
            <p className="eyebrow">历史群像结果（已写入项目文件）</p>
            <div className="group-scene-history-list">
              {groupSceneHistory.slice(0, 10).map((f) => {
                const url = resolveScriptFileUrl(f.fileId)
                return (
                  <div key={f.fileId} className="group-scene-history-item">
                    <div className="group-scene-history-main">
                      <strong className="group-scene-history-name">{f.fileName}</strong>
                      <span className="muted group-scene-history-path">{f.relativePath}</span>
                    </div>
                    <div className="group-scene-history-actions">
                      <a className="nav-btn" href={url} target="_blank" rel="noreferrer">
                        打开
                      </a>
                      <button
                        type="button"
                        className="pill small"
                        onClick={() => {
                          void (async () => {
                            try {
                              const res = await fetch(url)
                              const text = await res.text()
                              await navigator.clipboard.writeText(text)
                              showToast('群像提示词已复制', 'success')
                            } catch {
                              showToast('复制失败', 'error')
                            }
                          })()
                        }}
                      >
                        复制
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
            {groupSceneHistory.length > 10 ? <p className="muted">仅显示最新 10 条</p> : null}
          </div>
        ) : null}
      </div>

      <div className="tabs panel glass">
        <button type="button" className={activeTab === 'CHARACTER' ? 'active' : ''} onClick={() => setActiveTab('CHARACTER')}>
          人物形象
        </button>
        <button type="button" className={activeTab === 'BACKGROUND' ? 'active' : ''} onClick={() => setActiveTab('BACKGROUND')}>
          视频背景
        </button>
        <button type="button" className={activeTab === 'PROP' ? 'active' : ''} onClick={() => setActiveTab('PROP')}>
          视频道具
        </button>
      </div>

      {activeTab === 'CHARACTER' && characterAssets.length > 0 ? (
        <div className="panel glass batch-bar">
          <AppButton loading={visualPromptLoading} onClick={() => void handleBatchCharacters()}>
            批量生成角色视觉提示词 B-2
          </AppButton>
          <span className="muted">需先完成 B-1 美术指导</span>
        </div>
      ) : null}

      {assetLoading && !assets.length ? (
        <LoadingSpinner />
      ) : currentAssets.length ? (
        <div className="asset-list">
          {currentAssets.map((asset) => (
            <AssetBlock
              key={asset.assetId}
              projectId={projectId}
              asset={asset}
              keyframes={keyframes}
              assetLoading={assetLoading}
              keyframeLoading={keyframeLoading}
              visualPromptLoading={visualPromptLoading}
              onSave={(a) => void handleSave(a)}
              onSaveVisualPrompt={(assetId, text) => void handleSaveVisualPrompt(assetId, text)}
              onRollbackVisualPrompt={(assetId, vid) => void handleRollbackVisualPrompt(assetId, vid)}
              onGenerate={(id) => void handleGenerate(id)}
              onConfirm={(id) => void handleConfirm(id)}
              onRegenerate={(id) => void handleRegenerate(id)}
              onKeyframeSavePrompt={(kf, text) => void handleKeyframeSavePrompt(kf, text)}
              onKeyframeRollbackPrompt={(kf, vid) => void handleKeyframeRollbackPrompt(kf, vid)}
              onVisualPrompt={(id) => void handleVisualPrompt(id)}
              onTurnaroundPlan={(id) => void handleTurnaroundPlan(id)}
              onTurnaroundImage={(id) => void handleTurnaroundImage(id)}
              onThreeView={(id) => void handleThreeView(id)}
              shots={shots}
              onStoryboardPlan={(id) => void handleStoryboardPlan(id)}
              onStoryboardTranslate={(id) => void handleStoryboardTranslate(id)}
              onStoryboardRewrite={(id, instruction) => void handleStoryboardRewrite(id, instruction)}
              onStoryboardImage={(id) => void handleStoryboardImage(id)}
              onApplyStoryboardFirstFrame={(assetId, shotId, mode, panelIndex) =>
                void handleApplyStoryboardFirstFrame(assetId, shotId, mode, panelIndex)
              }
              onReloadAfterHistory={async () => {
                await loadAssets(projectId)
                await loadKeyframes(projectId)
              }}
            />
          ))}
        </div>
      ) : (
        <EmptyState title="当前分类还没有资产" description="点击上方按钮抽取当前分类的视觉资产，然后再生成关键帧。" />
      )}
    </section>
    </ProjectSubpageShell>
  )
}
