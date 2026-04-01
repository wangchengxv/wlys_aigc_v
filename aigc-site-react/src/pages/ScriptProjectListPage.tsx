import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptProjectCard } from '@/components/script/ScriptProjectCard'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'

export function ScriptProjectListPage() {
  const loadProjects = useScriptProjectStore((s) => s.loadProjects)
  const listLoading = useScriptProjectStore((s) => s.listLoading)
  const projects = useScriptProjectStore((s) => s.projects)
  const [deletedView, setDeletedView] = useState(false)

  useEffect(() => {
    void loadProjects({ deleted: deletedView })
  }, [loadProjects, deletedView])

  return (
    <section className="script-list-page">
      <div className="head panel glass">
        <div>
          <p className="eyebrow">Script Workflow</p>
          <h2>从剧本到关键帧，再到并发视频</h2>
          <p className="muted">单独维护项目、资产、关键帧和视频任务，不影响原有文生图/文生视频。</p>
        </div>
        <Link className="create-link" to="/script-projects/new">
          新建项目
        </Link>
      </div>
      <div className="panel glass" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
        <button
          type="button"
          className={`nav-btn ${deletedView ? '' : 'primary'}`}
          onClick={() => setDeletedView(false)}
          disabled={listLoading || !deletedView}
        >
          项目列表
        </button>
        <button
          type="button"
          className={`nav-btn ${deletedView ? 'primary' : ''}`}
          onClick={() => setDeletedView(true)}
          disabled={listLoading || deletedView}
        >
          回收站
        </button>
      </div>

      {listLoading ? (
        <LoadingSpinner />
      ) : projects.length ? (
        <div className="grid">
          {projects.map((item) => (
            <ScriptProjectCard key={item.projectId} project={item} deletedView={deletedView} />
          ))}
        </div>
      ) : (
        <EmptyState
          title={deletedView ? '回收站为空' : '还没有剧本工程'}
          description={deletedView ? '已删除项目会在这里展示，可随时恢复。' : '先创建一个项目，贴入剧本或上传文档后开始完善与生产。'}
        >
          {deletedView ? (
            <button type="button" className="create-link" onClick={() => setDeletedView(false)}>
              返回项目列表
            </button>
          ) : (
            <Link className="create-link" to="/script-projects/new">
              立即创建
            </Link>
          )}
        </EmptyState>
      )}
    </section>
  )
}
