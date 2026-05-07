import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ScriptProjectCard } from '@/components/script/ScriptProjectCard'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'

export function ScriptProjectListPage() {
  const loadProjects = useScriptProjectStore((s) => s.loadProjects)
  const listLoading = useScriptProjectStore((s) => s.listLoading)
  const projects = useScriptProjectStore((s) => s.projects)

  useEffect(() => {
    void loadProjects()
  }, [loadProjects])

  return (
    <section className="script-list-page">
      <div className="head panel glass">
        <div>
          <p className="eyebrow">Script Workflow</p>
          <h2>从剧本到关键帧，再到并发视频</h2>
          <p className="muted">单独维护项目、资产、关键帧和视频任务，不影响原有生成工作台。</p>
        </div>
        <Link className="create-link" to="/script-projects/new">
          新建项目
        </Link>
      </div>

      {listLoading ? (
        <LoadingSpinner />
      ) : projects.length ? (
        <div className="grid">
          {projects.map((item) => (
            <ScriptProjectCard key={item.projectId} project={item} />
          ))}
        </div>
      ) : (
        <EmptyState title="还没有剧本项目" description="先创建一个项目，贴入剧本或上传文档后开始完善与生产。">
          <Link className="create-link" to="/script-projects/new">
            立即创建
          </Link>
        </EmptyState>
      )}
    </section>
  )
}
