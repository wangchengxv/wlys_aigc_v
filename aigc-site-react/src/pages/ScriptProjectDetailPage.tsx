import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AppButton } from '@/components/common/AppButton'
import { AppInput } from '@/components/common/AppInput'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { EmptyState } from '@/components/common/EmptyState'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { PipelineProgressBar } from '@/components/script/PipelineProgressBar'
import { ScriptProjectWorkflowNav } from '@/components/script/ScriptProjectWorkflowNav'
import { useToast } from '@/context/ToastContext'
import { useScriptProjectStore } from '@/stores/scriptProjectStore'

export function ScriptProjectDetailPage() {
  const { projectId = '' } = useParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const currentProject = useScriptProjectStore((s) => s.currentProject)
  const detailLoading = useScriptProjectStore((s) => s.detailLoading)
  const pipelineStatus = useScriptProjectStore((s) => s.pipelineStatus)
  const loadProject = useScriptProjectStore((s) => s.loadProject)
  const loadPipelineStatus = useScriptProjectStore((s) => s.loadPipelineStatus)
  const removeProject = useScriptProjectStore((s) => s.removeProject)
  const stopPolling = useScriptProjectStore((s) => s.stopPolling)
  const refineWithPrompt = useScriptProjectStore((s) => s.refineWithPrompt)
  const refinePromptLoading = useScriptProjectStore((s) => s.refinePromptLoading)

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [briefPrompt, setBriefPrompt] = useState('')

  async function handleRefineWithPrompt() {
    const prompt = briefPrompt.trim()
    if (!prompt) {
      showToast('请输入短提示词', 'error')
      return
    }
    try {
      await refineWithPrompt(projectId, prompt)
      showToast('剧本完善功能完成', 'success')
      await navigate(`/script-projects/${projectId}/preview?tab=structured`)
    } catch (e) {
      showToast(e instanceof Error ? e.message : '完善剧本失败', 'error')
    }
  }

  async function handleDeleteConfirm() {
    setDeleting(true)
    try {
      await removeProject(projectId)
      stopPolling()
      setShowDeleteConfirm(false)
      showToast('剧本工程已移入回收站', 'success')
      await navigate('/script-projects')
    } catch (e) {
      showToast(e instanceof Error ? e.message : '删除失败', 'error')
    } finally {
      setDeleting(false)
    }
  }

  useEffect(() => {
    if (!projectId) return
    void (async () => {
      try {
        await Promise.all([loadProject(projectId), loadPipelineStatus(projectId)])
      } catch (e) {
        showToast(e instanceof Error ? e.message : '页面初始化失败，请重试', 'error')
      }
    })()
  }, [projectId, loadProject, loadPipelineStatus])

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

  const p = currentProject.project
  return (
    <div className="script-project-workflow-layout">
      <ScriptProjectWorkflowNav projectId={projectId} />
      <div className="script-project-workflow-layout__main">
    <section className="script-detail-page">
      <div className="hero panel glass">
        <div>
          <p className="eyebrow">Project</p>
          <h2>{p.name}</h2>
          <p className="muted">{p.scriptSummary || '暂无摘要'}</p>
        </div>
        <div className="actions" style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-md)', alignItems: 'center' }}>
          <Link className="nav-btn primary" to={`/script-projects/${projectId}/preview`}>
            剧本预览
          </Link>
          <Link className="nav-btn" to={`/script-projects/${projectId}/assets`}>
            资产与关键帧
          </Link>
          <Link className="nav-btn" to={`/script-projects/${projectId}/video`}>
            镜头拆分与视频生成
          </Link>
          <Link className="nav-btn" to={`/script-projects/${projectId}/export`}>
            成片与导出
          </Link>
          <div style={{ width: 'min(360px, 100%)' }}>
            <AppInput
              value={briefPrompt}
              onChange={(v) => setBriefPrompt(String(v))}
              label="短提示词"
              as="textarea"
              rows={3}
              placeholder="例如：让节奏更紧凑，突出主角情绪变化。"
            />
          </div>
          <AppButton variant="primary" loading={refinePromptLoading} onClick={() => void handleRefineWithPrompt()}>
            剧本完善功能
          </AppButton>
          <button
            type="button"
            className="nav-btn danger"
            disabled={deleting}
            onClick={() => setShowDeleteConfirm(true)}
          >
            删除剧本
          </button>
        </div>
      </div>

      <ConfirmDialog
        visible={showDeleteConfirm}
        title="删除剧本工程"
        message="确定将该项目移入回收站吗？你之后可以在回收站恢复。"
        confirmText="移入回收站"
        confirmLoading={deleting}
        disableCancel={deleting}
        cancelText="取消"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setShowDeleteConfirm(false)}
      />

      <div className="meta-grid">
        <div className="panel glass meta-card">
          <h3>基础信息</h3>
          <p>状态：{p.status}</p>
          <p>风格：{p.visualStyle}</p>
          <p>比例：{p.aspectRatio}</p>
          <p>时长：{p.targetDuration} 秒</p>
          <p>语言：{p.language}</p>
        </div>
        <div className="panel glass meta-card">
          <h3>资产统计</h3>
          <p>文档版本：{currentProject.documents.length}</p>
          <p>已抽取资产：{currentProject.assets.length}</p>
          <p>关键帧：{currentProject.keyframes.length}</p>
          <p>镜头：{currentProject.shots.length}</p>
          <p>视频任务：{currentProject.videoTasks.length}</p>
        </div>
      </div>

      <PipelineProgressBar pipeline={pipelineStatus} />
    </section>
      </div>
    </div>
  )
}
