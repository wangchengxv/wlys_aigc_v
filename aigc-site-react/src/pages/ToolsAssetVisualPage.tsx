import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  generateThreeView,
  generateTurnaroundImage,
  generateTurnaroundPlan,
  getScriptAssets,
  listScriptProjects,
  resolveScriptFileUrl,
} from '@/api'
import { AppButton } from '@/components/common/AppButton'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { useToast } from '@/context/ToastContext'
import type { ExtractedAsset, ScriptProjectSummary } from '@/types'

export function ToolsAssetVisualPage() {
  const { showToast } = useToast()
  const [projects, setProjects] = useState<ScriptProjectSummary[]>([])
  const [projectsLoading, setProjectsLoading] = useState(true)
  const [projectId, setProjectId] = useState('')
  const [assets, setAssets] = useState<ExtractedAsset[]>([])
  const [assetsLoading, setAssetsLoading] = useState(false)
  const [assetId, setAssetId] = useState('')
  const [busy, setBusy] = useState(false)

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
      showToast('请先选择剧本工程与资产', 'error')
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

  return (
    <section className="tools-asset-visual-page">
      <div className="head panel glass">
        <div>
          <p className="eyebrow">Tools</p>
          <h2>三视图与九宫格</h2>
          <p className="muted">选择已有剧本工程与资产，调用与「资产与关键帧」页相同的生成能力；数据仍保存在该工程中。</p>
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
    </section>
  )
}
