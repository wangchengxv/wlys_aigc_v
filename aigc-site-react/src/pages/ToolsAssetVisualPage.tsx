import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  generateContent,
  generateThreeView,
  generateTurnaroundImage,
  generateTurnaroundPlan,
  getScriptAssets,
  listScriptProjects,
  resolveScriptFileUrl,
} from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { useToast } from '@/context/ToastContext'
import { useAuthStore } from '@/stores/authStore'
import type { ExtractedAsset, ScriptProjectSummary } from '@/types'

export function ToolsAssetVisualPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [projects, setProjects] = useState<ScriptProjectSummary[]>([])
  const [projectsLoading, setProjectsLoading] = useState(true)
  const [projectId, setProjectId] = useState('')
  const [assets, setAssets] = useState<ExtractedAsset[]>([])
  const [assetsLoading, setAssetsLoading] = useState(false)
  const [assetId, setAssetId] = useState('')
  const [busy, setBusy] = useState(false)
  const [quickAssetName, setQuickAssetName] = useState('')
  const [quickAssetDesc, setQuickAssetDesc] = useState('')
  const [quickAssetType, setQuickAssetType] = useState<'CHARACTER' | 'BACKGROUND' | 'PROP'>('CHARACTER')
  const [quickBusy, setQuickBusy] = useState(false)
  const [quickThreeViewUrl, setQuickThreeViewUrl] = useState('')
  const [quickTurnaroundUrl, setQuickTurnaroundUrl] = useState('')

  const loadProjects = useCallback(async () => {
    setProjectsLoading(true)
    try {
      const list = await listScriptProjects({ deleted: false })
      setProjects(list)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '加载工程列表失败', 'error')
    } finally {
      setProjectsLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    void loadProjects()
  }, [loadProjects])

  const refreshAssets = useCallback(
    async (pid: string) => {
      if (!pid) {
        setAssets([])
        return
      }
      setAssetsLoading(true)
      try {
        const list = await getScriptAssets(pid)
        setAssets(list)
      } catch (e) {
        showToast(e instanceof Error ? e.message : '加载资产失败', 'error')
        setAssets([])
      } finally {
        setAssetsLoading(false)
      }
    },
    [showToast],
  )

  useEffect(() => {
    if (!projectId) {
      setAssets([])
      setAssetId('')
      return
    }
    void refreshAssets(projectId)
  }, [projectId, refreshAssets])

  const selected = useMemo(() => assets.find((a) => a.assetId === assetId), [assets, assetId])
  const isCharacter = selected?.assetType === 'CHARACTER'

  async function run<T>(fn: () => Promise<T>, okMsg: string, errMsg: string) {
    if (!projectId || !assetId) {
      showToast('当前未绑定工程。你仍可使用下方免绑定快速模式；如需写入工程，请先选择工程与资产。', 'info')
      return
    }
    setBusy(true)
    try {
      await fn()
      showToast(okMsg, 'success')
      await refreshAssets(projectId)
    } catch (e) {
      showToast(e instanceof Error ? e.message : errMsg, 'error')
    } finally {
      setBusy(false)
    }
  }

  async function runQuick(kind: 'THREE_VIEW' | 'TURNAROUND') {
    const name = quickAssetName.trim()
    if (!name) {
      showToast('请先填写主体名称', 'error')
      return
    }

    if (kind === 'TURNAROUND' && quickAssetType !== 'CHARACTER') {
      showToast('九宫格快速模式仅支持角色类型，请先切换为“角色”', 'info')
      return
    }

    const desc = quickAssetDesc.trim()
    const base = [`主体：${name}`, desc ? `描述：${desc}` : '', `类型：${quickAssetType}`].filter(Boolean).join('；')
    const prompt =
      kind === 'THREE_VIEW'
        ? `请生成角色设定三视图（正面、侧面、背面）拼图，统一服装与体态，白底，细节清晰。${base}`
        : `请生成角色九宫格设定图，九个机位保持同一角色一致性，白底，细节清晰。${base}`

    setQuickBusy(true)
    try {
      const res = await generateContent({
        prompt,
        mode: 'image',
        style: '影视级真实',
        imageSize: '1024x1024',
        textLength: 'medium',
        count: 1,
      })
      const imageUrl = res.imageResults?.[0]
      if (!imageUrl) {
        throw new Error('未返回图片结果')
      }
      if (kind === 'THREE_VIEW') {
        setQuickThreeViewUrl(imageUrl)
      } else {
        setQuickTurnaroundUrl(imageUrl)
      }
      showToast(kind === 'THREE_VIEW' ? '免绑定三视图已生成' : '免绑定九宫格已生成', 'success')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '免绑定生成失败', 'error')
    } finally {
      setQuickBusy(false)
    }
  }

  if (!user) {
    return <EmptyState title="请先登录后访问" description="三视图工具面向已登录用户开放，请登录后继续使用。" />
  }

  return (
    <section className="tools-asset-visual-page">
      <div className="head panel glass">
        <div>
          <p className="muted">绑定工程为可选项。可直接使用免绑定快速模式生成示意图；如选择工程与资产，则结果会写回项目资产页。</p>
        </div>
        {projectId ? (
          <Link className="create-link" to={`/script-projects/${encodeURIComponent(projectId)}/assets`}>
            打开资产页
          </Link>
        ) : null}
      </div>

      <div className="panel glass tools-asset-visual-controls">
        {projectsLoading ? (
          <LoadingSpinner />
        ) : (
          <>
            <label className="input-wrap">
              <span className="label">剧本工程</span>
              <select
                className="ctrl"
                value={projectId}
                onChange={(e) => {
                  setProjectId(e.target.value)
                  setAssetId('')
                }}
              >
                <option value="">请选择</option>
                {projects.map((p) => (
                  <option key={p.projectId} value={p.projectId}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="input-wrap">
              <span className="label">资产</span>
              <select
                className="ctrl"
                value={assetId}
                onChange={(e) => setAssetId(e.target.value)}
                disabled={!projectId || assetsLoading}
              >
                <option value="">{assetsLoading ? '加载中…' : '请选择资产'}</option>
                {assets.map((a) => (
                  <option key={a.assetId} value={a.assetId}>
                    {a.name} · {a.assetType}
                  </option>
                ))}
              </select>
            </label>
          </>
        )}
      </div>

      {selected ? (
        <div className="panel glass tools-asset-visual-actions">
          <div className="tools-asset-visual-section">
            <p className="eyebrow">三视图</p>
            <div className="actions">
              <AppButton
                size="sm"
                loading={busy}
                onClick={() =>
                  run(
                    () => generateThreeView(projectId, assetId),
                    '三视图已生成（正/侧/背）',
                    '三视图生成失败',
                  )
                }
              >
                生成三视图
              </AppButton>
            </div>
          </div>

          <div className="tools-asset-visual-section">
            <p className="eyebrow">角色九宫格（B-6 / B-7）</p>
            {!isCharacter ? (
              <p className="muted">仅「角色」资产支持九宫格规划与造型图。</p>
            ) : (
              <div className="actions">
                <AppButton
                  size="sm"
                  loading={busy}
                  onClick={() =>
                    run(
                      () => generateTurnaroundPlan(projectId, assetId),
                      '九宫格视角规划已生成（B-6）',
                      '规划失败',
                    )
                  }
                >
                  九宫格规划 B-6
                </AppButton>
                <AppButton
                  size="sm"
                  variant="primary"
                  loading={busy}
                  onClick={() =>
                    run(
                      () => generateTurnaroundImage(projectId, assetId),
                      '九宫格造型图已生成（B-7）',
                      '生成失败',
                    )
                  }
                >
                  九宫格造型图 B-7
                </AppButton>
              </div>
            )}
          </div>
        </div>
      ) : null}

      {!selected ? (
        <div className="panel glass tools-asset-visual-actions">
          <div className="tools-asset-visual-section">
            <p className="eyebrow">免绑定快速模式</p>
            <p className="muted">无需绑定工程，可直接生成三视图与九宫格示意图（不写入项目资产）。</p>
            <div className="form-grid">
              <label className="input-wrap">
                <span className="label">主体名称</span>
                <input
                  className="ctrl"
                  value={quickAssetName}
                  onChange={(e) => setQuickAssetName(e.target.value)}
                  placeholder="例如：未来女警"
                />
              </label>
              <label className="input-wrap">
                <span className="label">类型</span>
                <select className="ctrl" value={quickAssetType} onChange={(e) => setQuickAssetType(e.target.value as 'CHARACTER' | 'BACKGROUND' | 'PROP')}>
                  <option value="CHARACTER">角色</option>
                  <option value="BACKGROUND">背景</option>
                  <option value="PROP">道具</option>
                </select>
              </label>
            </div>
            <label className="input-wrap">
              <span className="label">描述（可选）</span>
              <textarea
                className="ctrl"
                rows={3}
                value={quickAssetDesc}
                onChange={(e) => setQuickAssetDesc(e.target.value)}
                placeholder="例如：银白短发、机能风外套、冷色调"
              />
            </label>
            <div className="actions">
              <AppButton size="sm" loading={quickBusy} onClick={() => void runQuick('THREE_VIEW')}>
                免绑定生成三视图
              </AppButton>
              <AppButton
                size="sm"
                variant="primary"
                loading={quickBusy}
                onClick={() => void runQuick('TURNAROUND')}
              >
                免绑定生成九宫格 B-7
              </AppButton>
            </div>
          </div>
        </div>
      ) : null}

      {selected ? (
        <div className="panel glass tools-asset-visual-previews">
          {isCharacter && selected.turnaroundImageFileId ? (
            <div className="turnaround-preview">
              <p className="eyebrow">九宫格造型 B-7</p>
              <img
                className="turnaround-img"
                src={resolveScriptFileUrl(selected.turnaroundImageFileId)}
                alt={`${selected.name} 九宫格`}
              />
            </div>
          ) : null}
          {selected.threeViewImageFileId ? (
            <div className="turnaround-preview">
              <p className="eyebrow">三视图（正/侧/背）</p>
              <img
                className="turnaround-img"
                src={resolveScriptFileUrl(selected.threeViewImageFileId)}
                alt={`${selected.name} 三视图`}
              />
            </div>
          ) : null}
          {!selected.threeViewImageFileId && !(isCharacter && selected.turnaroundImageFileId) ? (
            <p className="muted">尚无预览图，生成后将显示在此。</p>
          ) : null}
        </div>
      ) : null}

      {!selected && (quickThreeViewUrl || quickTurnaroundUrl) ? (
        <div className="panel glass tools-asset-visual-previews">
          {quickTurnaroundUrl ? (
            <div className="turnaround-preview">
              <p className="eyebrow">免绑定九宫格造型 B-7</p>
              <img className="turnaround-img" src={quickTurnaroundUrl} alt="免绑定九宫格" />
            </div>
          ) : null}
          {quickThreeViewUrl ? (
            <div className="turnaround-preview">
              <p className="eyebrow">免绑定三视图（正/侧/背）</p>
              <img className="turnaround-img" src={quickThreeViewUrl} alt="免绑定三视图" />
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
