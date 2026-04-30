import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listScriptProjects } from '@/api'
import { ComfyLikeCanvas } from '@/components/canvas/ComfyLikeCanvas'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { useToast } from '@/context/ToastContext'
import { useAuthStore } from '@/stores/authStore'
import type { ScriptProjectSummary } from '@/types'

function normalizeTitle(value: string) {
  return value.trim() || '未命名无限画布'
}

export function CanvasPage() {
  const { showToast } = useToast()
  const user = useAuthStore((s) => s.user)
  const [searchParams, setSearchParams] = useSearchParams()
  const [projects, setProjects] = useState<ScriptProjectSummary[]>([])
  const [loadingProjects, setLoadingProjects] = useState(true)
  const [draftTitle, setDraftTitle] = useState('未命名无限画布')
  const [projectId, setProjectId] = useState(searchParams.get('projectId') ?? '')
  const selectedProject = useMemo(
    () => projects.find((item) => item.projectId === projectId) ?? null,
    [projectId, projects],
  )

  useEffect(() => {
    void (async () => {
      setLoadingProjects(true)
      try {
        const list = await listScriptProjects({ deleted: false })
        setProjects(list)
      } catch (error) {
        showToast(error instanceof Error ? error.message : '加载工程列表失败', 'error')
      } finally {
        setLoadingProjects(false)
      }
    })()
  }, [showToast])

  useEffect(() => {
    const next = new URLSearchParams(searchParams)
    if (projectId) {
      next.set('projectId', projectId)
    } else {
      next.delete('projectId')
    }
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true })
    }
  }, [projectId, searchParams, setSearchParams])

  return (
    <section className="canvas-tool-page">
      <div className="workspace-home__hero canvas-tool-page__hero">
        <div className="workspace-home__hero-copy">
          <span className="workspace-home__pill">{user ? `${user.displayName} 的画布工作区` : '访客画布模式'}</span>
          <h2>无限画布创作台</h2>
          <p>聚焦节点编排与执行，工具与状态已收纳为抽屉，不再打断画布编辑。</p>
        </div>
        <div className="canvas-tool-page__hero-side">
          <div className="canvas-tool-page__hero-links">
            <Link className="app-btn v-primary s-md" to="/script-projects">
              打开项目库
            </Link>
            <Link className="app-btn v-ghost s-md" to="/workspace">
              返回创作工作台
            </Link>
          </div>
        </div>
      </div>

      <div className="canvas-tool-page__grid">
        <section className="content-card">
          <div className="section-heading">
            <h3>草稿绑定</h3>
            <span>标题与项目会作为画布上下文，切换项目后会恢复对应草稿</span>
          </div>

          <div className="canvas-tool-page__form-grid">
            <label className="input-wrap">
              <span className="label">草稿标题</span>
              <input
                className="ctrl"
                value={draftTitle}
                onChange={(event) => setDraftTitle(event.target.value)}
                onBlur={() => setDraftTitle((current) => normalizeTitle(current))}
                placeholder="例如：课程海报流程画布"
              />
            </label>

            <label className="input-wrap">
              <span className="label">绑定剧本工程（可选）</span>
              {loadingProjects ? (
                <div className="canvas-tool-page__select-loading">
                  <LoadingSpinner />
                </div>
              ) : (
                <select className="ctrl" value={projectId} onChange={(event) => setProjectId(event.target.value)}>
                  <option value="">暂不绑定工程</option>
                  {projects.map((project) => (
                    <option key={project.projectId} value={project.projectId}>
                      {project.name}
                    </option>
                  ))}
                </select>
              )}
            </label>
          </div>

          <div className="canvas-tool-page__binding-meta">
            <div>
              <strong>{selectedProject?.name || '未绑定工程'}</strong>
              <p className="muted">
                {selectedProject
                  ? `当前草稿会与项目「${selectedProject.name}」关联，便于后续衔接资产页与项目流程。`
                  : '未绑定时仍支持本地缓存、远端保存、Comfy 提交与结果回显。'}
              </p>
            </div>
            {selectedProject ? (
              <Link className="app-btn v-ghost s-sm" to={`/script-projects/${encodeURIComponent(selectedProject.projectId)}`}>
                打开工程
              </Link>
            ) : null}
          </div>
        </section>
      </div>

      <ComfyLikeCanvas
        draftTitle={normalizeTitle(draftTitle)}
        projectId={projectId || null}
        projectName={selectedProject?.name}
        onDraftMetaHydrated={(meta) => {
          setDraftTitle(normalizeTitle(meta.title))
          setProjectId(meta.projectId ?? '')
        }}
      />
    </section>
  )
}
