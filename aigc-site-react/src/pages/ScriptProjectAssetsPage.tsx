import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { KeyframeCard } from '@/components/script/KeyframeCard'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import type { AssetType, ExtractedAsset, KeyframeRecord } from '@/types'

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

function AssetBlock({
  asset: initial,
  keyframes,
  assetLoading,
  keyframeLoading,
  onSave,
  onGenerate,
  onConfirm,
  onRegenerate,
}: {
  asset: ExtractedAsset
  keyframes: KeyframeRecord[]
  assetLoading: boolean
  keyframeLoading: boolean
  onSave: (a: ExtractedAsset) => void
  onGenerate: (assetId: string) => void
  onConfirm: (keyframeId: string) => void
  onRegenerate: (keyframeId: string) => void
}) {
  const [draft, setDraft] = useState(initial)
  useEffect(() => {
    setDraft(initial)
  }, [initial.assetId, initial.updatedAt])

  const list = keyframes.filter((k) => k.assetId === draft.assetId)

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

      {list.length > 0 ? (
        <div className="keyframe-grid">
          {list.map((item) => (
            <KeyframeCard
              key={item.keyframeId}
              item={item}
              busy={keyframeLoading}
              onConfirm={onConfirm}
              onRegenerate={onRegenerate}
            />
          ))}
        </div>
      ) : (
        <p className="muted">还没有关键帧，保存后可直接生成。</p>
      )}
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
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadAssets = useScriptProjectStore((s) => s.loadAssets)
  const loadKeyframes = useScriptProjectStore((s) => s.loadKeyframes)
  const extractAssets = useScriptProjectStore((s) => s.extractAssets)
  const saveAsset = useScriptProjectStore((s) => s.saveAsset)
  const generateKeyframes = useScriptProjectStore((s) => s.generateKeyframes)
  const confirmKeyframe = useScriptProjectStore((s) => s.confirmKeyframe)
  const regenerateKeyframe = useScriptProjectStore((s) => s.regenerateKeyframe)

  const [activeTab, setActiveTab] = useState<AssetType>('CHARACTER')

  const currentAssets = useMemo(() => {
    return assets.filter((item) => item.assetType === activeTab)
  }, [assets, activeTab])

  useEffect(() => {
    if (!projectId) return
    void Promise.all([loadProject(projectId), loadAssets(projectId), loadKeyframes(projectId)])
  }, [projectId, loadProject, loadAssets, loadKeyframes])

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
      })
      showToast('资产已保存', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '保存失败', 'error')
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

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return <EmptyState title="项目不存在" description="请返回列表重新选择项目。" />
  }

  return (
    <section className="script-assets-page">
      <div className="toolbar panel glass">
        <div>
          <h2>资产与关键帧</h2>
          <p className="muted">分别抽取角色、背景、道具，并为每项生成可确认的关键帧。</p>
        </div>
        <div className="actions">
          <AppButton variant="primary" loading={assetLoading} onClick={() => void extractCurrent()}>
            抽取当前分类
          </AppButton>
          <Link className="nav-btn" to={`/script-projects/${projectId}/video`}>
            进入视频页
          </Link>
        </div>
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

      {assetLoading && !assets.length ? (
        <LoadingSpinner />
      ) : currentAssets.length ? (
        <div className="asset-list">
          {currentAssets.map((asset) => (
            <AssetBlock
              key={asset.assetId}
              asset={asset}
              keyframes={keyframes}
              assetLoading={assetLoading}
              keyframeLoading={keyframeLoading}
              onSave={(a) => void handleSave(a)}
              onGenerate={(id) => void handleGenerate(id)}
              onConfirm={(id) => void handleConfirm(id)}
              onRegenerate={(id) => void handleRegenerate(id)}
            />
          ))}
        </div>
      ) : (
        <EmptyState title="当前分类还没有资产" description="点击上方按钮抽取当前分类的视觉资产，然后再生成关键帧。" />
      )}
    </section>
  )
}
