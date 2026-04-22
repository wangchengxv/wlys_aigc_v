import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { ProjectSubpageShell } from '@/components/script/ProjectSubpageShell'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'
import { GlobalSettingsPage } from '@/pages/GlobalSettingsPage'

export function ScriptProjectGlobalSettingsPage() {
  const { projectId = '' } = useParams()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const loadProject = useScriptProjectStore((s) => s.loadProject)

  useEffect(() => {
    if (!projectId) return
    void loadProject(projectId).catch((e) => {
      showToast(e instanceof Error ? e.message : '项目加载失败，请重试', 'error')
    })
  }, [projectId, loadProject, showToast])

  if (detailLoading && (!currentProject || currentProject.project.projectId !== projectId)) {
    return <LoadingSpinner />
  }

  if (!currentProject || currentProject.project.projectId !== projectId) {
    return (
      <EmptyState title="项目不存在" description="请返回列表重新选择项目。">
        <Link to="/script-projects">返回项目列表</Link>
      </EmptyState>
    )
  }

  return (
    <ProjectSubpageShell
      projectId={projectId}
      title="全局设定"
      description="先统一画幅、风格和生成时长，再进入剧本生产流程，可减少后续返工。"
      meta={<span className="soft-badge">{currentProject.project.name}</span>}
      helpTitle="首步说明"
      help={
        <>
          <p>这里是剧本工程第 1 步。建议先确认视觉风格和目标时长，再开始剧本、资产和视频生产。</p>
          <p>设定会保存在浏览器本地，可复用于后续新建项目。</p>
        </>
      }
    >
      <GlobalSettingsPage />
    </ProjectSubpageShell>
  )
}
